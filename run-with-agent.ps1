param(
    [Parameter(ValueFromRemainingArguments=$true)]
    [string[]]$JavaArgs
)

Write-Host "[INFO] Compiling tests..." -ForegroundColor Cyan
mvn test-compile -q

Write-Host "[INFO] Building classpath..." -ForegroundColor Cyan
mvn dependency:build-classpath -D"mdep.outputFile"="cp.txt" -q

if (-not (Test-Path "cp.txt")) {
    Write-Host "[ERROR] Failed to create cp.txt" -ForegroundColor Red
    exit 1
}

$CLASSPATH = (Get-Content "cp.txt" -Raw).Trim() + ";target\test-classes;target\classes"

Write-Host "[INFO] Running Java with agent..." -ForegroundColor Green
Write-Host "[INFO] All arguments: $JavaArgs" -ForegroundColor Gray

$fullArgs = @(
    "-agentlib:native-image-agent=config-output-dir=config",
    "-cp", "`"$CLASSPATH`""
)

foreach ($arg in $JavaArgs) {
    $fullArgs += $arg
}

Write-Host "Command: java $fullArgs" -ForegroundColor Yellow
java @fullArgs

$exitCode = $LASTEXITCODE
Remove-Item "cp.txt" -Force -ErrorAction SilentlyContinue
exit $exitCode