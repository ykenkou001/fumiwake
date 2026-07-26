#!/usr/bin/env bash
# Type-check Android-free Kotlin sources from :app without an Android SDK.
#
# Why this exists: the authoring environment has no Android SDK, so Gradle skips :app
# entirely ("Android SDK not found - :app skipped"). Everything under
# app/src/main/kotlin/dev/fumiwake/app/score/ and app/src/test/ is plain JVM Kotlin that
# happens to live in :app, and pushing it to CI just to learn it does not compile is a
# slow loop. This runs the Kotlin compiler directly over the files you name.
#
# Limits, stated plainly:
#  - Only works for sources that do not touch the Android or Compose APIs (no android.jar
#    here). UI code still has to go through CI.
#  - Compiles against the same alphaTab AAR that tools/texcheck downloads, and against
#    :core's jar, so run `./gradlew :core:jar` first if :core changed.
#
# Usage:
#   tools/ktcheck/ktcheck.sh app/src/main/kotlin/dev/fumiwake/app/score/ScoreLocator.kt ...
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
LIB_DIR="$SCRIPT_DIR/lib"
TEXCHECK_LIB="$REPO_ROOT/tools/texcheck/lib"

if [ "$#" -eq 0 ]; then
    echo "usage: $0 <kotlin-file> [...]" >&2
    exit 2
fi

mkdir -p "$LIB_DIR"

# alphaTab / kotlin-stdlib / coroutines は texcheck が用意する。まだなら取りに行かせる。
if [ ! -f "$TEXCHECK_LIB/alphatab-classes.jar" ]; then
    echo "Fetching alphaTab via texcheck..." >&2
    "$REPO_ROOT/tools/texcheck/texcheck.sh" >/dev/null
fi

find_cached() {
    find /root/.gradle "$HOME/.gradle" -name "$1" 2>/dev/null | head -1
}

download() {
    local url="$1" dest="$2"
    [ -f "$dest" ] && return 0
    echo "Downloading $(basename "$dest")..." >&2
    curl -sS -L -o "$dest" "$url"
}

KOTLIN_VERSION="$(sed -n 's/^kotlin *= *"\(.*\)"/\1/p' "$REPO_ROOT/gradle/libs.versions.toml" | head -1)"
KOTLIN_VERSION="${KOTLIN_VERSION:-2.2.21}"
MAVEN="https://repo1.maven.org/maven2/org/jetbrains/kotlin"

COMPILER_JAR="$(find_cached "kotlin-compiler-embeddable-${KOTLIN_VERSION}.jar")"
if [ -z "$COMPILER_JAR" ]; then
    COMPILER_JAR="$LIB_DIR/kotlin-compiler-embeddable-${KOTLIN_VERSION}.jar"
    download "$MAVEN/kotlin-compiler-embeddable/${KOTLIN_VERSION}/kotlin-compiler-embeddable-${KOTLIN_VERSION}.jar" "$COMPILER_JAR"
fi

DAEMON_JAR="$(find_cached "kotlin-daemon-embeddable-${KOTLIN_VERSION}.jar")"
if [ -z "$DAEMON_JAR" ]; then
    DAEMON_JAR="$LIB_DIR/kotlin-daemon-embeddable-${KOTLIN_VERSION}.jar"
    download "$MAVEN/kotlin-daemon-embeddable/${KOTLIN_VERSION}/kotlin-daemon-embeddable-${KOTLIN_VERSION}.jar" "$DAEMON_JAR"
fi

# kotlin-reflect はコンパイラ自身が実行時に使う。バージョンが合わないと起動すらしない。
REFLECT_JAR="$(find_cached "kotlin-reflect-${KOTLIN_VERSION}.jar")"
if [ -z "$REFLECT_JAR" ]; then
    REFLECT_JAR="$LIB_DIR/kotlin-reflect-${KOTLIN_VERSION}.jar"
    download "$MAVEN/kotlin-reflect/${KOTLIN_VERSION}/kotlin-reflect-${KOTLIN_VERSION}.jar" "$REFLECT_JAR"
fi

ANNOTATIONS_JAR="$(find_cached "annotations-13.0.jar")"
if [ -z "$ANNOTATIONS_JAR" ]; then
    ANNOTATIONS_JAR="$LIB_DIR/annotations-24.0.1.jar"
    download "https://repo1.maven.org/maven2/org/jetbrains/annotations/24.0.1/annotations-24.0.1.jar" "$ANNOTATIONS_JAR"
fi

TROVE_JAR="$(find_cached "trove4j-*.jar")"
JUNIT_JAR="$(find_cached "junit-4*.jar")"
HAMCREST_JAR="$(find_cached "hamcrest-core-*.jar")"

CORE_JAR="$REPO_ROOT/core/build/libs/core.jar"
if [ ! -f "$CORE_JAR" ]; then
    echo "Building :core jar..." >&2
    (cd "$REPO_ROOT" && ./gradlew --console=plain -q :core:jar)
fi

STDLIB_JAR="$TEXCHECK_LIB/kotlin-stdlib-2.2.21.jar"
COROUTINES_JAR="$TEXCHECK_LIB/kotlinx-coroutines-core-jvm-1.10.2.jar"

# コンパイラ自身を動かすための classpath（-cp）と、対象コードが参照する classpath
# (-classpath) は別物。前者に coroutines と reflect が要る。
RUNTIME_CP="$COMPILER_JAR:$STDLIB_JAR:$DAEMON_JAR:$REFLECT_JAR:$COROUTINES_JAR:$ANNOTATIONS_JAR"
[ -n "$TROVE_JAR" ] && RUNTIME_CP="$RUNTIME_CP:$TROVE_JAR"

TARGET_CP="$TEXCHECK_LIB/alphatab-classes.jar:$STDLIB_JAR:$COROUTINES_JAR:$CORE_JAR"
[ -n "$JUNIT_JAR" ] && TARGET_CP="$TARGET_CP:$JUNIT_JAR"
[ -n "$HAMCREST_JAR" ] && TARGET_CP="$TARGET_CP:$HAMCREST_JAR"

OUT_DIR="$(mktemp -d)"
trap 'rm -rf "$OUT_DIR"' EXIT

# -opt-in は app/build.gradle.kts と同じ。alphaTab の公開 API が
# @ExperimentalContracts で注釈されているため、これが無いと全参照が opt-in エラーになる。
set +e
java -cp "$RUNTIME_CP" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler \
    -nowarn \
    -jvm-target 17 \
    -opt-in=kotlin.contracts.ExperimentalContracts \
    -classpath "$TARGET_CP" \
    -d "$OUT_DIR" \
    "$@" 2>&1 | grep -v "unable to find kotlin-\(stdlib\|reflect\|script-runtime\).jar in the Kotlin home"
status=${PIPESTATUS[0]}
set -e

if [ "$status" -ne 0 ]; then
    echo "ktcheck: FAILED" >&2
    exit "$status"
fi

echo "ktcheck: OK"
