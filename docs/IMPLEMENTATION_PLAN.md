# fumiwake 実装計画（Phase 0–1 / Bundled MVP）rev.2

> DESIGN.md の Phase 0（技術スパイク）と Phase 1（同梱 MVP）を対象とした実装計画。
> rev.2: セルフレビュー（テックリード視点の批判的レビュー）の指摘を反映。
> 主な変更: スパイク優先の再編（旧 B2）、ProgressEvaluator 契約補完（旧 B1）、
> alphaTex の機械検証手段の確立、並行作業の衝突回避規則。
> DESIGN.md からの逸脱点は §9 に明記する。

## 0. ゴール（このイテレーションの Definition of Done）

DESIGN.md の MVP 定義をそのまま採用する：

> 同梱エクササイズ1式を読み込み、五線譜とTABを並べて表示し、音符をタップすると
> 音名・音価・フレット位置と解説が出て、その音／その小節を（テンポ可変で）再生できる。

成果物：
1. **スパイク APK**（最小の譜面表示＋再生。Phase 0 の go/no-go 判定用。早期に引き渡し）
2. ビルド可能な Android アプリ（debug APK が GitHub Actions の成果物として取得可能）
3. `:core` モジュール（純 Kotlin/JVM）＋ ユニットテスト一式（ローカル実行でグリーン）
4. rhythm ドメインの同梱エクササイズ（rhythm.beat 〜 rhythm.tie、各概念 2〜3 譜例、
   **全譜例が AlphaTexImporter によるパース検証済み**）
5. CI（GitHub Actions）：core テスト + Android ビルド（alphaTex パーステスト含む）がグリーン

## 1. 環境制約と検証戦略（最重要の前提）

開発コンテナの調査結果：

| 項目 | 状態 | 影響 |
| --- | --- | --- |
| Java 21 / Gradle 8.14.3 | あり | JVM モジュールはローカルでビルド・テスト可 |
| Android SDK | **なし**（dl.google.com 遮断、インストール不可） | `:app` はローカルでコンパイル不可 |
| maven.google.com | HTML は 200 だが**アーティファクトは dl.google.com へ 301 → 遮断** | AGP の解決すらローカル不可。`:app` は構成対象から外す必要（§2 の SDK ガードは必須） |
| Maven Central / plugins.gradle.org / services.gradle.org | アクセス可 | 依存・プラグイン・Gradle wrapper の解決は可能 |
| alphatab.net（公式ドキュメント） | **遮断** | AAR から javap で抽出した `docs/alphatab-api-notes.md` を一次資料とする |
| alphaTab | `net.alphatab:alphaTab:1.8.4`（Android AAR、minSdk 26）Maven Central にあり | 採用。旧 `alphaTab-android:1.3.0` は使わない |

⇒ **検証は三層構成**にする：

- **ローカル（この環境）**: `:core` の全ロジック＋エクササイズ YAML の全件スキーマ検証。
  さらに **`tools/texcheck`（§6.5）で全 alphaTex を AlphaTexImporter に実際にパースさせて検証**。
- **GitHub Actions**: `:app` のコンパイル、`:app:testDebugUnitTest`（alphaTex パーステスト
  — Android ユニットテストは JVM 実行なのでエミュレータ不要）、debug APK 生成。
- **ユーザー実機**: 描画品質・発音・タップ位置合わせなど「実行しないと分からないこと」。
  スパイク APK（M2a）で早期に、完成 APK（M2b）で最終確認。

この制約から導かれる設計原則：**「Android でしか動かないコード」を最小化する**。
ロジックは可能な限り `:core` に置き、`:app` は「alphaTab 表示・再生・UI」に限定する。

### alphaTab 1.8.4 の検証済み事実（AAR 展開・javap・JVM 実行で確認）

- `alphaTab.AlphaTabView`（Android View、`RelativeLayout` 派生）。`view.api` で
  `AlphaTabApiBase<AlphaTabView>` を取得 — これが統合の中心
- `api.tex(String)` で alphaTex を直接描画
- `api.playNote(Note)` / `api.playBeat(Beat)`：単音・単拍の再生（Note レイヤーの「聴く」）
- `api.playbackRange` + `api.isLooping` + `api.playbackSpeed`：小節ループとテンポ 50–100%
- `api.boundsLookup`：タップ座標 → `MasterBarBounds/BarBounds/BeatBounds/NoteBounds`
- イベントは `IEventEmitter` / `IEventEmitterOfT`（ready, readyForPlayback, stateChanged 等）
- AAR に `Bravura.otf` と `sonivox.sf2/sf3` を同梱 → 追加アセット不要で描画・再生可
- **`AlphaTexImporter` は純 JVM で動作することを実証済み**（kotlin-stdlib と
  kotlinx-coroutines-core-jvm のみで `initFromString` → `readScore` が成功。
  構文エラー時は行・列付き診断 `AT2xx` を返す）→ alphaTex 検証は完全自動化できる
- Note モデル: `fret` / `string` / `realValue`（MIDI 値）/ `octave` / `stringTuning`。
  Beat モデル: `duration` / `dots` / `isRest` / `index` / `notes`。
  → 「音名・音価・フレット位置」表示に必要な情報は全て取得可能。
  音名文字列は直接持たないため **MIDI 値→音名変換は `:core` のユーティリティで実装**（§4）
- 検証済み alphaTex 基本構文: 音符 `fret.string.duration`、休符 `r.duration`、
  付点 `{d}`、タイ `-.duration`、拍子 `\ts 4 4`、小節線 `|`
- 注意: alphaTex の弦番号と `Note.string` の値は同一とは限らない（実測: `3.3.4` →
  `Note.string == 4`）。対応付けは ScoreLocator（§5）に閉じ込め、ユニットテストで固定する

## 2. モジュール構成

```
fumiwake/
├── settings.gradle.kts        # :app は Android SDK がある環境でのみ include（下記）
├── gradle/libs.versions.toml  # version catalog（M0 で全依存を確定し以後凍結 — §6.0）
├── core/                      # 純 Kotlin/JVM。Android 依存ゼロ
│   └── src/main/kotlin/dev/fumiwake/core/
│       ├── concept/           # タクソノミー：Concept, ConceptGraph（DAG）
│       ├── exercise/          # Exercise スキーマ, YAML ロード, 検証
│       ├── catalog/           # Catalog（概念×エクササイズの索引・学習順）
│       ├── music/             # NoteName / DurationText（MIDI値→音名等の変換）
│       └── progress/          # ProgressStore IF + ProgressEvaluator（昇格ロジック含む）
├── content/exercises/         # 同梱エクササイズ YAML（単一ソース）
│   └── rhythm/
│       └── rhythm.beat.001.yaml ...
├── tools/texcheck/            # ローカル alphaTex 検証ツール（§6.5）
└── app/                       # Android（Kotlin + Jetpack Compose）
    └── src/main/kotlin/dev/fumiwake/app/
        ├── score/             # AlphaTabView ラッパ, ScoreLocator, 再生コントローラ
        ├── ui/                # 3 画面（§5）
        └── data/              # SQLite 実装の ProgressStore, assets ローダ
```

**`settings.gradle.kts` の SDK ガード**（この計画の鍵。ローカルでは AGP 解決自体が
不可能なため必須）：

```kotlin
include(":core")
val localProps = File(rootDir, "local.properties")
val hasAndroidSdk = System.getenv("ANDROID_HOME") != null ||
    System.getenv("ANDROID_SDK_ROOT") != null ||
    (localProps.exists() && localProps.readText().contains("sdk.dir"))
if (hasAndroidSdk) include(":app") else println("Android SDK not found - :app skipped")
```

- assets 取込: `:app` の `sourceSets` で `assets.srcDirs("../content/exercises")`
  → asset パスは `rhythm/xxx.yaml`（`content/` 直下の他物が紛れ込まない）
- `:core` テストからの content 参照: システムプロパティ
  `fumiwake.content.dir`（Gradle が `rootDir/content/exercises` を注入）で解決。
  cwd 依存にしない

## 3. 技術選定（確定）

| 項目 | 選定 | 補足 |
| --- | --- | --- |
| Kotlin | 2.2.21（Maven Central 確認済み最新安定） | :core / :app 共通 |
| AGP | 8.11 系（Gradle 8.14.3 互換の安定版。M0 で互換表確認し確定） | 解決は CI のみ |
| ビルド | Gradle wrapper（CI・ユーザー用）＋ローカルは /opt/gradle 直接 | wrapper 生成はローカルで可（services.gradle.org 到達可） |
| UI | Jetpack Compose（BOM）＋ single Activity | AlphaTabView は `AndroidView{}` で埋込 |
| 譜面・再生 | alphaTab 1.8.4 | §1 の確認結果どおり |
| minSdk / target / compile | 26 / 35 / 36 | alphaTab の minSdk=26。compileSdk 36 は androidx.core 1.17（alphaTab の推移的依存）の要求 |
| シリアライズ | kotlinx.serialization + kaml 0.104.0（YAML） | リスクと代替は §8 R3 |
| 進捗保存 | Android 標準 SQLite（`SQLiteOpenHelper`、ライブラリなし） | 逸脱根拠は §9 |
| DI | なし（手書きコンストラクタ注入） | MVP 規模では不要 |

## 4. `:core` API 契約（並行作業のための境界定義）

実装エージェントはこのシグネチャを契約として実装・利用する。
変更が必要な場合はリーダーの承認を経て本節を先に更新する。

**エラー処理の統一方針**: パース系（`fromYaml`）は
`ExerciseFormatException`（`IllegalArgumentException` 派生、原因と対象を message に含む）
を投げる。検証系（`validate`）はエラーメッセージのリストを返す（空 = OK）。
呼び出し側（テスト・:app）はこの二系統のみを扱う。

```kotlin
// concept/
@Serializable
data class Concept(
    val id: String,            // "<domain>.<name>" 形式
    val nameJa: String,
    val dependsOn: List<String> = emptyList(),
)

class ConceptGraph(concepts: List<Concept>) {
    // init 時に検証: id 形式 / 依存先の存在 / 閉路なし（DAG）。違反は IllegalArgumentException
    val all: List<Concept>
    fun get(id: String): Concept?
    fun prerequisitesOf(id: String): Set<String>       // 推移的依存（全祖先）
    fun topologicalOrder(): List<Concept>              // 学習ナビの既定順（安定ソート）
    fun availableConcepts(learned: Set<String>): Set<String>  // 依存充足済み＆未習
    companion object { fun bundled(): ConceptGraph }   // DESIGN.md §2.1 の全概念を内蔵
}

// music/ — 表示用変換（純ロジック。:app に置かないこと）
object NoteNames {
    // midi=60 → "ド (C4)" 形式。臨時記号は "ド# (C#4)"。オクターブ表記は科学的ピッチ表記
    fun fromMidi(midi: Int): String
}
object DurationTexts {
    // (Quarter互換のenum値, dots) → "4分音符" / "付点4分音符"。isRest=true → "4分休符"
    fun of(durationValue: Int, dots: Int, isRest: Boolean = false): String
    // durationValue は alphaTab の Duration.value と同じ規約 (1=全,2=2分,4=4分,...)
    // :core は alphaTab に依存できないため Int で受ける。:app 側で Duration.value を渡す
}

// exercise/  — DESIGN.md §3.1 の Pydantic スキーマの Kotlin 版
@Serializable
data class FocusElement(
    val barIndex: Int,
    val beatIndex: Int,
    val noteIndex: Int? = null,   // null = 拍全体（休符など）
    val noteText: String,         // この出現箇所の文脈解説
)

@Serializable
data class Exercise(
    val exerciseId: String,       // 例 "rhythm.tie.001"（<concept_id>.<3桁> 形式）
    val conceptId: String,
    val prerequisites: List<String> = emptyList(),
    val titleJa: String,
    val alphatex: String,
    val explanationJa: String,
    val focusElements: List<FocusElement> = emptyList(),
    val difficulty: Int,          // 1..5
    val variantOf: String? = null,
)

object ExerciseLoader {
    fun fromYaml(yaml: String): Exercise                        // ExerciseFormatException
    fun validate(e: Exercise, graph: ConceptGraph): List<String> // 空リスト = OK
    // 検証項目: exerciseId 形式と conceptId の一致 / conceptId が graph に存在 /
    // prerequisites がタクソノミーの推移的依存の部分集合 / difficulty 1..5 /
    // alphatex 非空 / focusElements のインデックス非負 / variantOf の形式
    // ※ alphaTex の構文検証はここではやらない（texcheck / CI パーステストが担当 — §6.5）
}

// catalog/
class Catalog(val graph: ConceptGraph, exercises: List<Exercise>) {
    fun byConcept(conceptId: String): List<Exercise>   // difficulty 順（同値は exerciseId 順）
    fun byId(exerciseId: String): Exercise?
    val domains: List<String>                          // エクササイズが1件以上あるドメインのみ。
                                                       // 表示順: rhythm, pitch, guitar, structure, harmony
    fun conceptsInDomain(domain: String): List<Concept> // topological 順
}

// progress/
enum class ConceptStatus { LOCKED, AVAILABLE, LEARNED }

interface ProgressStore {                              // :app が SQLite で実装
    fun completedExercises(): Set<String>
    fun markCompleted(exerciseId: String)
    fun learnedConcepts(): Set<String>
    fun markConceptLearned(conceptId: String)
}

class ProgressEvaluator(
    private val graph: ConceptGraph,
    private val catalog: Catalog,
    private val store: ProgressStore,
) {
    fun statusOf(conceptId: String): ConceptStatus
    // LEARNED = store.learnedConcepts に含まれる
    // AVAILABLE = 全 dependsOn が LEARNED / LOCKED = それ以外

    fun recordCompletion(exerciseId: String): ConceptStatus
    // 完了を store に記録し、その概念の全エクササイズが完了済みなら
    // markConceptLearned まで行い、記録後の概念の状態を返す（昇格ロジックは :core で完結）
}

// InMemoryProgressStore（テスト用実装）も :core に同梱する
```

## 5. `:app` 画面構成（MVP の 3 画面）

1. **ConceptMapScreen（ホーム）** — ドメイン別に概念をリスト表示。
   **エクササイズが 1 件以上あるドメインのみ表示**（`Catalog.domains`。MVP では rhythm のみに
   なる想定。タクソノミー自体は全 25 概念を保持するが UI には出さない）。
   各概念に状態バッジ（🔒 LOCKED / ▶ AVAILABLE / ✓ LEARNED）。
   DAG の完全なグラフ描画は行わず、topological 順リスト＋依存の注記で MVP とする。
   LOCKED でも閲覧可能（強制はしない。ナビの推奨のみ）。
2. **ExerciseListScreen** — 概念タップで遷移。エクササイズを difficulty 順に表示。
   概念の説明（タクソノミー名＋依存概念）をヘッダに表示。
3. **ExerciseScreen（中核画面）** —
   - 上部: AlphaTabView（五線譜 + TAB。alphaTex を `api.tex()` で描画）
   - タップ: ScoreLocator で Beat/Note を特定 → `focusElements` に一致があれば
     その `noteText` を、なければ汎用情報（音名 = `NoteNames.fromMidi(note.realValue)`、
     音価 = `DurationTexts.of(...)`、フレット/弦 = Note モデル）を下部パネルに表示。
     タップした音は `playNote()` で発音
   - 下部パネル: 解説（`explanationJa`）⇄ タップ詳細の切替
   - 再生バー: 再生/停止、テンポスライダー（50–100%）、小節ループ切替
     （現在小節を `PlaybackRange` + looping で反復）
   - 「このエクササイズを完了」ボタン → `ProgressEvaluator.recordCompletion()`
     （昇格判定は :core。UI は返ってきた状態を表示するだけ）

### ScoreLocator の責務（リスク隔離点）

`:app/score` の `ScoreLocator` に以下を**すべて**閉じ込める：
1. View 座標（タッチイベント、スクロール・density 込み）→ boundsLookup 座標系への変換
2. boundsLookup ヒットテスト → alphaTab の Bar/Beat/Note モデル
3. alphaTab モデル ⇄ `FocusElement` の (barIndex, beatIndex, noteIndex) の対応付け
   （alphaTex の弦番号と `Note.string` の食い違い等、実測で判明した規約差もここで吸収）

実機で位置ずれが判明した場合の修正がこのクラスに限定されるようにする。

## 6. マイルストーンと作業分割

実装は Sonnet エージェントに委任し、リーダーが契約定義・統合・レビュー・CI 監視を行う。
**順序はスパイク優先**（レビュー指摘 B2 の反映）：

```
M0（雛形+CI） → M2a（スパイクAPK）→ ユーザーへ中間引き渡し（go/no-go）
        └→ M1（:core）/ M1.5（エクササイズ）/ M2b（フルUI）を並行 → M3 統合
```

M2a で alphaTab の描画・再生が Android 上で成立することを確認するのが Phase 0 スパイクの
実体。**M2a の結果を待たずに M1/M1.5/M2b を進める**（no-go でも :core とコンテンツは
レンダラ差し替え時に無傷で残る資産。損失は :app の UI 層のみ）。

### 並行作業の衝突回避規則（レビュー指摘 M3 の反映）

- 並行エージェントは **git worktree 隔離**で実行し、リーダーが順に統合する
- 書き込み領域を完全分離: M1 → `core/` のみ / M1.5 → `content/` のみ /
  M2a・M2b → `app/` のみ。**`settings.gradle.kts`・`libs.versions.toml`・
  `.github/` は M0 完成後凍結**（変更はリーダーのみ）
- そのため **M0 で全マイルストーンの依存を version catalog に先行登録する**
  （kaml, kotlinx-serialization, coroutines, Compose BOM, alphaTab, junit ほか）
- M1 の「content 全件検証テスト」は **ファイル 0 件なら fail** とし、統合順を
  M1 → M1.5 の順に固定（M1 単体のマージ時点では当該テストのみ `@Disabled`、
  M1.5 統合時に有効化）

### M0: リポジトリ雛形 + CI（Sonnet ①、先行・単独）

- Gradle マルチモジュール（§2、SDK ガード付き settings、`gradle wrapper` 生成）
- version catalog に**全依存を先行登録**、`.gitignore`、README 骨子
- `:core` 空パッケージ＋ダミーテスト（ローカル `gradle :core:test` グリーン必須）
- `:app` 最小構成（AGP + Compose + alphaTab 依存宣言、MainActivity は文字表示のみ）
- `.github/workflows/ci.yml`：
  - job `core-test`: `./gradlew :core:test`
  - job `android-build`: `./gradlew :app:testDebugUnitTest :app:assembleDebug`
    + `actions/upload-artifact` で `app-debug.apk` 保存
- **DoD**: ローカル `:core:test` グリーン。push 後 CI 両ジョブグリーン
  （`android-build` の初回グリーン確認までが M0。ここで AGP 互換・SDK ガードの
  実効性が証明される）

### M2a: スパイク画面（Sonnet ②、M0 完了後すぐ）

- MainActivity に AlphaTabView を置き、ハードコードした検証済み alphaTex
  1 譜例を描画・再生できるだけの画面（ボタン: 再生/停止、テンポ 50/75/100%、
  ループ ON/OFF。おまけ: 画面タップで boundsLookup の結果を Toast 表示）
- **DoD**: CI グリーン + APK artifact。→ **ユーザーに中間引き渡し**して実機確認
  （§7 の 1〜3 を先行判定）
- ※ここまでで DESIGN.md Phase 0 の 1〜3 に相当。4（MusicXML）は Phase 2 冒頭（§9）

### M1: `:core` ドメイン実装（Sonnet ③、M0 後に M2a と並行・worktree）

- §4 の契約を完全実装＋ DESIGN.md §2.1 の全 25 概念を `ConceptGraph.bundled()` に内蔵
- ユニットテスト：
  - DAG 検証（閉路検出、未知依存、id 形式）
  - bundled タクソノミーが DESIGN.md の表と一致（概念数 25・依存関係の全件チェック）
  - NoteNames / DurationTexts の変換（開放弦 EADGBE の MIDI 値ほか）
  - ExerciseLoader 正常系/異常系、Catalog 索引・順序
  - ProgressEvaluator: statusOf の 3 状態、recordCompletion の昇格（全完了→LEARNED）
  - content 全件検証テスト（0 件 fail。マージ時は @Disabled — 上記規則）
- **DoD**: ローカルで全テストグリーン

### M1.5: rhythm エクササイズ作成（Sonnet ④、M0 後に並行・worktree）

- 対象: `rhythm.beat` → `rhythm.basic-values` → `rhythm.rests` → `rhythm.dotted`
  → `rhythm.tie` の 5 概念 × 2〜3 譜例（計 12 前後）
- 1譜例1概念の原則。alphaTex は検証済み基本構文のみ（§1）。単一トラック・単旋律・
  ローポジション（開放〜3フレット中心）
- `focusElements` は概念の実例すべてに文脈解説（この位置のこのタイが何を意味するか）
- **作成の都度 `tools/texcheck` で全件パース検証（必須）**
- **DoD**: texcheck 全件パス＋（統合後）M1 の全件検証テストがグリーン

### M2b: `:app` フル UI（Sonnet ⑤、M2a 完了後・worktree）

- §5 の 3 画面＋ SQLite ProgressStore ＋ assets ローダ（kaml で YAML 読込）
- M2a のスパイクコードを ExerciseScreen に発展させる
- `:app` の JVM ユニットテスト: `ExerciseTexParseTest`（assets の全 YAML の alphatex を
  AlphaTexImporter でパース — CI の門番）、ScoreLocator の対応付けテスト
  （AlphaTexImporter で作った Score に対して実行 — JVM で動くことは実証済み）
- **DoD**: CI 両ジョブグリーン＝コンパイル成功＋パーステスト通過＋ APK 生成

### M3: 統合・CI グリーン化（リーダー）

- worktree 成果を M1 → M1.5 → M2b の順に統合（@Disabled 解除含む）、push、CI 監視
- CI 失敗時は原因特定 → 担当領域を Sonnet に再委任（軽微ならリーダー直接修正）
- 最終セルフレビュー（正しさ・境界・命名・冗長性）
- **DoD**: 両ジョブグリーン、APK artifact 取得可能、README に §7 の確認手順を記載

### §6.5 alphaTex 検証ツール `tools/texcheck`

AAR（Maven Central から curl 可能）の classes.jar + kotlin-stdlib +
kotlinx-coroutines-core-jvm で `AlphaTexImporter.initFromString → readScore` を実行する
小さな Java プログラム＋シェルスクリプト。`content/exercises/**/*.yaml` の `alphatex`
フィールドを全件パースし、診断（行・列付き）を表示、エラーがあれば非 0 終了。
依存 jar は初回に `tools/texcheck/lib/` へダウンロードしてキャッシュ（.gitignore 対象）。
動作原理はこの環境で実証済み。M0 の一部として作成する（M1.5 が使うため）。

## 7. ユーザー実機確認への引き渡し

**中間（M2a スパイク APK）**: Phase 0 go/no-go 判定
1. 譜面表示（五線譜 + TAB、Bravura フォントの描画品質）
2. 再生・テンポ変更・ループ（音が出るか、レイテンシ・音質）
3. タップで Beat が特定されるか（Toast の内容）

**最終（M2b APK）**:
4. エクササイズ一覧 → 譜面表示 → タップ解説（focusElements の位置ずれがないか）
5. 進捗の保存（アプリ再起動後も完了状態が残るか）と概念の昇格（LEARNED 遷移）

## 8. リスクと対応

| # | リスク | 影響 | 対応 |
| - | --- | --- | --- |
| R1 | `:app` をローカルビルドできず、コンパイルエラーの発見が CI まで遅延 | 統合の往復回数増 | :app のロジック最小化（:core へ寄せる）。M0 で CI を先に整備し、M2a/M2b 中も細かく push してビルドを回す |
| R2 | focusElements のアドレッシングが alphaTab 内部モデルとずれる | タップ解説の位置ずれ | ScoreLocator に全変換を集約（§5）。JVM 上で AlphaTexImporter 製 Score に対する対応付けテストを書く（実機依存を座標変換のみに縮小）。残りは M2a/実機確認 |
| R3 | kaml（snakeyaml-engine）が Android で動かない/重い | エクササイズ読込不可 | 発覚時は Gradle タスクで YAML→JSON 変換して assets には JSON を同梱（:core の JSON ロードは kotlinx.serialization 標準で無リスク） |
| R4 | ~~alphaTex 文法ミスを検証できない~~ → **解消** | — | AlphaTexImporter の JVM 実行を実証済み。texcheck（ローカル）+ ExerciseTexParseTest（CI）の二重検証 |
| R5 | AGP/Kotlin/Compose のバージョン非互換（ローカルでは検証不可能） | CI ビルド失敗 | M0 の DoD に「CI 初回グリーン」を含め、最小構成で早期に洗い出す。互換表に従い保守的に選定 |
| R6 | alphaTab の再生初期化（サウンドフォント）の作法が不明 | 音が出ない | AAR は sonivox.sf2 同梱＋ `soundFontLoaded` イベントあり（自動ロードの見込み）。M2a で `readyForPlayback` を待って play する実装とし、動かない場合は AlphaTabView のソース（GitHub の alphaTab リポジトリ）を確認して手動ロードに切替 |
| R7 | Android 実機での描画・発音が不成立（Phase 0 の本質リスク） | :app 手戻り | M2a を最優先で引き渡し早期判定。no-go でも :core / content は資産として残る構造 |

## 9. 設計（DESIGN.md）からの逸脱と根拠

| 項目 | DESIGN.md | 本計画 | 根拠 |
| --- | --- | --- | --- |
| スキーマ定義言語 | Pydantic（Python） | Kotlin data class + kotlinx.serialization | ランタイムと同一言語で単一ソース化。Pydantic はバリアント量産スクリプト（Phase 2 以降）で導入 |
| カタログの SQLite 化 | 起動時/ビルド時に SQLite カタログ構築 | MVP はメモリ内 Catalog（YAML 直読み）。SQLite は**進捗のみ** | エクササイズ数十件で SQLite 索引は過剰。進捗は永続化が必須なので SQLite を維持 |
| Phase 0 の MusicXML 検証 | 実装初日に go/no-go | Phase 2 冒頭へ後送 | 本環境で実機実行不可。Bundled MVP は alphaTex のみで完結し判定を待つ必要がない。なお描画・再生のスパイク（Phase 0 の 1〜3）は M2a として最優先で実施 |
| 概念マップ画面 | DAG 順の学習ナビ | topological 順リスト＋状態バッジ（グラフ描画なし）。エクササイズのあるドメインのみ表示 | MVP の学習ナビ機能は満たしつつ UI 実装を最小化 |

## 10. Git / CI 運用と配布

- ブランチ: `claude/design-review-overview-ulu50f`（指定ブランチ）に統合コミット
- 並行エージェントは worktree で作業し、リーダーがレビュー後に統合
- コミット粒度: マイルストーン単位＋統合修正。メッセージは `M0: ...` / `M1: ...` 形式
- CI: push 毎に core-test / android-build 両ジョブ実行。APK artifact の retention は 14 日

### 配布（ユーザー要望: スマホで直接ダウンロード→インストール）

- **main へのマージ時**、CI の release ジョブが APK をビルドし、
  GitHub Release（タグ `latest`）に `fumiwake.apk` として添付する
- スマホからの取得 URL（固定）:
  `https://github.com/ykenkou001/fumiwake/releases/latest/download/fumiwake.apk`
- `versionCode` は `GITHUB_RUN_NUMBER` から採番
- **署名**: GitHub Secrets（`SIGNING_KEYSTORE_BASE64` / `SIGNING_KEY_ALIAS` /
  `SIGNING_STORE_PASSWORD`）が登録済みなら固定鍵で署名した release APK を配布し、
  端末で上書きアップデート可能。未登録なら debug 署名（ランナーの一時鍵）に
  フォールバックし、更新時は旧版のアンインストールが必要。鍵はリポジトリに
  置かず Secrets に保持（平文パスワード付き鍵のコミットはしない）。手順は
  README「署名と上書きアップデート」を参照
- 開発中ブランチの確認は従来どおり Actions artifact（`app-debug`）を使用
