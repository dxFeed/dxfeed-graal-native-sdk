$ErrorActionPreference = 'Stop'
. C:\run-mvn-vs.ps1 -SetupOnly

Write-Host "JAVA_HOME=$env:JAVA_HOME"
dir $env:JAVA_HOME
dir (Join-Path $env:JAVA_HOME 'bin')

where.exe java
java -version
where.exe mvn
mvn -v
mvn clean package
