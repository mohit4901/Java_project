@echo off
if not exist "bin" (
    call build.bat
)
java -cp bin com.timetabler.Main %*
