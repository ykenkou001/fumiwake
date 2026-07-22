# fumiwake（譜分け）— Design Document

> Score decomposition & comprehension app for acoustic guitar players.
> 楽譜を要素に分解して意味を理解し、その音を耳で確かめる「読解ツール」。

## 1. One-line Definition

**fumiwake is a score *reader/analyzer*, not a performance trainer.**
It decomposes sheet music into elements (notes, rhythm, structure, harmony),
explains each element in context, and lets the user verify by ear via playback.

- ❌ NOT: real-time pitch detection / performance grading（マイク聴音・演奏判定はスコープ外。初期構想から撤回済み）
- ✅ IS: view → decompose → explain → listen

### Target user (initial: the author himself)

- Can read note names on the staff (ドレミは読める)
- Cannot yet read: rhythm notation, key signatures, note→fret mapping, structural symbols
- Plays acoustic guitar

## 2. Core Concept: Concept-Driven Curriculum（概念ドリブン）

The app is organized around a **taxonomy of notation concepts**, not around songs.
Each bundled exercise isolates exactly ONE concept (1譜例1概念の原則).
User-imported songs are analyzed against the same taxonomy
(→ "this song contains 7 ties; ties not yet learned" のような未習概念ハイライト).

### 2.1 Concept Taxonomy (initial draft)

Concepts form a **DAG (dependency graph)**, not a linear sequence.
`concept_id` naming: `<domain>.<name>`

#### Domain: rhythm（最優先領域 — 学習者の最初の壁）

| concept_id            | name（日本語）        | depends_on                  |
| --------------------- | --------------------- | --------------------------- |
| rhythm.beat           | 拍と拍子（4/4の意味） | —                           |
| rhythm.basic-values   | 音価の基本（全〜8分） | rhythm.beat                 |
| rhythm.rests          | 休符                  | rhythm.basic-values         |
| rhythm.dotted         | 付点                  | rhythm.basic-values         |
| rhythm.tie            | タイ                  | rhythm.basic-values         |
| rhythm.sixteenth      | 16分音符              | rhythm.basic-values         |
| rhythm.syncopation    | シンコペーション      | rhythm.tie, rhythm.sixteenth|
| rhythm.triplet        | 3連符                 | rhythm.basic-values         |
| rhythm.compound-meter | 複合拍子（6/8等）     | rhythm.triplet              |

#### Domain: pitch（既知の拡張）

| concept_id           | name（日本語）       | depends_on           |
| -------------------- | -------------------- | -------------------- |
| pitch.staff-names    | 五線上の音名（既習） | —                    |
| pitch.ledger-lines   | 加線                 | pitch.staff-names    |
| pitch.accidentals    | ♯♭♮（臨時記号）      | pitch.staff-names    |
| pitch.key-signatures | 調号                 | pitch.accidentals    |
| pitch.octaves        | オクターブの読み分け | pitch.ledger-lines   |

#### Domain: guitar（このアプリ固有の価値 — 音符→運指変換）

| concept_id             | name（日本語）             | depends_on                            |
| ---------------------- | -------------------------- | ------------------------------------- |
| guitar.open-strings    | 開放弦と五線の対応         | pitch.staff-names                     |
| guitar.low-position    | ローポジションの音符→フレット | guitar.open-strings                |
| guitar.multi-positions | 同じ音の複数ポジション     | guitar.low-position, pitch.octaves    |
| guitar.tab-mapping     | TAB譜⇄五線譜の読み替え     | guitar.low-position                   |

#### Domain: structure

| concept_id             | name（日本語）                  | depends_on          |
| ---------------------- | ------------------------------- | ------------------- |
| structure.repeats      | 反復記号（リピート、1・2番括弧）| rhythm.beat         |
| structure.dc-ds        | D.C. / D.S. / Coda              | structure.repeats   |
| structure.articulation | スラー・スタッカート等          | rhythm.basic-values |

#### Domain: harmony（後段。MVP外）

| concept_id         | name（日本語）        | depends_on                              |
| ------------------ | --------------------- | --------------------------------------- |
| harmony.chords     | コード記号            | guitar.low-position                     |
| harmony.keys       | キーと調号の対応      | pitch.key-signatures                    |
| harmony.diatonic   | ダイアトニックコード  | harmony.chords, harmony.keys            |
| harmony.progressions | 進行の型            | harmony.diatonic                        |

## 3. Content Architecture: Two Inlets, One Pipeline

```
[bundled exercises]      [user-imported songs]
  alphaTex (text)          MusicXML / Guitar Pro (.gp)
        \                       /
         v                     v
        alphaTab loader → internal score model
                    |
        +-----------+-----------+
        |           |           |
     render      decompose    playback
   (staff+TAB)  (tap→explain) (synth)
```

- **Bundled side（同梱）** = systematic learning. Author-controlled, one concept per
  exercise, written in **alphaTex** (text format → Git-friendly, diffable, generatable).
  著作権的にもクリーン（自作エクササイズ or パブリックドメインのみ）。
- **Imported side（持ち込み）** = real-world application. MusicXML (from MuseScore etc.)
  and Guitar Pro files. Private use by the user; no redistribution.
- **Never bundle copyrighted songs.**（既存ヒット曲の同梱は配布にあたるためNG）

### 3.1 Exercise Schema (Pydantic draft)

```python
from pydantic import BaseModel

class FocusElement(BaseModel):
    """譜面中のどの要素が概念の実例か（タップ時ハイライト＋解説の紐付け）"""
    bar_index: int
    beat_index: int          # position within bar
    note_index: int | None   # None = whole beat (e.g. rest)
    note_text: str           # explanation for THIS occurrence, in context

class Exercise(BaseModel):
    exercise_id: str             # e.g. "rhythm.tie.001"
    concept_id: str              # primary concept — exactly one
    prerequisites: list[str]     # concept_ids (redundant w/ taxonomy, for validation)
    title_ja: str
    alphatex: str                # score body（テキスト。Git管理・生成が効く）
    explanation_ja: str          # concept explanation tied to this exercise
    focus_elements: list[FocusElement]
    difficulty: int              # 1..5, within-concept ordering
    variant_of: str | None       # exercise_id of the base variant
```

Storage: exercises as YAML/JSON files in-repo; a small **SQLite catalog**
(concept tags, dependency edges, user progress) built at app startup or build time.

Variant generation（バリエーション量産）is a first-class workflow: scripts that
take a rhythm pattern and emit alphaTex across keys/strings.
（Logi-Intel の「スキーマ→成果物生成」パターンと同型）

## 4. Interaction Model: Decompose by Layer

Decomposition is hierarchical; each layer pairs a *view* with a *listen* mode.

| Layer    | Decompose（見る）                       | Listen（聴く）                     |
| -------- | --------------------------------------- | ---------------------------------- |
| Note     | tap → 音名・音価・♯♭・フレット位置      | play single note (guitar / sine)   |
| Rhythm   | 音高を消して音価だけ表示                | click track + rhythm-only playback |
| Bar      | 1小節の要素一覧                         | loop bar, tempo 50–100%            |
| Phrase   | repeats / sections map（曲の地図）      | play section                       |
| Harmony  | chord symbols, key context（後段）      | chords-only layer                  |

Key principle: **explanations are contextual** — the symbol is explained
*as it appears in this score*, not as a detached glossary entry.
（汎用記号辞典ではなく「この曲のこのタイ」を起点に学ぶ）

## 5. Tech Stack Decisions

| Area            | Decision                                   | Status      |
| --------------- | ------------------------------------------ | ----------- |
| Platform        | Android native (Kotlin)                    | decided     |
| Render + synth  | **alphaTab** (Android flavor)              | decided*    |
| Bundled format  | alphaTex                                   | decided     |
| Import formats  | MusicXML, Guitar Pro (.gp3–.gp)            | decided     |
| Exercise schema | Pydantic (authoring) → YAML/JSON in repo   | decided     |
| Catalog/progress| SQLite                                     | decided     |
| Playback        | alphaTab built-in synth (MIDI+SoundFont2, AudioTrack) | decided |

\* **Known risk / first spike**: alphaTab's MusicXML support is documented
inconsistently ("experimental" in README vs "Mature" in format docs).
Must verify with real MuseScore-exported files.
Fallback candidate for import fidelity: OpenSheetMusicDisplay (render-only,
playback separate) — evaluate only if alphaTab MusicXML proves insufficient.

## 6. MVP Scope & Ordering

**MVP definition（一文）**: 同梱エクササイズ1式を読み込み、五線譜とTABを並べて
表示し、音符をタップすると音名・音価・フレット位置と解説が出て、
その音／その小節を（テンポ可変で）再生できる。

### Phase 0 — Technical spike（実装初日）
1. Embed alphaTab in a minimal Android app
2. Render an alphaTex snippet (staff + TAB)
3. Play it via built-in synth; verify tempo change + bar loop
4. Feed 2–3 real MusicXML files; assess fidelity → go/no-go on import path

### Phase 1 — Bundled MVP
- Exercise schema + catalog (SQLite)
- rhythm domain exercises first（rhythm.beat → rhythm.tie あたりまで）
- Tap-to-explain (focus_elements) + note/bar playback
- Concept map screen (DAG順の学習ナビ)

### Phase 2 — Import
- MusicXML / .gp import
- Concept scan of imported songs → 未習概念ハイライト
- Same decompose/listen UX applied to imported scores

### Phase 3+ (later)
- harmony domain, section maps, spaced repetition of missed concepts,
  sine-wave tone mode, layer separation (melody/chords/click)

## 7. Non-Goals

- Real-time performance judgment (mic input, pitch detection)
- Bundling copyrighted commercial songs
- iOS / web (until core UX proven on Android)
- Social features, cloud sync (initial versions)

## 8. Open Questions

- [ ] alphaTab MusicXML fidelity（Phase 0 spike で判定）
- [ ] rhythm domain の粒度：rhythm.basic-values を全/2分/4分/8分に分割するか
- [ ] focus_elements のアドレッシングが alphaTab の内部モデルと素直に対応するか
- [ ] 解説文の管理：exercise 内に埋め込み vs concept 単位で共有＋文脈差分
