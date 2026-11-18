#!/usr/bin/env bash
set -e

if [ -x "/opt/homebrew/opt/openjdk/bin/javac" ]; then
    JAVAC="/opt/homebrew/opt/openjdk/bin/javac"
    JAVA="/opt/homebrew/opt/openjdk/bin/java"
elif command -v javac >/dev/null 2>&1; then
    JAVAC="javac"
    JAVA="java"
else
    echo "Error: Java not found."
    exit 1
fi

mkdir -p bin

echo "Compiling test sources..."
find src/main/java src/test/java -name "*.java" > test_sources.txt
"$JAVAC" -d bin @test_sources.txt
rm test_sources.txt

echo "Running test suite..."
"$JAVA" -cp bin com.timetabler.TimetableTest
