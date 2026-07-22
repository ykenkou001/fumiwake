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

### 署名と上書きアップデート

`release` ジョブは、GitHub Secrets に署名鍵が登録されていれば **固定鍵で署名した
release APK** を配布します（端末で**アンインストール不要の上書きアップデート**が
可能）。未登録なら CI ランナーの一時鍵による debug 署名で配布します（署名が毎回
変わるため、更新時は旧版のアンインストールが必要。学習進捗も消えます）。

固定署名を有効にする手順（一度だけ）:

1. 署名鍵を作る（お手元の PC。`keytool` は JDK 付属）:

   ```bash
   keytool -genkeypair -v \
     -keystore fumiwake-release.keystore \
     -alias fumiwake \
     -keyalg RSA -keysize 2048 -validity 10000 \
     -storetype PKCS12 \
     -dname "CN=fumiwake, O=ykenkou001"
   ```

   実行時にパスワードを設定する。**この鍵ファイルとパスワードは安全に保管・
   バックアップし、リポジトリにはコミットしないこと**（失うと以後の上書き
   アップデートができなくなる）。

2. base64 テキストに変換: `base64 fumiwake-release.keystore > keystore.b64.txt`

3. リポジトリの **Settings → Secrets and variables → Actions** で 3 つ登録:
   - `SIGNING_KEYSTORE_BASE64` = ステップ2の base64 テキスト
   - `SIGNING_KEY_ALIAS` = `fumiwake`
   - `SIGNING_STORE_PASSWORD` = ステップ1のパスワード

4. 次の main 更新（または `release` ジョブの再実行）から署名付き APK になる。

> **注意**: 現在 debug 署名版が端末に入っている場合、**最初の署名付き版へ切り替える
> 1 回だけ**は署名不一致でアンインストールが必要。それ以降は上書きアップデートが
> できる。個人配布（サイドロード）向けの運用であり、Play ストア配布時は
> 鍵の管理方針を別途見直すこと。
