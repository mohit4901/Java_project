if (!(Test-Path -Path "bin")) {
    Write-Host "Compiling first..." -ForegroundColor Yellow
    & .\build.ps1
}

$javaArgs = @("-cp", "bin", "com.timetabler.Main") + $args
& java $javaArgs
