Write-Host "Compiling Java sources..." -ForegroundColor Cyan
if (!(Test-Path -Path "bin")) {
    New-Item -ItemType Directory -Path "bin" | Out-Null
}
$sources = (Get-ChildItem -Recurse -Path "src/main/java" -Filter "*.java").FullName
& javac -d bin $sources
if ($LASTEXITCODE -eq 0) {
    Write-Host "Compilation successful! Classes output to bin/" -ForegroundColor Green
} else {
    Write-Host "Compilation failed. Please ensure JDK is installed." -ForegroundColor Red
}
