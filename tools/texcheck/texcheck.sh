#!/usr/bin/env bash
# Validate alphaTex snippets embedded in content exercise YAML files by
# actually parsing them with alphaTab's AlphaTexImporter (JVM), the same
# parser :app uses at runtime. See docs/IMPLEMENTATION_PLAN.md §6.5.
#
# Usage:
#   tools/texcheck/texcheck.sh                 # checks content/exercises/**/*.yaml
#   tools/texcheck/texcheck.sh file1.yaml ...   # checks specific files
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
LIB_DIR="$SCRIPT_DIR/lib"

ALPHATAB_AAR_URL="https://repo1.maven.org/maven2/net/alphatab/alphaTab/1.8.4/alphaTab-1.8.4.aar"
KOTLIN_STDLIB_URL="https://repo1.maven.org/maven2/org/jetbrains/kotlin/kotlin-stdlib/2.2.21/kotlin-stdlib-2.2.21.jar"
COROUTINES_URL="https://repo1.maven.org/maven2/org/jetbrains/kotlinx/kotlinx-coroutines-core-jvm/1.10.2/kotlinx-coroutines-core-jvm-1.10.2.jar"
SNAKEYAML_URL="https://repo1.maven.org/maven2/org/yaml/snakeyaml/2.3/snakeyaml-2.3.jar"

KOTLIN_STDLIB_JAR="$LIB_DIR/kotlin-stdlib-2.2.21.jar"
COROUTINES_JAR="$LIB_DIR/kotlinx-coroutines-core-jvm-1.10.2.jar"
SNAKEYAML_JAR="$LIB_DIR/snakeyaml-2.3.jar"
ALPHATAB_AAR="$LIB_DIR/alphaTab-1.8.4.aar"
ALPHATAB_JAR="$LIB_DIR/alphatab-classes.jar"

mkdir -p "$LIB_DIR"

download() {
    local url="$1" dest="$2"
    if [ -f "$dest" ]; then
        return 0
    fi
    echo "Downloading $(basename "$dest")..." >&2
    curl -sS -L -o "$dest" "$url"
}

download "$KOTLIN_STDLIB_URL" "$KOTLIN_STDLIB_JAR"
download "$COROUTINES_URL" "$COROUTINES_JAR"
download "$SNAKEYAML_URL" "$SNAKEYAML_JAR"

if [ ! -f "$ALPHATAB_JAR" ]; then
    download "$ALPHATAB_AAR_URL" "$ALPHATAB_AAR"
    echo "Extracting classes.jar from alphaTab AAR..." >&2
    tmp_extract="$(mktemp -d)"
    unzip -oq "$ALPHATAB_AAR" classes.jar -d "$tmp_extract"
    cp "$tmp_extract/classes.jar" "$ALPHATAB_JAR"
    rm -rf "$tmp_extract"
fi

CLASSPATH="$ALPHATAB_JAR:$KOTLIN_STDLIB_JAR:$COROUTINES_JAR:$SNAKEYAML_JAR"

if [ ! -f "$LIB_DIR/TexCheck.class" ] || [ "$SCRIPT_DIR/TexCheck.java" -nt "$LIB_DIR/TexCheck.class" ]; then
    echo "Compiling TexCheck.java..." >&2
    javac -classpath "$CLASSPATH" -d "$LIB_DIR" "$SCRIPT_DIR/TexCheck.java"
fi

if [ "$#" -gt 0 ]; then
    files=("$@")
else
    files=()
    while IFS= read -r -d '' f; do
        files+=("$f")
    done < <(find "$REPO_ROOT/content/exercises" -name '*.yaml' -print0 | sort -z)
fi

if [ "${#files[@]}" -eq 0 ]; then
    echo "No YAML files to check."
    exit 0
fi

java -Dfile.encoding=UTF-8 -classpath "$LIB_DIR:$CLASSPATH" TexCheck "${files[@]}"
