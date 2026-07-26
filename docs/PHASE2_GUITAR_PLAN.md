# Phase 2-A: ギター読解の核 実装計画 rev.2（実施済み）

> DESIGN.md rev.2 §6 Phase 2 の前半。**guitar ドメインを「実際に到達できて、
> 実際に読める」状態にする**ことがゴール。
>
> Phase 2 の後半（読解確認モード §4.4 / レイヤー再生 §4.3）は
> `docs/PHASE2_CHECK_PLAN.md` に分ける。指板図が本計画で出来ていることが
> 後半の前提になるため、本計画が先。
>
> **rev.2 = 実施後の記録**。着手前のスパイク（§5）で前提が 2 つ崩れたため、
> その結果と対応を本文に反映してある。DoD 5 項目はすべて達成し、CI は
> `core-test` / `android-build` とも green。残るのは実機確認（§8-5）のみ。

## 0. ゴール（Definition of Done）— 5/5 達成

1. ✅ **guitar ドメインが LOCKED から解ける** — `pitch.staff-names` の詰み
   （DESIGN.md §2.3）を `assumedKnown` で解消し、概念マップ上で guitar の
   概念が AVAILABLE になる
2. ✅ **主張↔実音の検証層が動いている**（DESIGN.md §3.4 の第三層）。
   guitar / pitch ドメインのコンテンツは、意図した音と実際に鳴る音の
   一致が CI で機械検証される
3. ✅ **guitar 4 概念 + pitch 3 概念にエクササイズが存在する**（計 15 譜例。
   既存 22 → 37）
4. ✅ **指板図が表示される** — 音符をタップすると、その音が指板のどこかが見える
5. ✅ 既存の検証がすべてグリーン（`:core:test` / `ExerciseTexParseTest` /
   `ExerciseMidiSemanticsTest`）

**スコープ外**（意図的に外す）：

- 読解確認モード・レイヤー再生（Phase 2-B）
- 持ち込み曲・インポート（Phase 3。技術スパイクも含めて着手しない）
- 定着の仕組み（Phase 4。設計は DESIGN.md にあるが実装しない）
- structure / harmony ドメインのコンテンツ
- チューニング変更・カポ（レギュラーチューニング固定。ただし `:core` の
  ロジックはチューニングを引数に取る形にしておき、後で開ける）
- 指板図への**入力**（タップして答える）。表示のみ。入力は Phase 2-B

## 1. 作業順序と依存関係

前提条件が 2 つあり、どちらもコンテンツ作成より**先**でなければならない。

```
§2 assumedKnown ─────────┐
                         ├─→ §6 コンテンツ 15 譜例 ─→ §7 指板図 UI
§3 スキーマ拡張 ─→ §4 :core ロジック ─→ §5 構文スパイク ─┘
                         └─→ §3.3 検証層（:app テスト）
```

- **§2 を先にやる理由**: これが無いと、コンテンツを追加しても画面上は 🔒 のまま。
  「作ったのに触れない」状態でコンテンツを書き進めるのは検証にならない
- **§3〜§5 を先にやる理由**: guitar のコンテンツは音高が主題であり、
  検証層が無い状態で 15 譜例書くと**間違いに気づけない**。rhythm では
  手作業の検算表（`RHYTHM_COMPLETION_PLAN.md` §7）で凌いだが、
  pitch↔fret の組み合わせは人手では必ず取りこぼす

## 2. 前提条件 A — `assumedKnown`（DESIGN.md §2.3）

### 2.1 変更点

**`:core/concept/Concept.kt`**

```kotlin
data class Concept(
    val id: String,
    val nameJa: String,
    val dependsOn: List<String> = emptyList(),
    val assumedKnown: Boolean = false,
)
```

`ConceptGraph.bundled()` で `pitch.staff-names` のみ `assumedKnown = true`。

**`:core/concept/ConceptGraph.kt`** — 派生集合を 1 つ追加する。

```kotlin
val assumedKnownIds: Set<String> = all.filter { it.assumedKnown }.map { it.id }.toSet()
```

**`:core/progress/ProgressEvaluator.kt`** — ここが本丸。現在の実装は

```kotlin
val learned = store.learnedConcepts()
```

を 2 箇所で使っている。これを**実効的な既習集合**に置き換える：

```kotlin
private fun effectiveLearned(): Set<String> =
    store.learnedConcepts() + graph.assumedKnownIds

fun statusOf(conceptId: String): ConceptStatus {
    val learned = effectiveLearned()
    if (conceptId in learned) return ConceptStatus.LEARNED
    val concept = graph.get(conceptId) ?: return ConceptStatus.LOCKED
    return if (learned.containsAll(concept.dependsOn)) ConceptStatus.AVAILABLE
           else ConceptStatus.LOCKED
}
```

**重要**: `assumedKnown` を LEARNED として返すだけでは不十分で、
**依存充足の判定に使う集合にも混ぜる**必要がある。`guitar.open-strings` が
AVAILABLE になるのは後者の効果。片方だけ直すと「`pitch.staff-names` は
既習と表示されるのに guitar は 🔒 のまま」という中途半端な状態になる。

`recordCompletion` の昇格ロジックは変更不要（`assumedKnown` の概念は
エクササイズを持たないため、そもそも通らない）。

### 2.2 UI（`:app/ui/ConceptMapScreen.kt`）

`assumedKnown` の概念は LEARNED と同じ緑ではなく「既習」バッジで区別する。
理由: ユーザーが自分で学んだのではなく、アプリが仮定しているだけだから。
将来オンボーディングで可変にする余地（DESIGN.md §2.3）を UI 上も残す。

エクササイズ 0 件なので、タップしてもエクササイズ一覧は空になる。
一覧画面に空状態のテキスト（「この概念は既習として扱っています」）を出す。

### 2.3 不変条件テスト（再発防止の門番）

`:core` に追加。同種の詰みを将来のドメイン追加で再発させないためのもの。

> **不変条件**: エクササイズを 1 件以上持つ概念は、その推移的依存の
> すべてが「エクササイズを持つ」または「`assumedKnown`」でなければならない。

「エクササイズを持つ概念だけ」を対象にするのが要点。harmony / structure は
まだコンテンツが無く、`pitch.key-signatures` にも依存しているので、
全概念を対象にすると今は必ず落ちる。**到達可能性が問題になるのは
「そこへ行けと UI が誘っている概念」だけ**である。

配置は `ContentExercisesTest` と同じ仕組み（システムプロパティ
`fumiwake.content.dir` 経由で `content/exercises/` を読む）。
新規ファイル `core/src/test/kotlin/.../concept/ConceptReachabilityTest.kt`。

失敗メッセージは詰みの内容がそのまま分かる形にする：

```
guitar.open-strings はエクササイズを持つが、依存する pitch.staff-names は
エクササイズ 0 件かつ assumedKnown=false のため永久に LEARNED にならない
```

## 3. 前提条件 B — 主張↔実音の検証層（DESIGN.md §3.4 第三層）

### 3.1 何を検証すべきかの再定義

DESIGN.md §3.4 は第三層を「記譜された音高と `fret`/`string` の組が一致するか」と
書いたが、**alphaTex ではこれは原理的に不一致になりえない**。alphaTex の
`fret.string.duration` は運指を書き、音高はそこから導出されるからである。
検証としては空振りする。

実際に起きる誤りは 3 つで、いずれも**「コンテンツの主張」と「実際の音」のズレ**である：

| # | 誤りの形 | 例 | 現行の検証で捕まるか |
| - | --- | --- | --- |
| E1 | 弦番号の打ち間違い | ド(C3) を出すつもりで `3.5` と書くところ `3.4` → F3 が鳴る。解説文は「ド」のまま | ❌ 構文も MIDI 意味も通る |
| E2 | ポジションの逸脱 | ローポジションの譜例に 7 フレットが混ざる | ❌ |
| E3 | 「同じ音」の主張が偽 | multi-positions で代替位置として挙げた組が実は別の音 | ❌ |

したがって**第三層は「コンテンツに主張を書かせ、その主張を機械が検算する」形**にする。
主張が無ければ検証は成立しない。これがスキーマ拡張（§3.2）が要る理由である。

### 3.2 スキーマ拡張（Q2 の決定を含む）

2 つの**任意**フィールドを追加する。任意なので既存 22 譜例は無改修。

**`FocusElement.expectPitch: String? = null`** — その位置で鳴るべき音の主張。
科学的音名（`"C3"`, `"F#4"`）で書く。

**`Exercise.fretRange: FretRange? = null`** — 譜例が想定するフレット範囲。

```kotlin
@Serializable
data class FretRange(val min: Int, val max: Int)
```

`ExerciseLoader.validate` に加える規則：

- `expectPitch` は **`guitar.*` と `pitch.*` の focusElements では必須**
  （この 2 ドメインは音高そのものが主題なので、主張なしを許さない）。
  他ドメインでは任意
- `expectPitch` は科学的音名としてパースできること
- `fretRange` は `0 <= min <= max <= 24`
- `fretRange` は **`guitar.*` では必須**（E2 を検出可能にするため）

#### Q2（`multi-positions` の代替ポジションをどう表現するか）の決定

**決定: 代替ポジションはコンテンツに書かない。`:core` の `FretPositions` が
音高から列挙する。**

当初の選択肢は「`noteText` に文章で書く」か「構造化して持つ」かだったが、
**第三の道が正しい**。代替ポジションは音高とチューニングから一意に決まる
導出可能な情報であり、著述すべきデータではない。

- 文章で書く案 → 機械検証できない（E3 が素通りする）。却下
- リストを著述する案 → 導出できるものを手で書くのは、書き間違いという
  新しい誤りの源を作るだけ。却下
- **導出する案** → 検証も表示も 1 つのロジックから出る。採用

副次効果として、`fretRange` が「どの範囲を代替ポジションとみなすか」の
境界を与える。`multi-positions.002` は `fretRange: {min: 0, max: 10}` と
書いてあり、指板図はそこまでを描き、代替ポジションの列挙もそこで打ち切られる。
絞らないと、ローポジションの譜例にネックの上の方の運指が並んでしまう。

この決定は Phase 2-B にも効く。§4.4 の確認モードが「この音はどのフレット？」の
**正解判定**をするとき、代替ポジションも正解として受け入れる必要がある。
その判定器が本計画で先に出来ることになる。

### 3.3 検証の実装（`:app` の新規 JVM テスト）

`ExerciseMidiSemanticsTest` と同じ枠組み。`alphaTab` に依存するため `:app` 側。
新規ファイル `app/src/test/kotlin/.../ExerciseFingeringTest.kt`。

3 つのテスト：

1. **`:core` の音高計算が alphaTab と一致する**（チューニングモデルの合意確認）。
   全譜例の全音符について `FretPositions.midiOf(string, fret) == note.realValue`。
   これは指板図が乗る土台そのものなので、1 度きりではなく常時検証する
2. **`expectPitch` が実際の音と一致する**（E1 を捕まえる）。
   `FocusElement` の位置を `ScoreLocator` と同じ規約で引き当て、
   宣言された音名と実音を突き合わせる
3. **全音符が `fretRange` に収まる**（E2 を捕まえる）

E3 は §3.2 の決定により**構造的に発生しなくなる**（代替ポジションを
著述しないので、偽の主張を書く余地が無い）。テストは不要。

#### 記譜音と実音の区別 — 検証前に確定させる

ギターは**移調楽器**で、五線には実音より 1 オクターブ高く書かれる
（開放 6 弦は実音 E2、五線上は E3 の位置）。`expectPitch` がどちらを指すかを
曖昧にすると、検証は通るのに解説文が全部 1 オクターブずれる。

**規約: `expectPitch` は実音（鳴る音）を書く。** 理由: 指板の位置と
1 対 1 で対応し、`FretPositions` の出力とも一致するため、検証も表示も
変換を挟まない。五線上の見え方に言及する必要がある場合は `noteText` に書く。

alphaTab 側でどのプロパティが実音でどのプロパティが記譜音かは
**§5 のスパイクで確定させる**（`Note.realValue` / `Note.displayValue` の
どちらがどちらか、実測で確認してから検証コードを書く）。ここを推測で
書くと、テストが通るのに全部ずれているという最悪の形になる。

## 4. `:core` に追加するロジック

Android 非依存・ローカルテスト可能。本プロジェクトの中心原則
（Android でしか動かないコードを最小化する）に従う。

### 4.1 `music/Tuning.kt`

```kotlin
data class Tuning(val stringMidi: List<Int>) {
    companion object {
        /** レギュラーチューニング。index 0 = 1弦（最高音）。 */
        val STANDARD = Tuning(listOf(64, 59, 55, 50, 45, 40))
    }
}
```

弦番号の規約は **alphaTex に合わせて 1 弦 = 最高音**。既存コンテンツの
`0.5`（= A2、開放 5 弦）がこの規約であることを確認済み。

**alphaTab のモデルは逆順**（`Note.string` は 1 = 最低音）。`:core` は
alphaTab の番号を見ない。変換は `:app` の `ScoreLocator` に閉じる — §5.2。

### 4.2 `music/FretPositions.kt`

```kotlin
data class FretPosition(val string: Int, val fret: Int)  // string は 1 始まり

object FretPositions {
    fun midiOf(string: Int, fret: Int, tuning: Tuning = Tuning.STANDARD): Int
    fun positionsOf(midi: Int, maxFret: Int = 12, tuning: Tuning = Tuning.STANDARD): List<FretPosition>
}
```

`positionsOf` は弦の低い方（＝6 弦側）から順に返す — 指板図で下から上へ
並ぶ順序と一致させ、UI 側でソートし直さずに済ませる。

テスト（`:core`）:

- 開放弦 6 本が期待 MIDI を返す
- `midiOf(5, 3) == 48`（C3）
- `positionsOf(45)`（A2）が `[(6,5), (5,0)]` を返す
- `positionsOf` の全要素が `midiOf` で元の値に戻る（往復不変）
- `maxFret` 境界（12 フレットちょうどが含まれる）

### 4.3 `music/NoteNames.kt` の拡張

現在は `fromMidi(midi): String` のみで、`"ド (C4)"` という表示用の形を返す。
`expectPitch` の検証には**逆変換**が要る。

```kotlin
fun toMidi(scientific: String): Int?   // "C3" -> 48, "F#4" -> 66, "Bb3" -> 58, 不正なら null
```

♭ 表記も受ける（`Bb3`）。ギターの解説では ♯ と ♭ が文脈で使い分かれるため、
コンテンツ著述側を ♯ に縛らない。内部では MIDI に正規化されるので
検証結果は表記に依存しない。

テスト: 往復（`toMidi(fromMidi(n)) == n` の音名部分）、異名同音
（`toMidi("A#3") == toMidi("Bb3")`）、不正入力が null。

## 5. alphaTex 構文の事前スパイク（コンテンツ作成の前）

`RHYTHM_COMPLETION_PLAN.md` §1 と同じ手順。`tools/texcheck` で
**本文を書く前に**全構文の通過を確認する。guitar ドメインは rhythm より
使う構文が多いので、ここを飛ばすと手戻りが大きい。

### 5.1 実施結果（全項目クリア。ただし前提が 2 つ崩れた）

| # | 確認事項 | 結果 |
| - | --- | --- |
| P1 | `\tuning e4 b3 g3 d3 a2 e2` | ✅ PASS。ただし**既定値と同じ**（`tuning=[64,59,55,50,45,40]`）なので明示は任意 |
| P2 | `\staff{score tabs}` | ✅ PASS。ただし**既定で `showStandardNotation` も `showTablature` も true**。五線+TAB 併記に特別な構文は要らない |
| P3 | `\ks C` 等の調号指定 | ✅ PASS |
| P4 | 高音側 `12.1` / 低音側 `0.6` | ✅ PASS |
| P5 | 実音と記譜音の別 | ✅ **`realValue` = 実音、`displayValue` = 記譜音**。`displayTranspositionPitch = -12` が出ており、記譜が実音の 1 オクターブ上であることが実測で確認できた |
| P6 | 和音 `(0.6 2.5 2.4).4` | ✅ PASS（本計画では使わない。記録のみ） |

§3.3 の規約（`expectPitch` は実音 = `realValue`）は P5 により裏づけられた。

### 5.2 崩れた前提 1 — alphaTab の弦番号は alphaTex と逆

**最も危険な発見。** alphaTex の `0.6.4`（6 弦開放）をパースすると、
alphaTab のモデル上は `Note.string == 1` になり、`realValue == 40`（E2）を返す。

| | 1 = | 6 = |
| --- | --- | --- |
| alphaTex の `fret.string.duration` / ギタリストの数え方 / 本アプリ | 最高音弦（細い E4） | 最低音弦（太い E2） |
| **alphaTab の `Note.string`** | **最低音弦（E2）** | **最高音弦（E4）** |

計画 §4.1 は「弦番号は alphaTex に合わせて 1 弦 = 最高音」と書いており、
この規約自体は正しい（コンテンツも参照表 §6.4 もギタリストの数え方が自然）。
危険なのは §3.3 の検証テストで、`FretPositions.midiOf(note.string, note.fret)` を
alphaTab の `note.string` にそのまま渡す想定だった点である。**そのまま実装すると
全弦が入れ替わったまま検算が回り、実装を「直す」方向を間違える**。

**対応**: 変換は `:app` の `ScoreLocator` に置く（「alphaTab モデル ⇄ 本アプリの
アドレッシングはすべてここ」という既存の責務そのもの）。`:core` は
alphaTab の番号を一切見ない。弦数はハードコードせず、その譜面の
チューニング長から取る。

### 5.3 崩れた前提 2 — TAB は既に出ている

`\staff{score tabs}` は既定と同じで、既存の rhythm 22 譜例も五線と TAB を
併記して描画されている。`tab-mapping` の 2 譜例に必要なのは**新しい構文ではなく
新しい解説**（TAB の数字が何を指すかを言語化すること）だと分かった。
§10 S9 と §11 に置いた「`\staff` が通らなければ `tab-mapping` を落とす」
撤退ラインは**不要になったので撤回する**。

### 5.4 texcheck への反映

`tools/texcheck/TexCheck.java` に音符ごとの
`string / fret / realValue / displayValue` ダンプを足す。コンテンツ著述中に
「今書いた譜例が何の音を鳴らすか」を確認する一次手段になる。

**`:app` の JUnit テストはこの開発環境では動かない**（Android SDK が無く
`:app` はスキップされる。`:core:test` のみ実行可能）。したがって
§3.3 の検証は CI でしか回らない。手元で同じ検証を回せるよう、
**texcheck 側にも `expectPitch` / `fretRange` の検算を実装する**。
二重実装になるが、手元で落ちないものを CI で初めて発見する運用は
コンテンツ 15 譜例に対して回転が悪すぎる。

### 5.5 `tools/ktcheck` — `:app` の型チェック手段

同じ制約の別の面として、**`:app` の Kotlin コードは手元でコンパイルすら
できない**。テストコードの型の誤りを CI の往復で潰すのは遅すぎるため、
Kotlin コンパイラを直接叩く `tools/ktcheck/ktcheck.sh` を用意した。

- 対象は **Android / Compose API に触れないソース**のみ（`android.jar` が無い）。
  `app/.../score/` 配下とテストはこれで通せる。UI コードは CI 頼み
- `-opt-in=kotlin.contracts.ExperimentalContracts` を渡す点が要（alphaTab の
  公開 API がこの注釈を持つため、無いと全参照が opt-in エラーになる）

## 6. コンテンツ — 15 譜例

### 6.1 一覧

実装済み。`fretRange` は実際に書いた内容に合わせて確定した値。

| exerciseId | 概念 | 主題 | fretRange | difficulty |
| --- | --- | --- | --- | --- |
| pitch.ledger-lines.001 | ledger-lines | 五線の上に出る音（1 弦を上がって加線が増える） | 0–8 | 2 |
| pitch.ledger-lines.002 | ledger-lines | 五線の下に出る音（低音弦側） | 0–3 | 2 |
| pitch.accidentals.001 | accidentals | ♯ = 1 フレット上、♮ で戻る | 0–4 | 2 |
| pitch.accidentals.002 | accidentals | ♭ = 1 フレット下。♯ との異名同音 | 0–4 | 3 |
| pitch.octaves.001 | octaves | 同じ音名・違うオクターブ（3 組） | 0–3 | 2 |
| pitch.octaves.002 | octaves | 3 オクターブの「ミ」を加線の数で読み分ける | 0–2 | 3 |
| guitar.open-strings.001 | open-strings | 6 本の開放弦と五線の対応 | 0–0 | 1 |
| guitar.open-strings.002 | open-strings | 開放弦だけで上って下りる（3→2 弦だけ幅が狭い） | 0–0 | 1 |
| guitar.low-position.001 | low-position | 5・6 弦のローポジション | 0–3 | 2 |
| guitar.low-position.002 | low-position | 1〜3 弦のローポジション | 0–3 | 2 |
| guitar.low-position.003 | low-position | 6 弦を通してローポジションで上がる（Cメジャー） | 0–3 | 3 |
| guitar.multi-positions.001 | multi-positions | 同じ音を開放弦と 1 本下の弦の 5 フレットで | 0–5 | 3 |
| guitar.multi-positions.002 | multi-positions | 同じフレーズをローポジションと 7〜10 フレットで | 0–10 | 4 |
| guitar.tab-mapping.001 | tab-mapping | 五線と TAB を見比べる（線 = 弦、数字 = フレット） | 0–3 | 2 |
| guitar.tab-mapping.002 | tab-mapping | 同じ数字でも弦が違えば別の音 | 0–7 | 3 |

**prerequisites**（タクソノミーの推移的依存の部分集合であること）：

- `pitch.ledger-lines` / `pitch.accidentals` → `["pitch.staff-names"]`
- `pitch.octaves` → `["pitch.ledger-lines"]`
- `guitar.open-strings` → `["pitch.staff-names"]`
- `guitar.low-position` → `["guitar.open-strings"]`
- `guitar.multi-positions` → `["guitar.low-position", "pitch.octaves"]`
- `guitar.tab-mapping` → `["guitar.low-position"]`

### 6.2 pitch.accidentals を含める理由

DAG 上、guitar ドメインは `pitch.accidentals` に依存していない。
必須なのは `ledger-lines` と `octaves` の 2 概念（`multi-positions` が
`pitch.octaves` を要求するため）だけである。

それでも含めるのは、**指板は半音単位で並んでおり、♯♭ を避けたコンテンツでは
指板の構造そのものを教えられない**ため。1 フレット = 半音 = ♯ 1 つ、という
対応はギター読解の基礎であって、後回しにすると `low-position` の解説文が
不自然に ♯♭ を迂回することになる。

### 6.3 設計原則

- **1 譜例 1 概念**（既存原則の継承）。ただし rhythm と逆で、**リズムを固定して
  音高だけを動かす**。全音符 4 分音符・`\ts 4 4`・`\tempo 60` を基本とし、
  リズムが注意を奪わないようにする
- **`expectPitch` は全 focusElement に書く**（§3.2 の必須規則）。
  これがコンテンツの主張であり、検証の入力
- **`fretRange` は実際に使う範囲ぴったりに書く**。広めに書くと E2 の検出力が
  落ちる。「この譜例はここしか使わない」という主張として書く
- **`noteText` は 1 文**（`ExerciseScreen` の解説パネルが `heightIn(max = 132.dp)`
  と狭いため。`RHYTHM_COMPLETION_PLAN.md` §8.2 の反映を継承）
- **`explanationJa` は 2〜3 文**（同上）
- **音名表記の統一**: 解説文中は `NoteNames.fromMidi` と同じ「ド (C3)」形式に
  揃える。指板図・タップ解説・解説文で表記が割れると、同じ音だと分からない

### 6.4 ローポジション参照表（著述時の一次資料）

0〜4 フレット・全 6 弦の実音。`FretPositions.midiOf` の期待値でもあり、
§4.2 のテストケースの出どころでもある。

| 弦 \ フレット | 0 | 1 | 2 | 3 | 4 |
| --- | --- | --- | --- | --- | --- |
| 1 弦 | E4 | F4 | F#4 | G4 | G#4 |
| 2 弦 | B3 | C4 | C#4 | D4 | D#4 |
| 3 弦 | G3 | G#3 | A3 | A#3 | B3 |
| 4 弦 | D3 | D#3 | E3 | F3 | F#3 |
| 5 弦 | A2 | A#2 | B2 | C3 | C#3 |
| 6 弦 | E2 | F2 | F#2 | G2 | G#2 |

（実音。五線上は 1 オクターブ上に書かれる — §3.3 の規約）

`low-position.003` の C メジャー音階は 5 弦 3 フレット（C3）から
4 弦・3 弦を使って上行する形にすると、この表の範囲に収まる。

## 7. 指板図 UI

### 7.1 責務分割

- **`:core`**: どの位置にどの音があるか（§4.2）。Android 非依存
- **`:app`**: 描画のみ。`FretboardView`（Compose Canvas）

### 7.2 コンポーネント

```kotlin
@Composable
fun FretboardView(
    fretRange: FretRange,
    highlighted: List<FretPosition>,   // 主たる位置（濃い色）
    alternatives: List<FretPosition>,  // 同じ音の代替位置（薄い色）
    modifier: Modifier = Modifier,
)
```

- 6 本の弦を横線、フレットを縦線で描く。3・5・7・9・12 にポジションマーク
- `fretRange` の幅に応じて横幅を配分する。`0–0`（開放弦のみ）でも
  破綻しないこと（`open-strings` の 2 譜例がこのケース）
- **入力は受けない**（Phase 2-B のスコープ）

### 7.3 `ExerciseScreen` への組み込み

現在、音符タップは `panelText` を上書きするだけ（`ExerciseScreen.kt:143`）。
ここに指板図を足す。

- タップされた音符の `(string, fret)` を `ScoreLocator` 経由で取得し
  `highlighted` に、`FretPositions.positionsOf(midi, fretRange.max)` の
  残りを `alternatives` に渡す
- **指板図は `fretRange` を持つ譜例でのみ表示する**。rhythm の 22 譜例は
  `fretRange` が null なので何も表示されず、既存画面は無変更で済む。
  リズムの譜例に指板図が出ても意味がない（全部同じ位置）ので、これが正しい挙動
- 配置は楽譜と解説パネルの間。解説パネルが狭いので、指板図に高さを
  取られすぎないよう固定高さ（目安 120dp）で置く

### 7.4 既知の未解決（本計画では触らない）

`RHYTHM_COMPLETION_PLAN.md` §9-2 で挙がった「解説⇄タップ詳細の切替 UI が無い」
問題は未解決のまま残る。Phase 2-B の確認モード（読解モード ⇄ 確認モード切替）が
同じ場所に手を入れるので、**そこで一緒に解く**のが自然。本計画で別の切替を
足すと二重になる。

## 8. 検証フロー

1. 各 YAML 作成の都度 `tools/texcheck/texcheck.sh <file>`（構文 + §5 の音高ダンプ）
2. `./gradlew :core:test` — スキーマ検証・到達可能性・FretPositions・NoteNames
3. `./gradlew :app:testDebugUnitTest` — 構文 / MIDI 意味 / **記譜↔運指**（新規）
4. push → CI グリーン確認
5. 実機確認（Android SDK はこの開発環境に無いため、ここだけは手元では閉じない）:
   - guitar / pitch 概念が 🔒 でなくなり、`pitch.staff-names` が「既習」と出ること
   - 音符タップで指板図の正しい位置が光ること
   - `multi-positions` で代替位置が白抜きの丸で出ること
   - rhythm の譜例では指板図が**出ない**こと（`fretRange` を持たないため）
   - 開放弦のみの譜例（`fretRange` 0–0）で指板図が潰れないこと
   - `accidentals` の譜例で ♯/♭ が意図どおりの向きで描かれること
     （alphaTab の異名同音の綴り選択はこちらで指定していない。MIDI 上は正しいので
     検証は通るが、見た目が ♭ 側に倒れる可能性がある。倒れていたら `\ks` で調整する）

## 9. コミット粒度

前提条件とコンテンツを混ぜない。混ぜると、検証層が正しいのか
コンテンツが正しいのかの切り分けができなくなる。

1. `feat: 既習概念（assumedKnown）を導入して guitar ドメインの詰みを解消`（§2）
2. `feat: 指板位置の計算を :core に追加`（§4）
3. `feat: 記譜と運指の一致を検証する層を追加`（§3。スキーマ拡張 + `:app` テスト）
4. `chore: texcheck に音高ダンプを追加`（§5。P5 の実測結果を含む）
5. `feat: pitch ドメイン 3 概念のエクササイズを追加`（§6 のうち pitch 6 譜例）
6. `feat: guitar ドメイン 4 概念のエクササイズを追加`（§6 のうち guitar 9 譜例）
7. `feat: 指板図を追加`（§7）

3 が 5・6 より前にあるのが重要。**検証層が動いている状態でコンテンツを書く**
ことで、書いた端から間違いが落ちる。

## 10. 自己レビュー（テックリード視点）と反映

| # | 指摘 | 対応 |
| - | --- | --- |
| S1 | DESIGN.md §3.4 の「記譜↔運指の一致」は alphaTex では原理的に成立しない検証。計画に書き写すと、意味のないテストを実装して「第三層は済んだ」と誤認する | §3.1 で検証対象を「コンテンツの主張 vs 実際の音」に再定義。主張を持たせるためのスキーマ拡張（§3.2）まで含めて第三層とする |
| S2 | `assumedKnown` を `statusOf` の戻り値にだけ効かせると、依存充足判定に入らず guitar は 🔒 のまま。「直したのに直っていない」で終わる | §2.1 で `effectiveLearned()` として**両方**に効かせることを明示。実装時の落とし穴として本文に残す |
| S3 | ギターの記譜は実音より 1 オクターブ上。`expectPitch` の定義を曖昧にすると、検証は全部通るのに解説文が全部ずれる | §3.3 で「実音を書く」と規約化。alphaTab のどのプロパティが実音かは §5 P5 で**実測してから**検証コードを書く |
| S4 | 到達可能性テストを全概念対象にすると harmony / structure で必ず落ち、テストを無効化する圧力がかかる | §2.3 で対象を「エクササイズを持つ概念」に限定。UI が誘導する先だけを守る |
| S5 | `fretRange` を広めに書くとポジション逸脱（E2）の検出力がゼロになる。任意フィールドは緩く書かれがち | §3.2 で `guitar.*` は必須に。§6.3 で「実際に使う範囲ぴったりに書く」を著述原則として明記 |
| S6 | 指板図を全譜例に出すと、rhythm の 22 譜例で無意味な図が常時出る | §7.3 で `fretRange` を持つ譜例に限定。既存コンテンツが null なので自動的に除外され、既存画面は無変更 |
| S7 | Q2 の当初の二択（文章 vs 構造化）はどちらも機械検証できないか、書き間違いを招く | §3.2 で第三の道（`:core` から導出）を採用。Phase 2-B の正解判定器を兼ねる |
| S8 | 15 譜例を一度に書くと、途中で構文の壁に当たったとき手戻りが大きい | §5 のスパイクを全構文について**先に**通す。`RHYTHM_COMPLETION_PLAN.md` §1 で有効だった手順の踏襲 |
| S9 | `\staff{score tabs}` が通らない場合、`tab-mapping` の 2 譜例が書けない | ~~§5 P2 で事前確認。通らなければ概念ごと Phase 2-B へ送る~~ → **不要になった**。TAB は既定で描画されている（§5.3）。`tab-mapping` に要るのは構文ではなく解説 |
| S10 | （スパイクで判明）alphaTab の弦番号が alphaTex と逆順。気づかずに検証テストを書くと、全弦が入れ替わったまま検算が回り、実装を直す方向を間違える | §5.2。変換を `ScoreLocator` に閉じ、`:core` は alphaTab の番号を見ない。`Tuning` の KDoc にも明記して再発を防ぐ |
| S11 | `:app` のテストはこの開発環境で実行できない（Android SDK 不在）。第三層が CI でしか回らないと、コンテンツ 15 譜例の回転が悪すぎる | §5.4。texcheck 側にも同じ検算を実装し、手元で閉じられるようにする |

## 11. リスクと撤退ライン

| リスク | 影響 | 撤退ライン |
| --- | --- | --- |
| ~~`\staff{score tabs}` が使えない~~ | — | **解消**。TAB は既定で描画される（§5.3） |
| ~~alphaTab の実音/記譜音プロパティが期待と違う~~ | — | **解消**。`realValue` = 実音と実測（§5.1 P5） |
| 指板図の描画が実機で崩れる | UI のみの問題 | コンテンツと検証層は独立して価値があるので、指板図だけ次イテレーションへ送れる（§9 のコミット 7 が最後にあるのはこのため） |

## 12. 関連ドキュメント

- `DESIGN.md` §2.3 / §3.2 / §3.4 / §4.2 / §6 — 本計画の設計上の根拠
- `docs/PHASE2_CHECK_PLAN.md` — Phase 2 後半（未作成。本計画の完了後）
- `docs/RHYTHM_COMPLETION_PLAN.md` — コンテンツ追加イテレーションの先例
- `docs/AUDIO_DEFECT_PLAN.md` — 機械検証を原則とするに至った障害の記録
