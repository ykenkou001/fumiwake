# rhythm ドメイン完成 実装計画 rev.1

> 現状 MVP（Phase 0–1）では rhythm ドメイン 9 概念のうち 5 概念
> （beat / basic-values / rests / dotted / tie）にしかエクササイズが無い。
> 本イテレーションは残る 4 概念にエクササイズを追加し、rhythm ドメインを
> タクソノミー上「学習経路が最後までつながる」状態にする。
> rev.1 の自己レビュー結果は §6 に反映済み。

## 0. ゴール（Definition of Done）

`ConceptGraph.bundled()` の rhythm ドメイン全 9 概念それぞれに
エクササイズが 1 件以上存在し、以下を満たす：

1. 新規 4 概念（sixteenth / triplet / syncopation / compound-meter）に
   各 2〜3 譜例（既存概念と同密度）
2. **全譜例が `tools/texcheck` の AlphaTexImporter パースを通過**
3. `:core` の content 全件検証テスト（`ExerciseLoader.validate` 相当）がグリーン
4. `:app` の `ExerciseTexParseTest`（CI 門番）がグリーン（＝コンパイル・パース）
5. 追加コンテンツは既存の YAML スキーマ・命名・解説文トーンに準拠

**スコープ外**（今回やらないことの明示）：
- 新しい alphaTex 構文を要する装飾（ゴーストノート・ベンド等）
- pitch / guitar / structure / harmony ドメインのコンテンツ
- `:app` / `:core` のコード変更（純粋にコンテンツ追加のみ）
- UI の変更（概念マップは `Catalog.domains` を自動反映するため追加不要）

## 1. 前提の検証（実施済み）

`tools/texcheck` で新規に使う構文が AlphaTexImporter を通ることを実機パースで確認済み：

| 構文 | 用途 | 検証結果 |
| --- | --- | --- |
| `0.5.16` | 16分音符 | PASS |
| `0.5.8{tu 3}` | 3連符（tuplet 3） | PASS |
| `\ts 6 8` | 複合拍子（6/8） | PASS |
| `0.5.8{d} 0.5.16` | 付点8分＋16分 | PASS（§dotted 既存構文の再利用） |
| タイ `-.duration` と 16分の併用 | シンコペーション | PASS |

→ 新規構文リスク（IMPLEMENTATION_PLAN §1 の「検証済み構文のみ」制約）はクリア。

## 2. 追加するエクササイズ一覧

命名は既存規約 `<concept_id>.<3桁>` に従う。全て単一トラック・単旋律・
ローポジション（開放弦 `0.5` 中心）、`\tempo 60`、`\ts` 明示。

| exerciseId | 概念 | 主題 | difficulty |
| --- | --- | --- | --- |
| rhythm.sixteenth.001 | sixteenth | 4分→8分→16分と刻みを細かく | 1 |
| rhythm.sixteenth.002 | sixteenth | 付点8分＋16分（タッカのリズム） | 2 |
| rhythm.sixteenth.003 | sixteenth | 16分と8分の混在（1拍内の順番違い） | 2 |
| rhythm.triplet.001 | triplet | 1拍を3つに割る基本 | 2 |
| rhythm.triplet.002 | triplet | 8分（2分割）と3連符（3分割）の対比 | 2 |
| rhythm.syncopation.001 | syncopation | 弱拍アクセント（8分＋4分＋…＋8分） | 3 |
| rhythm.syncopation.002 | syncopation | タイで拍をまたぐシンコペーション | 3 |
| rhythm.compound-meter.001 | compound-meter | 6/8を「付点4分×2拍」で感じる | 3 |
| rhythm.compound-meter.002 | compound-meter | 6/8内のグルーピング変化 | 3 |

**prerequisites**（タクソノミーの推移的依存の部分集合であること）：
- sixteenth / triplet → `["rhythm.basic-values"]`
- syncopation → `["rhythm.tie", "rhythm.sixteenth"]`（DAG の直接依存）
- compound-meter → `["rhythm.triplet"]`

## 3. 各譜例の設計原則

- **1譜例1概念**（IMPLEMENTATION_PLAN §M1.5 の原則を継承）。音高は固定
  （`0.5` 開放5弦）にしてリズムだけに注意を向ける＝既存 basic-values と同方針
- **拍数の厳密性**: 各小節が拍子どおりの合計長になることを 16分音符単位で検算
  （4/4 = 16単位、6/8 = 12単位）。§7 の検算表で全小節を確認
- **focusElements の文脈解説**: 概念の実例が最初に現れる位置に、その位置での
  意味を書く（汎用辞書ではなく「この拍のこの3連符」）。休符は `noteIndex: null`
- **beatIndex は小節内の Beat 出現順（0始まり）**。タイ継続 `-.x` や3連符の各音も
  1 Beat として数える（alphaTex の Beat 分割に一致）

## 4. 検証フロー

1. 各 YAML 作成の都度 `tools/texcheck/texcheck.sh <file>` でパース確認
2. 全 9 ファイル作成後 `tools/texcheck/texcheck.sh`（全件）でリグレッション確認
3. `./gradlew :core:test` — content 全件検証（validate）がグリーン
4. push → CI（`android-build` の `ExerciseTexParseTest`）がグリーンを確認

## 5. 作業手順（コミット粒度）

1. 4 概念 × 譜例を作成（本計画 §2）
2. texcheck 全件パス、`:core:test` グリーンを確認
3. 単一コミット `feat: rhythm ドメイン残り4概念のエクササイズを追加` で
   指定ブランチ `claude/next-feature-planning-33hx0a` に push
4. CI グリーン確認

## 6. 自己レビュー（テックリード視点）と反映

| # | 指摘 | 対応 |
| - | --- | --- |
| S1 | 新規構文（tuplet・16分・6/8）が未検証のまま本文を書くと手戻り | **先に texcheck で全構文を実機検証**（§1）。検証後に本作成へ |
| S2 | syncopation の prerequisites に sixteenth を含めるが、譜例で必ずしも16分を使わないと概念間の依存が形骸化 | syncopation の少なくとも1譜例で16分を用い、もう1譜例でタイを用いて、両依存（tie・sixteenth）を実例で裏づける |
| S3 | 拍数の数え間違いは texcheck では検出できない（小節線 `\|` を跨げば別小節としてパスしてしまう） | §7 の検算表で全小節を16分音符単位で明示検算。加えて texcheck の bar 数出力が想定小節数と一致することを確認 |
| S4 | difficulty のばらつき基準が曖昧 | 概念の認知的負荷で相対設定：単独概念の導入=低め、複数概念の合成（syncopation=tie+16分、compound=拍子+3連的分割）=高め。同一概念内は易→難で採番 |
| S5 | focusElements の barIndex/beatIndex が実際の描画とずれると解説位置がずれる（ScoreLocator 依存） | 位置検算を §7 に含める。ただし最終的な位置ずれ確認は実機（IMPLEMENTATION_PLAN §7 の 4）に委ねる — 本計画の責務はスキーマ的妥当性まで |
| S6 | コンテンツ追加で概念マップ画面に新概念が出るが、依存元（例: compound は triplet 必須）が LOCKED のままだと導線が不自然 | これは既存 `ProgressEvaluator` の設計どおり（依存充足で AVAILABLE 昇格）。コンテンツ追加の責務外。UI 変更なしで正しく機能することを確認するに留める |

## 7. 拍数・位置の検算表（4/4=16単位, 6/8=12単位。16分音符=1単位）

各行「小節: 各Beatの単位数 = 合計」。合計が拍子の単位数に一致することを確認する。

### sixteenth.001（4/4=16）
- bar0: 4,4,2,2,2,2 = 16 ✓（q q e e e e）
- bar1: 1,1,1,1,2,2,4,4 = 16 ✓（16×4 e e q q）
- bar2: 1,1,1,1,1,1,1,1,4,4 = 16 ✓（16×8 q q）
- bar3: 16 = 16 ✓（whole）

### sixteenth.002（4/4=16）— 付点8分=3, 16分=1
- bar0: 3,1,3,1,4,4 = 16 ✓
- bar1: 4,4,3,1,3,1 = 16 ✓
- bar2: 3,1,3,1,3,1,3,1 = 16 ✓
- bar3: 16 ✓

### sixteenth.003（4/4=16）
- bar0: 1,1,2,2,1,1,4,4 = 16 ✓
- bar1: 2,1,1,1,1,2,4,4 = 16 ✓
- bar2: 1,1,2,2,1,1,2,2,4 = 16 ✓
- bar3: 16 ✓

### triplet.001（4/4=16）— 3連8分の各音 = 4/3 単位、3つで4単位（1拍）
- bar0: 4,4,(4/3×3=4),4 = 16 ✓（q q [trip] q）
- bar1: (4)+(4)+4+4 = 16 ✓（[trip][trip] q q）
- bar2: 4,(4),(4),4 = 16 ✓（q [trip][trip] q）
- bar3: 16 ✓

### triplet.002（4/4=16）
- bar0: (e e=4),(trip=4),4,4 = 16 ✓
- bar1: (trip=4),(e e=4),4,4 = 16 ✓
- bar2: (e e=4),(trip=4),(e e=4),(trip=4)... 検算は作成時に確定
- bar3: 16 ✓

### syncopation.001（4/4=16）
- bar0: 2,4,4,4,2 = 16 ✓（e q q q e：内側の4分が弱拍に乗る）
- bar1: 作成時に確定（合計16を保つ）

### syncopation.002（4/4=16）— タイ `-.x` を含む
- 作成時に検算（タイ継続も単位数に含める）

### compound-meter.001（6/8=12）— 8分=2, 付点4分=6
- bar0: 2,2,2,2,2,2 = 12 ✓（8分6つ）
- bar1: 6,6 = 12 ✓（付点4分×2＝6/8の2拍）
- bar2: 4,2,4,2 = 12 ✓（q e q e：各拍=付点4分ぶん）
- bar3: 6,6 = 12 ✓

### compound-meter.002（6/8=12）
- 作成時に検算（合計12を保つ）

### triplet.002（4/4=16）
- bar0: (e e=4),(trip=4),4,4 = 16 ✓
- bar1: (trip=4),(e e=4),4,4 = 16 ✓
- bar2: (e e=4),(trip=4),(e e=4),4 = 16 ✓
- bar3: 16 ✓

### syncopation.002（4/4=16）— タイ `-.4` を含む
- bar0: 4,2,2,(tie 4),4 = 16 ✓（q e e[tie→q] q。2拍裏の8分を3拍へタイ）
- bar1: 4,4,4,4 = 16 ✓（表拍対比）
- bar2: 4,2,2,(tie 4),4 = 16 ✓
- bar3: 16 ✓

### compound-meter.002（6/8=12）
- bar0: 2,2,2,4,2 = 12 ✓
- bar1: 4,2,2,2,2 = 12 ✓
- bar2: 2,4,2,4 = 12 ✓
- bar3: 6,6 = 12 ✓

全 9 ファイルの texcheck bar 数（各 4 小節）が本表と一致することを確認済み。

## 8. 追加レビュー（音楽家／ユーザー目線）と反映

計画の自己レビュー（§6, テックリード視点）に加え、2 つの実利用視点でレビューし反映した。

### 8.1 作曲家・ギタリスト視点
| 指摘 | 反映 |
| --- | --- |
| 同一音・無アクセントだとシンコペーション/複合拍子が“聞こえない”（長短の羅列に化ける） | syncopation は裏拍の音を、compound-meter は 3+3 グループ頭の音を変えて可聴化（ローポジ運指 `2.5`=B, `3.5`=C を使用） |
| 音価比較・分割対比は同一音が正解（変数を分離） | sixteenth / triplet は単一ピッチ `0.5` を維持 |
| テンポ 60 はグルーヴが出ない | syncopation / compound-meter を `\tempo 66` に微増 |
| リズム学習にクリック/カウントインが無い（最大の欠陥） | 本イテレーション外。§9 の推奨フォローアップに記載 |

### 8.2 ユーザー視点（`ExerciseScreen` の実装確認に基づく）
| 指摘 | 反映 |
| --- | --- |
| 解説パネルは `heightIn(max=132.dp)` と狭く、長文はスクロール強制 | `explanationJa` を 2〜3 文、`noteText` を 1 文に統一 |
| 音符タップで `panelText` が上書きされ、解説へ戻す導線が無い | コンテンツ側では解決不可。§9 の推奨フォローアップに記載 |

## 9. 推奨フォローアップ（本イテレーション外・次の候補）

1. **メトロノーム／クリック＋カウントイン**（DESIGN.md §4 の "click track" 未実装）。
   リズム学習の中核体験。追加コンテンツより価値が高い可能性。`:app` の再生制御に追加。
2. **解説⇄タップ詳細の切替 UI**（IMPLEMENTATION_PLAN §5.3 の当初設計）。
   `ExerciseScreen` に「解説にもどる」ボタン／タブを追加（小規模なコード改修）。
