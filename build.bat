@echo off
echo Compiling Java sources...
if not exist "bin" mkdir bin
dir /s /b src\main\java\*.java > sources.txt
javac -d bin @sources.txt
del sources.txt
echo Compilation successful! Classes stored in bin/
