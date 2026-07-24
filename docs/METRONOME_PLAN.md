# メトロノーム／クリック＋カウントイン 実装計画 rev.1

> `docs/RHYTHM_COMPLETION_PLAN.md` §9 の推奨フォローアップ 1 を実装する。
> DESIGN.md §4 は Rhythm レイヤーの Listen 手段として "click track + rhythm-only
> playback" を挙げているが未実装。リズム学習アプリの中核体験にあたる。
> rev.1 の自己レビュー結果は §7 に反映済み。

## 0. ゴール（Definition of Done）

エクササイズ画面で、**一定のパルス（クリック）を聴きながら譜例を再生できる**。

1. メトロノーム ON/OFF をエクササイズ画面から 1 タップで切り替えられる
2. カウントイン（再生前の 1 小節分の予備カウント）を ON/OFF できる
3. 設定はエクササイズを切り替えても・アプリを再起動しても保持される
4. CI（`core-test` / `android-build`）グリーン
5. 既存の再生・テンポ・ループ・タップ解説の挙動を壊さない

**スコープ外**（今回やらないことの明示）：
- リズムのみ再生（音高を消す／メロディをミュート）— DESIGN §4 の別項目。
  `IAlphaSynth.setChannelMute/setChannelVolume` で実現可能だが本イテレーション外
- テンポそのものの変更（現状の 50–100% スライダーを維持。BPM 直接指定はしない）
- 拍頭アクセントの強弱カスタマイズ、クリック音色の選択
- コンテンツ（YAML）の変更

## 1. 技術的前提（AAR の javap で確認済み）

`docs/alphatab-api-notes.md` より、**alphaTab が標準でクリックとカウントインを持つ**。
自前の音源生成・スケジューリングは不要：

| API | 型 | 用途 |
| --- | --- | --- |
| `AlphaTabApiBase.metronomeVolume` | `double` | クリック音量。0.0 で無音＝OFF |
| `AlphaTabApiBase.countInVolume` | `double` | カウントイン音量。0.0 で無効＝OFF |

（`IAlphaSynth` 側にも同名プロパティがあり、`AlphaTabApiBase` はそこへ委譲する）

⇒ 実装は「**2 つの double を UI から出し入れするだけ**」に収束する。
新規の音声処理コードは書かない＝実機でしか判明しないリスクを最小化する
（IMPLEMENTATION_PLAN §1 の設計原則「Android でしか動かないコードを最小化」に合致）。

## 2. 設計

### 2.1 変更するファイル

| ファイル | 変更内容 |
| --- | --- |
| `app/.../data/PlaybackPreferences.kt` | **新規**。SharedPreferences による設定の永続化（薄いラッパ） |
| `app/.../AppState.kt` | `playbackPreferences` を保持し各画面へ配る |
| `app/.../MainActivity.kt` | `PlaybackPreferences` を生成して `AppState` に渡す |
| `app/.../score/ExerciseScoreView.kt` | `metronomeEnabled` / `countInEnabled` を受け取り api へ反映 |
| `app/.../ui/ExerciseScreen.kt` | トグル UI＋状態管理（初期値は保存値、変更時に保存） |

`:core` は変更しない（再生は純粋に `:app` の関心事）。コンテンツも変更しない。

### 2.2 状態の持ち方

現状 `playbackSpeed` / `isLooping` は `remember(exerciseId)` で**エクササイズごとにリセット**
される。メトロノームも同じ扱いにすると、譜例を切り替えるたびに OFF に戻り、
クリックを常用したいユーザーが毎回入れ直すことになる。

⇒ **メトロノームとカウントインはアプリ全体の設定として永続化する**
（`SharedPreferences`）。テンポ・ループは現状どおりエクササイズごと（譜例ごとに
適切な速度・ループ範囲が違うため、リセットされる方が自然）。

```kotlin
// app/data/PlaybackPreferences.kt（新規・薄いラッパ）
class PlaybackPreferences(context: Context) {
    var metronomeEnabled: Boolean   // 既定 true（§2.4）
    var countInEnabled: Boolean     // 既定 false
}
```

進捗用の `SqliteProgressStore` には手を入れない。進捗（`ProgressStore` は `:core` の
契約）と再生設定は関心が別であり、SharedPreferences で十分な粒度のため。

### 2.3 api への反映タイミング

`ExerciseScoreView` の `update` ブロック（＝再コンポーズのたびに走る。既存の
`playbackSpeed` / `isLooping` と同じ経路）で適用する。加えて **`playerReady` ハンドラでも
再適用**する：シンセ初期化前に設定した値が取りこぼされる可能性を潰すため
（`AlphaTabApiBase` が内部の `IAlphaSynth` へ委譲する構造上、player 未生成時の
セットが失われても、ready 後に必ず正しい値へ収束させる）。

```kotlin
// 値の対応
api.metronomeVolume = if (metronomeEnabled) METRONOME_VOLUME else 0.0
api.countInVolume   = if (countInEnabled)   COUNT_IN_VOLUME  else 0.0
```

`METRONOME_VOLUME` / `COUNT_IN_VOLUME` は定数として切り出す（§7 S1 参照）。

### 2.4 既定値

| 設定 | 既定 | 根拠 |
| --- | --- | --- |
| メトロノーム | **ON** | 本アプリはリズム領域が最優先（DESIGN §2.1）。クリック無しでは「裏拍に乗れているか」を検証できず、シンコペーション・複合拍子の譜例は特に意味が半減する。1 タップで OFF にでき、選択は永続化される |
| カウントイン | **OFF** | 再生開始が 1 小節ぶん遅れるため、初見では「押しても鳴らない」と誤解されうる。必要な人が明示的に ON にする |

### 2.5 UI（配置）

現在の操作行は `[再生] ループ(Switch)` の 1 行。ここへ Switch を 2 つ足すと横幅が
足りず、行を増やすと譜面（`weight(1f)`）の高さを圧迫する（解説パネルは既に
`heightIn(max=132.dp)` まで詰めた経緯がある）。

⇒ **3 つの真偽値トグルを `FilterChip` に統一し、1 行に折り返し配置**する：

```
[再生]  [ループ] [メトロノーム] [カウントイン]     ← chip は Switch より高さ・幅とも小さい
```

既存の「ループ」Switch も chip に置き換える。3 つとも同種（再生オプションの
ON/OFF）であり、表現を揃えた方が学習コストが低く、縦幅も節約できるため。
`FlowRow` を使って狭い端末では自動折り返しさせる。

## 3. 検証

| 層 | 手段 | 対象 |
| --- | --- | --- |
| ローカル | `./gradlew :core:test` | `:core` 無変更の回帰確認 |
| ローカル | `tools/texcheck/texcheck.sh` | コンテンツ無変更の回帰確認 |
| CI | `android-build`（`:app:testDebugUnitTest :app:assembleDebug`） | **コンパイル通過が主目的**。ローカルに Android SDK が無いため（IMPLEMENTATION_PLAN §1）ここが唯一のコンパイル検証 |
| 実機 | ユーザー確認 | §5 の項目 |

`:app` に新規ユニットテストは追加しない：`PlaybackPreferences` は
`SharedPreferences` の薄いラッパで、JVM ユニットテスト（Robolectric なし）では
Android フレームワークに触れられず検証できないため。ロジック分岐を持たせない
設計にすることでテスト不要な薄さを保つ（§7 S4）。

## 4. 作業手順

1. `PlaybackPreferences` 追加 → `AppState` / `MainActivity` へ配線
2. `ExerciseScoreView` に 2 引数追加 → api へ反映（update＋playerReady）
3. `ExerciseScreen` の操作行を chip 化し、トグルを追加・永続化
4. `:core:test` / texcheck で回帰確認 → コミット → push → CI グリーン確認
5. PR 作成（実機確認の観点を本文に記載）

## 5. 実機確認してほしいこと（ユーザー引き渡し）

ローカルで Android を実行できないため、以下は実機判定になる：

1. メトロノーム ON で**クリックが鳴るか**、譜面の音と音量バランスは適切か
   （`masterVolume` は歪み対策で 0.8 に下げてある。クリックが大きすぎ／小さすぎないか）
2. カウントイン ON で**再生前に予備カウントが入るか**、その間に「押したのに無音」と
   感じないか（カーソルが動かない時間が生じる可能性）
3. **ループ ON × カウントイン ON** の組み合わせで、周回ごとにカウントインが
   入るか／初回だけか（alphaTab の仕様が不明。§6 R2）
4. テンポ 50% でクリックも一緒に遅くなるか
5. 設定がエクササイズ切替・アプリ再起動後も保持されるか

## 6. リスク

| # | リスク | 影響 | 対応 |
| - | --- | --- | --- |
| R1 | `metronomeVolume` の適正値が不明（マスター 0.8 とのバランス） | クリックが埋もれる／うるさい | 定数として一箇所に切り出し、実機フィードバックで即調整できるようにする（§7 S1） |
| R2 | ループ時のカウントインの挙動が仕様不明 | 毎周回カウントが入り煩わしい可能性 | 実機確認項目（§5 の 3）。煩わしければ「ループ ON 時はカウントインを初回のみ」等の追加制御を次イテレーションで検討 |
| R3 | player 未初期化時に設定した音量が失われる | クリックが鳴らない | `update` ブロックに加え `playerReady` でも再適用（§2.3） |
| R4 | ローカルで `:app` をコンパイルできない | 型・API 名の誤りが CI まで露見しない | 変更を小さく保ち、API 名は `docs/alphatab-api-notes.md`（javap 由来の一次資料）で確認済みの綴りのみ使う |
| R5 | chip 化で既存 UI の操作性が変わる | ユーザーの戸惑い | ラベル文言は「ループ」を据え置き、位置も同じ行の先頭に維持 |

## 7. 自己レビュー（テックリード視点）と反映

| # | 指摘 | 対応 |
| - | --- | --- |
| S1 | 音量値をコード中に直書きすると、実機で調整が必要になったとき修正点が散らばる | `METRONOME_VOLUME` / `COUNT_IN_VOLUME` を定数化し 1 箇所に集約。R1 の調整コストを最小化 |
| S2 | 「メトロノーム既定 ON」は既存の再生挙動を全譜例で変える破壊的変更にあたる | 意図的な決定として §2.4 に根拠を明記し、PR 本文でも明示してユーザーが覆せるようにする。1 タップで OFF＋永続化されるため実害は小さい |
| S3 | 設定を `remember(exerciseId)` に載せると譜例切替で毎回リセットされ、常用したい機能として破綻する | 永続化（SharedPreferences）を DoD に含めた（§2.2）。テンポ・ループは従来どおり譜例ごと、と方針を分離 |
| S4 | 新規クラスに `:app` のユニットテストを足すと、Android フレームワーク依存で CI の JVM テストが落ちる | `PlaybackPreferences` は分岐を持たない薄いラッパとし、テストを追加しない方針を §3 に明記 |
| S5 | Switch を 3 つ並べると縦幅を食い、譜面の可視領域（過去に拡大した経緯あり）を再び削る | `FilterChip` ＋ `FlowRow` で 1 行に集約（§2.5）。譜面の `weight(1f)` を守る |
| S6 | 「カウントイン」の効果はカーソル・進行バーが動かない時間として現れ、無音バグと誤認されうる（過去に無音・進行停止の不具合があった経緯） | 既定 OFF とし（§2.4）、実機確認項目に明記（§5 の 2）。誤認を招くようなら次イテレーションでカウント表示 UI を検討 |
| S7 | `FlowRow` は Compose Foundation の実験的 API で、BOM のバージョンによっては `@OptIn` が必要 | 実装時に `@OptIn(ExperimentalLayoutApi::class)` を付与。万一解決できなければ通常の `Row` ＋横スクロールに退避（コンパイルは CI で確認） |
