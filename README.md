# fumiwake（譜分け）

Score decomposition & comprehension app for acoustic guitar players — decomposes
sheet music into elements (rhythm, notes, structure), explains each element in
context, and lets you verify by ear via playback. See [`DESIGN.md`](DESIGN.md)
for the full design and [`docs/IMPLEMENTATION_PLAN.md`](docs/IMPLEMENTATION_PLAN.md)
for the implementation plan and milestone breakdown.

## Module layout

```
fumiwake/
├── core/                # Pure Kotlin/JVM domain logic. No Android dependency.
├── app/                 # Android app (Kotlin + Jetpack Compose + alphaTab).
├── content/exercises/   # Bundled exercise YAML (single source, also used as
│                         # :app asset content).
└── tools/texcheck/      # Local alphaTex syntax validator (see below).
```

`:app` is only included by `settings.gradle.kts` when an Android SDK is
detected (`ANDROID_HOME`/`ANDROID_SDK_ROOT` env var or `local.properties`
with `sdk.dir`). Without an SDK, `gradle`/`./gradlew` prints
`Android SDK not found - :app skipped` and only `:core` is buildable — this is
expected in SDK-less dev environments; `:app` is built and tested in CI.

## Building locally

Only `:core` can be built/tested without an Android SDK:

```
./gradlew :core:test
```

`:app` requires an Android SDK and is built/tested exclusively in CI
(GitHub Actions) — see `.github/workflows/ci.yml`. Do not attempt to build
`:app` locally without an Android SDK installed.

## alphaTex validation (`tools/texcheck`)

Every bundled exercise's `alphatex` field must actually parse with alphaTab's
`AlphaTexImporter`. `tools/texcheck` runs that same parser standalone on the
JVM, outside the Android app:

```
tools/texcheck/texcheck.sh                  # checks all content/exercises/**/*.yaml
tools/texcheck/texcheck.sh path/to/one.yaml  # checks specific file(s)
```

On first run it downloads the alphaTab AAR (for its `classes.jar`),
kotlin-stdlib, kotlinx-coroutines-core-jvm, and snakeyaml into
`tools/texcheck/lib/` (gitignored, cached across runs) from Maven Central.
It prints PASS/FAIL with bar counts or diagnostics per file and exits
non-zero if any file fails to parse.

## Getting the APK

`:app` isn't buildable locally in this project's reference dev environment
(no Android SDK). Debug APKs are built by the `android-build` CI job on every
push and uploaded as a GitHub Actions artifact named `app-debug`
(14-day retention) — download it from the workflow run's Summary page.

## スマホへのインストール（main マージ後）

main ブランチにマージされると、CI の `release` ジョブが署名済み APK をビルドし、
GitHub Release（タグ `latest`）に添付します。スマホのブラウザで次の URL を開くと
直接ダウンロードできます：

```
https://github.com/ykenkou001/fumiwake/releases/latest/download/fumiwake.apk
```

ダウンロード後、通知またはファイルアプリから APK を開いてインストールします。
初回のみ「提供元不明のアプリのインストール」をブラウザに許可する必要があります。

> **現状の制約（署名について）**: 当面は CI ランナーが生成する一時鍵による
> debug 署名で配布するため、**更新版を入れる前に旧バージョンのアンインストールが
> 必要**です（署名不一致のため上書きインストールが拒否されます。学習進捗も
> 消えます）。上書きアップデートに対応するには署名鍵の固定が必要で、
> 鍵を GitHub リポジトリの Secrets に登録して release ビルドに切り替える対応を
> 予定しています（`docs/IMPLEMENTATION_PLAN.md` §10 参照）。
