# fumiwake（譜分け）— Design Document rev.2

> Score decomposition & comprehension app for acoustic guitar players.
> 楽譜を要素に分解して意味を理解し、その音を耳で確かめる「読解ツール」。

> **rev.2 の位置づけ** — rev.1 は着手前の構想だった。rev.2 は
> **rhythm ドメインを完成させた実装の現実**を織り込み、次段階（guitar ドメイン）へ
> 進むために必要な設計判断を確定させたもの。主な変更：
> §0 現在地の追加 / §1.3「確認」の原則 / §2.3 既習概念（構造的欠陥の解消） /
> §3.3 ドメイン間のコンテンツ非対称性 / §3.4 検証の三層 / §4 レイヤーの実装状況 /
> §5 実装の実測反映と音声の既知制約 / §6 ロードマップ改訂 / §8 未解決問題の更新。

## 0. 現在地（rev.2 時点）

Phase 0（技術スパイク）と Phase 1（Bundled MVP）は達成済み。
**アプリとして動作し、実機に配布されている**。

| 領域 | 状態 |
| --- | --- |
| rhythm ドメイン 9 概念 × 全 22 譜例 | ✅ 完成（DAG が最後までつながる） |
| pitch / guitar / structure / harmony | ⛔ タクソノミーのみ。エクササイズ 0 件 |
| 譜面表示（五線 + TAB）・タップ解説・単音再生 | ✅ |
| 再生・テンポ 50–100%・小節ループ・メトロノーム・カウントイン | ✅ |
| 進捗保存（SQLite）・概念の習得判定 | ✅ |
| レイヤー再生（リズムのみ／クリックのみ） | ⛔ §4 の設計に記載があるが未実装 |
| 指板可視化 | ⛔ 未実装 |
| 曲の持ち込み（MusicXML / Guitar Pro） | ⛔ 未着手。技術検証も未実施 |
| 音の安定性 | ⚠️ ゴースト音は解決。遅延／うねりはユーザー設定で吸収中（§5.2） |

**率直な評価**: 「リズム読解の専用ドリル」としては完成しているが、
*アコースティックギター奏者のための*という看板の核心（音符→フレット変換）は
まだ空である。次段階の主題はここを埋めることに置く。

## 1. One-line Definition

**fumiwake is a score *reader/analyzer*, not a performance trainer.**
It decomposes sheet music into elements (notes, rhythm, structure, harmony),
explains each element in context, and lets the user verify by ear via playback.

- ❌ NOT: real-time pitch detection / performance grading（マイク聴音・演奏判定はスコープ外。初期構想から撤回済み）
- ✅ IS: view → decompose → explain → listen → **check**（§1.3）

### 1.1 Target user (initial: the author himself)

- Can read note names on the staff (ドレミは読める)
- Cannot yet read: rhythm notation, key signatures, note→fret mapping, structural symbols
- Plays acoustic guitar

### 1.2 何をもって「読めた」とするか

rhythm ドメインでは「見る → 解説を読む → 聴いて確かめる」で成立していた。
リズムは**見た目と音の対応**が主題であり、聴けば確認になるからである。

しかし guitar ドメインの主題は **「五線上の音 → 指板上の位置」という変換**であり、
解説を読み、音を聴いても、その変換が自分でできるようになったかは分からない。
**現在の完了判定は自己申告ボタンのみ**で、この非対称性に対応できていない。

### 1.3 原則：「確認」は読解の確認であって、演奏の判定ではない

したがって rev.2 で「確認（check）」を読解サイクルの一部として正式に位置づける。
ただし境界を厳密に引く：

| | 内容 | 入力手段 | 可否 |
| --- | --- | --- | --- |
| **読解の確認** | 記譜 → 意味への変換が正しくできるか（この音はどのフレットか／このリズムはどう刻むか） | 画面タップ・選択 | ✅ スコープ内 |
| **演奏の判定** | 音程・音色・タイミング精度の巧拙を測る | マイク入力 | ❌ スコープ外（不変） |

この線引きにより、初期構想から撤回した「マイク聴音・演奏採点」を復活させることなく、
「読めるようになったか」を閉じることができる。確認は**採点ではなく答え合わせ**であり、
正誤の提示までを行い、上達度のスコアリングはしない。

## 2. Core Concept: Concept-Driven Curriculum（概念ドリブン）

The app is organized around a **taxonomy of notation concepts**, not around songs.
Each bundled exercise isolates exactly ONE concept (1譜例1概念の原則).
User-imported songs are analyzed against the same taxonomy
(→ "this song contains 7 ties; ties not yet learned" のような未習概念ハイライト).

### 2.1 Concept Taxonomy

Concepts form a **DAG (dependency graph)**, not a linear sequence.
`concept_id` naming: `<domain>.<name>`。全 25 概念（`ConceptGraph.bundled()` に内蔵）。

#### Domain: rhythm（✅ 全概念にエクササイズあり）

| concept_id            | name（日本語）        | depends_on                  | 譜例 |
| --------------------- | --------------------- | --------------------------- | ---- |
| rhythm.beat           | 拍と拍子（4/4の意味） | —                           | 2 |
| rhythm.basic-values   | 音価の基本（全〜8分） | rhythm.beat                 | 3 |
| rhythm.rests          | 休符                  | rhythm.basic-values         | 3 |
| rhythm.dotted         | 付点                  | rhythm.basic-values         | 2 |
| rhythm.tie            | タイ                  | rhythm.basic-values         | 3 |
| rhythm.sixteenth      | 16分音符              | rhythm.basic-values         | 3 |
| rhythm.syncopation    | シンコペーション      | rhythm.tie, rhythm.sixteenth| 2 |
| rhythm.triplet        | 3連符                 | rhythm.basic-values         | 2 |
| rhythm.compound-meter | 複合拍子（6/8等）     | rhythm.triplet              | 2 |

#### Domain: pitch

| concept_id           | name（日本語）       | depends_on           | 状態 |
| -------------------- | -------------------- | -------------------- | ---- |
| pitch.staff-names    | 五線上の音名         | —                    | **既習扱い**（§2.3） |
| pitch.ledger-lines   | 加線                 | pitch.staff-names    | 次段階で着手 |
| pitch.accidentals    | ♯♭♮（臨時記号）      | pitch.staff-names    | 次段階で着手（推奨） |
| pitch.key-signatures | 調号                 | pitch.accidentals    | 後段 |
| pitch.octaves        | オクターブの読み分け | pitch.ledger-lines   | 次段階で着手 |

#### Domain: guitar（このアプリ固有の価値 — 次段階の主題）

| concept_id             | name（日本語）                | depends_on                            |
| ---------------------- | ----------------------------- | ------------------------------------- |
| guitar.open-strings    | 開放弦と五線の対応            | pitch.staff-names                     |
| guitar.low-position    | ローポジションの音符→フレット | guitar.open-strings                   |
| guitar.multi-positions | 同じ音の複数ポジション        | guitar.low-position, pitch.octaves    |
| guitar.tab-mapping     | TAB譜⇄五線譜の読み替え        | guitar.low-position                   |

#### Domain: structure

| concept_id             | name（日本語）                  | depends_on          |
| ---------------------- | ------------------------------- | ------------------- |
| structure.repeats      | 反復記号（リピート、1・2番括弧）| rhythm.beat         |
| structure.dc-ds        | D.C. / D.S. / Coda              | structure.repeats   |
| structure.articulation | スラー・スタッカート等          | rhythm.basic-values |

#### Domain: harmony（後段）

| concept_id           | name（日本語）        | depends_on                              |
| -------------------- | --------------------- | --------------------------------------- |
| harmony.chords       | コード記号            | guitar.low-position                     |
| harmony.keys         | キーと調号の対応      | pitch.key-signatures                    |
| harmony.diatonic     | ダイアトニックコード  | harmony.chords, harmony.keys            |
| harmony.progressions | 進行の型              | harmony.diatonic                        |

### 2.2 ドメインは独立していない — 依存の推移閉包でスコープを決める

「次は guitar ドメインをやる」と宣言するだけでは**スコープを取り違える**。
guitar 4 概念すべてを AVAILABLE にするには、依存を推移的にたどった閉包が必要：

```
guitar.open-strings    ← pitch.staff-names（既習）
guitar.low-position    ← guitar.open-strings
guitar.tab-mapping     ← guitar.low-position
guitar.multi-positions ← guitar.low-position, pitch.octaves ← pitch.ledger-lines ← pitch.staff-names（既習）
```

⇒ **次段階のコンテンツ範囲は guitar 4 概念 + pitch 2 概念（ledger-lines, octaves）の計 6 概念**。
`pitch.accidentals` は DAG 上は必須ではないが、ローポジションの実譜面で♯♭は避けて
通れないため、含めることを推奨する（計 7 概念）。

**この閉包計算は新ドメイン着手時に必ず行う**。以後のドメインも同様
（例: harmony.chords は guitar.low-position を要求する）。

### 2.3 既習概念（assumed known）— 構造的欠陥とその解消

**問題**: `ProgressEvaluator` は「概念のエクササイズを全て完了したとき」にのみ
その概念を LEARNED へ昇格させる。したがって**エクササイズが 0 件の概念は永久に
LEARNED にならない**。`pitch.staff-names` はエクササイズを持たない（対象ユーザーが
既に読めるため作る意味がない）ので、それに依存する **guitar ドメイン全体が永久に
LOCKED のまま**になる。guitar のエクササイズを追加しても画面上は 🔒 のままである。

rev.1 は `pitch.staff-names` を「既習」と記述していたが、**「既習」を表現する手段が
スキーマにもコードにも存在しなかった**ことが原因である。

**決定**: `Concept` に `assumedKnown: Boolean = false` を追加する。

- `assumedKnown = true` の概念は、進捗ストアの内容によらず **LEARNED として扱う**
  （`ProgressEvaluator.statusOf` が判定に含める）
- 初期値: `pitch.staff-names` のみ true
- エクササイズを持たないため、概念マップ上は「既習」バッジで表示する（🔒 にしない）
- 将来、オンボーディングで「五線の音名は読めますか？」と尋ねてユーザーごとに
  可変にする余地を残す（タクソノミー既定値 → ユーザー設定で上書き）

**副次的な規約**: 「エクササイズ 0 件かつ `assumedKnown = false` の概念に依存する
概念は到達不能」という不変条件を `:core` のテストで検査する。同種の詰みを
将来のドメイン追加で再発させないための門番とする。

## 3. Content Architecture: Two Inlets, One Pipeline

```
[bundled exercises]      [user-imported songs]
  alphaTex (text)          MusicXML / Guitar Pro (.gp)
        \                       /
         v                     v
        alphaTab loader → internal score model
                    |
        +-----------+-----------+-----------+
        |           |           |           |
     render      decompose    playback     check
   (staff+TAB    (tap→explain (synth,      (§1.3
    +fretboard)   +fretboard)  layers)      読解確認)
```

- **Bundled side（同梱）** = systematic learning. Author-controlled, one concept per
  exercise, written in **alphaTex** (text format → Git-friendly, diffable, generatable).
  著作権的にもクリーン（自作エクササイズ or パブリックドメインのみ）。
- **Imported side（持ち込み）** = real-world application. MusicXML (from MuseScore etc.)
  and Guitar Pro files. Private use by the user; no redistribution.
- **Never bundle copyrighted songs.**（既存ヒット曲の同梱は配布にあたるためNG）

### 3.1 Exercise Schema（実装済み・Kotlin が単一ソース）

rev.1 は Pydantic で記述していたが、実装ではランタイムと同一言語で単一ソース化した
（`:core` の `exercise/Exercise.kt`。YAML は kotlinx.serialization + kaml で読む）。

```kotlin
@Serializable
data class FocusElement(
    val barIndex: Int,
    val beatIndex: Int,
    val noteIndex: Int? = null,   // null = 拍全体（休符など）
    val noteText: String,         // この出現箇所の文脈解説
)

@Serializable
data class Exercise(
    val exerciseId: String,       // "<concept_id>.<3桁>" 例 "rhythm.tie.001"
    val conceptId: String,        // primary concept — exactly one
    val prerequisites: List<String> = emptyList(),
    val titleJa: String,
    val alphatex: String,
    val explanationJa: String,
    val focusElements: List<FocusElement> = emptyList(),
    val difficulty: Int,          // 1..5, within-concept ordering
    val variantOf: String? = null,
)
```

Storage: `content/exercises/**/*.yaml`（単一ソース。`:app` の asset としても使用）。
SQLite は**進捗のみ**に使う（カタログはメモリ内。rev.1 の「ビルド時 SQLite カタログ」は
数十件規模には過剰と判断して撤回）。

### 3.2 ドメイン間のコンテンツ非対称性（次段階で効く）

rhythm の全譜例は `2.2.x`（同一弦・同一フレット）で書けた。**音高が主題ではなかった**
からである。guitar ドメインはその真逆で、**記譜音とフレット位置の対応そのものが主題**。
この非対称性は 3 方向に波及する：

1. **検証軸が増える**（§3.4）— 「記譜された音高と `fret`/`string` の組が一致するか」
   という新しい意味検証が必要。人手のレビューでは必ず取りこぼす。
2. **スキーマ拡張の可能性** — `guitar.multi-positions`（同じ音の複数ポジション）は
   「1 つの音に対する代替ポジション」を表現する必要がある。`focusElements.noteText`
   に文章で書いて済ませるか、構造化するかは実装計画で決める（§8 Q2）。
3. **表示要素が増える** — 指板図（§4.3）。

### 3.3 Variant generation（バリエーション量産）

スキーマ → 成果物生成のパターンは今後も一級市民として維持する。
guitar ドメインでは「同じ音型を弦・ポジションを変えて展開する」生成が特に有効で、
`multi-positions` のコンテンツは手書きより生成が適する。

### 3.4 コンテンツ検証の三層（実装済み + 次段階の追加）

同梱コンテンツの正しさは**機械検証で担保する**。人手のレビューは通り抜ける。
この方針は実際の障害から得た教訓である（`docs/AUDIO_DEFECT_PLAN.md`）：
alphaTex のタイ記法を取り違えた譜例が**構文検証を通過したまま**、
楽譜に存在しないゴースト音を鳴らし続けていた。

| 層 | 検証内容 | 実装 | 状態 |
| --- | --- | --- | --- |
| **構文** | `AlphaTexImporter` で実際にパースできるか | `tools/texcheck`（ローカル）/ `ExerciseTexParseTest`（CI 門番） | ✅ |
| **意味（MIDI）** | ①音の重なりが無い（全譜例が単旋律）②タイに相手がいる ③各小節長が拍子と一致 | `MidiFileGenerator` で生成 MIDI を検査 / `ExerciseMidiSemanticsTest` | ✅ |
| **意味（記譜↔運指）** | 記譜された音高と `fret`/`string` の組が一致する。ポジション範囲の逸脱がない | 次段階で追加 | ⛔ |
| **スキーマ** | id 形式・概念の存在・prerequisites が推移的依存の部分集合・difficulty 範囲 ほか | `ExerciseLoader.validate` / content 全件テスト | ✅ |

**次段階で第三層を追加すること**を guitar ドメイン着手の前提条件とする。

## 4. Interaction Model: Decompose by Layer

Decomposition is hierarchical; each layer pairs a *view* with a *listen* mode.

| Layer    | Decompose（見る）                       | Listen（聴く）                     | 状態 |
| -------- | --------------------------------------- | ---------------------------------- | ---- |
| Note     | tap → 音名・音価・♯♭・フレット位置      | play single note                   | ✅ |
| Rhythm   | 音高を消して音価だけ表示                | click track + rhythm-only playback | ⛔ §4.4 |
| Bar      | 1小節の要素一覧                         | loop bar, tempo 50–100%            | ✅ 再生側のみ |
| Phrase   | repeats / sections map（曲の地図）      | play section                       | ⛔ 後段 |
| Harmony  | chord symbols, key context              | chords-only layer                  | ⛔ 後段 |

Key principle: **explanations are contextual** — the symbol is explained
*as it appears in this score*, not as a detached glossary entry.
（汎用記号辞典ではなく「この曲のこのタイ」を起点に学ぶ）

### 4.1 タップ解説（実装済み）

音符/休符のタップ → `ScoreLocator` が `(barIndex, beatIndex, noteIndex)` を特定 →
`focusElements` に一致があればその文脈解説、なければ汎用情報（音名・音価・フレット）。
alphaTab の座標変換・モデル対応付けはすべて `ScoreLocator` に閉じ込め、
実機での位置ずれ修正が 1 箇所で済む構造を維持する。

### 4.2 指板可視化（次段階）

alphaTab は五線と TAB を描画するが、**指板図は描画しない**。自前の Compose
コンポーネントとして実装する。

- **`:core` に置くもの**: 「この MIDI 値は、このチューニングで、どの (弦, フレット) に
  存在するか」を列挙する純ロジック（`music/FretPositions` 等）。`NoteNames` と同じく
  Android 非依存でローカルテスト可能 — 本プロジェクトの中心原則
  「Android でしか動かないコードを最小化する」に従う
- **`:app` に置くもの**: 指板の描画とタップ入力のみ

指板図は 3 つの役割を同時に果たす：タップした音の位置提示（Note レイヤーの強化）、
`multi-positions`（同じ音が複数箇所にあることの提示）、
そして §1.3 の読解確認の入力面（「この音はどこ？」を指板でタップして答える）。

### 4.3 レイヤー再生（次段階）

「音高を消して音価だけ」は rev.1 から設計に載っていながら未実装である。
rhythm ドメインが完成した今、既存資産の価値を最大化する費用対効果の高い機能となる。

実装方針の候補（詳細は実装計画で決定 — §8 Q3）：
alphaTex を**全音符が同一音高になるよう変換した譜面**へ差し替える方式なら、
「見る（リズム譜表示）」と「聴く（リズムのみ再生）」を一手で満たせる。
同梱コンテンツは構文サブセットが限定されているためテキスト変換で扱え、
変換ロジックは `:core` に置いてローカルテストできる（Android SDK 不在の
開発環境という制約を突かない）。

## 5. Tech Stack

| Area            | Decision                                   | Status |
| --------------- | ------------------------------------------ | ------ |
| Platform        | Android native (Kotlin 2.2.x + Jetpack Compose) | 実装済み |
| Render + synth  | **alphaTab 1.8.4**（Android AAR、minSdk 26） | 実装済み・実機で動作確認済み |
| Bundled format  | alphaTex                                   | 実装済み |
| Exercise schema | Kotlin data class + kotlinx.serialization / kaml（YAML） | 実装済み（rev.1 の Pydantic から変更） |
| Catalog         | メモリ内（YAML 直読み）                     | 実装済み（rev.1 の SQLite カタログから変更） |
| Progress        | Android 標準 SQLite                         | 実装済み |
| Playback        | alphaTab built-in synth（AAR 同梱 sonivox.sf2 + Bravura.otf） | 実装済み |
| Metronome       | alphaTab の click / count-in（設定は端末に永続化） | 実装済み |
| Import formats  | MusicXML, Guitar Pro (.gp3–.gp)            | **未検証**（§8 Q4） |
| 配布            | main マージ時に CI が署名済み APK を GitHub Release（タグ `latest`）へ添付 | 実装済み |

### 5.1 開発環境の制約（設計を規定する前提）

開発コンテナに **Android SDK が無く、`:app` をローカルでビルドできない**。
そのため検証は三層構成をとる：ローカル（`:core` 全テスト + `tools/texcheck`）／
GitHub Actions（`:app` コンパイル・JVM ユニットテスト・APK 生成）／ユーザー実機
（描画品質・発音・タップ位置）。

⇒ **設計原則: ロジックは可能な限り `:core` に置き、`:app` は「表示・再生・UI」に限定する。**
新機能を設計するときは常に「この判断ロジックは `:core` に置けるか」を先に問う。

### 5.2 音声の既知の制約（設計上の妥協として記録）

alphaTab のオーディオバッファ長には**トレードオフ**がある：

- **短い** → 再生カーソルと音の同期が合う。しかし出力先によっては音が途切れる
- **長い** → 途切れないが、カーソルが音より先行して見える

最適点は出力経路（内蔵スピーカー / Bluetooth）と機種で変わり、**アプリ側で決め打ちできない**。
そのため ExerciseScreen に「音の遅延」スライダー（300–1500ms）を置き、
再生しながら合わせてもらう設計とした。値は端末に永続化する。
なお alphaTab の内部バッファは 4096 サンプル（約 46ms）単位に量子化される。

これは**恒久的な解ではない**。将来 `AudioTrack` の実測レイテンシからカーソル表示を
補正できれば、スライダーは不要になる可能性がある（§8 Q5）。

## 6. Roadmap

### ✅ Phase 0 — Technical spike（完了）

alphaTab の Android 組み込み・alphaTex 描画・内蔵シンセ再生・テンポ変更・小節ループを
実機で確認。※ MusicXML 検証（rev.1 の項目 4）は Phase 3 冒頭へ後送した（§8 Q4）。

### ✅ Phase 1 — Bundled MVP（完了）

エクササイズスキーマ + カタログ + 進捗（SQLite）、rhythm ドメイン全 9 概念 22 譜例、
タップ解説、単音/小節再生、概念マップ画面、メトロノーム/カウントイン。

### ▶ Phase 2 — ギター読解の核（次段階）

**主題**: 「五線の音 → 指板の位置」の変換を、教えられ・確かめられる状態にする。
これがアプリの存在意義（*ギター奏者のための*読解ツール）を成立させる。

前提として先に片づけるもの：

1. **`assumedKnown` の導入**（§2.3）— これ無しでは guitar ドメインが永久 LOCKED
2. **記譜↔運指の意味検証**（§3.4 第三層）— これ無しではコンテンツを信用できない

本体：

3. **コンテンツ**: guitar 4 概念 + pitch 2〜3 概念（§2.2 の閉包）
4. **指板可視化**（§4.2）— `:core` に位置計算、`:app` に描画
5. **読解確認**（§1.3）— 「この音はどこ？」を指板タップで答える形式。採点はしない
6. **レイヤー再生**（§4.3）— rhythm 資産の価値を最大化

### Phase 3 — 持ち込み曲

- MusicXML / .gp インポート。**着手前に fidelity のスパイクで go/no-go を出す**（§8 Q4）
- インポート曲を全概念でスキャン →「未習概念ハイライト」（学習と実曲の橋渡し。
  本アプリの目玉であり、同梱コンテンツが増えるほど価値が上がる）
- 同じ decompose / listen / check の UX を持ち込み譜面にも適用

### Phase 4 以降

- structure ドメイン（反復記号・セクションマップ＝曲の地図）
- harmony ドメイン（コード記号・ダイアトニック。コードダイアグラム表示）
- **定着の仕組み**（間違えた概念の間隔反復）。譜例数が Phase 2 で倍増して初めて
  「復習する価値のある量」になるため、この順序とする
- チューニング / カポ対応（持ち込み曲で必須になる）
- サイン波トーンモード、レイヤー分離（メロディ / コード / クリック）

## 7. Non-Goals

- **演奏の判定**（マイク入力・音程検出・巧拙のスコアリング）。§1.3 の「読解の確認」は
  入力がタップ／選択に限られ、これに該当しない
- 著作権のある市販曲の同梱
- iOS / web（Android でコア UX が証明されるまで）
- ソーシャル機能、クラウド同期（初期バージョンでは）

## 8. Open Questions

rev.1 から解決したもの：

- ~~alphaTab MusicXML fidelity~~ → 未解決だが Phase 3 冒頭のスパイクに位置づけ直した（Q4）
- ~~rhythm domain の粒度~~ → **解決**。9 概念のまま完成させて過不足なしと確認
- ~~focus_elements のアドレッシングが alphaTab の内部モデルと対応するか~~ →
  **解決**。`ScoreLocator` で `(bar.index, beat.index, notes 内の位置)` に対応付けでき、
  実機でも位置ずれなく動作
- ~~解説文の管理~~ → **解決**。エクササイズ内に埋め込む方式で運用できている
  （文脈依存の解説という原則に合致。概念単位の共有は必要が生じてから）

rev.2 で未解決のもの：

- [ ] **Q1**: `assumedKnown` をタクソノミー固定にするか、オンボーディングでユーザーに
      尋ねて可変にするか（初期実装は固定、将来可変を想定 — §2.3）
- [ ] **Q2**: `multi-positions` の代替ポジションを `noteText` の文章で扱うか、
      スキーマとして構造化するか（§3.2）
- [ ] **Q3**: レイヤー再生を alphaTex のテキスト変換で実装するか、
      パース済みモデルの操作で実装するか（§4.3）
- [ ] **Q4**: alphaTab の MusicXML 対応度（ドキュメントが "experimental" と "Mature" で
      不整合）。MuseScore 実出力での検証が必要。不十分なら
      OpenSheetMusicDisplay（描画のみ）を代替候補として評価
- [ ] **Q5**: 再生カーソルのレイテンシ補正を自動化し、「音の遅延」スライダーを
      不要にできるか（§5.2）
- [ ] **Q6**: 読解確認（§1.3）の結果を進捗に反映するか。現在の「完了ボタン」を
      置き換えるのか併存させるのか

## 9. 関連ドキュメント

| ドキュメント | 内容 |
| --- | --- |
| `docs/IMPLEMENTATION_PLAN.md` | Phase 0–1 の実装計画。モジュール構成・`:core` API 契約・検証戦略 |
| `docs/RHYTHM_COMPLETION_PLAN.md` | rhythm ドメイン残り 4 概念の追加計画 |
| `docs/AUDIO_DEFECT_PLAN.md` | 音の不具合の調査記録。タイ記法の取り違えと機械検証の必要性（§3.4 の根拠） |
| `docs/METRONOME_PLAN.md` | メトロノーム・カウントインの設計 |
| `docs/alphatab-api-notes.md` | alphaTab 1.8.4 の API 一次資料（AAR から抽出。公式サイトが開発環境から遮断されているため） |
