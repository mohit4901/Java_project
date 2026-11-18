#!/usr/bin/env bash
set -e

# Detect Java runtime
if [ -x "/opt/homebrew/opt/openjdk/bin/java" ]; then
    JAVA="/opt/homebrew/opt/openjdk/bin/java"
elif command -v java >/dev/null 2>&1; then
    JAVA="java"
else
    echo "Error: Java runtime not found."
    exit 1
fi

if [ ! -d "bin" ]; then
    echo "Compiling first..."
    ./build.sh
fi

"$JAVA" -cp bin com.timetabler.Main "$@"
