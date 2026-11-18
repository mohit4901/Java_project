#!/usr/bin/env bash
set -e

# Detect Java compiler
if [ -x "/opt/homebrew/opt/openjdk/bin/javac" ]; then
    JAVAC="/opt/homebrew/opt/openjdk/bin/javac"
    JAVA="/opt/homebrew/opt/openjdk/bin/java"
elif command -v javac >/dev/null 2>&1; then
    JAVAC="javac"
    JAVA="java"
else
    echo "Error: Java compiler (javac) not found. Please install OpenJDK."
    exit 1
fi

echo "Using Java Compiler: $JAVAC"
"$JAVAC" -version

mkdir -p bin

echo "Compiling Java sources..."
find src/main/java -name "*.java" > sources.txt
"$JAVAC" -d bin @sources.txt
rm sources.txt

echo "Compilation successful! Classes output to bin/"
