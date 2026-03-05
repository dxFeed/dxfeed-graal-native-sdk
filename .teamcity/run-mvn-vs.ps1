param(
  [switch]$SetupOnly,
  [Parameter(ValueFromRemainingArguments=$true)]
  [string[]]$MvnArgs
)

$ErrorActionPreference = 'Stop'

$vs = "C:\BuildTools\Common7\Tools\VsDevCmd.bat"
if (-not (Test-Path $vs)) { throw "VsDevCmd not found: $vs" }

$envDump = cmd /c "`"$vs`" -arch=amd64 && set"
foreach ($line in $envDump) {
  if ($line -match '^(?<k>[^=]+)=(?<v>.*)$') {
    [System.Environment]::SetEnvironmentVariable($Matches.k, $Matches.v, "Process")
  }
}

if ($SetupOnly) { return }

cmd /c "where cl"
cmd /c "where java"
cmd /c "where mvn"

& mvn @MvnArgs
exit $LASTEXITCODE
