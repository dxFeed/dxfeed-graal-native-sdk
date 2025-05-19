function Install-Maven {
  param (
    [Parameter(Mandatory=$true)]
    [string]$Version,
    [Parameter(Mandatory=$true)]
    [string]$InstallPath
  )

  Write-Host "Starting Maven installation with parameters:"
  Write-Host "Version: $Version"
  Write-Host "Install path: $InstallPath"

  $BaseUrl = "https://dlcdn.apache.org/maven/"
  $DownloadUrl = "$BaseUrl/maven-$($Version.Substring(0, 1))/$Version/binaries/apache-maven-$Version-bin.tar.gz"

  Write-Host "Download URL: $DownloadUrl"

  if (-not (Test-Path -Path $InstallPath)) {
    New-Item -ItemType Directory -Path $InstallPath | Out-Null
  }

  Invoke-WebRequest -Uri $DownloadUrl -OutFile "$($InstallPath)\maven.tar.gz"
  tar -xzf "$($InstallPath)\maven.tar.gz" -C $InstallPath --strip-components=1
  Remove-Item "$($InstallPath)\maven.tar.gz"
}

function Install-GraalVM {
  param (
    [Parameter(Mandatory=$true)]
    [string]$Version,
    [Parameter(Mandatory=$true)]
    [string]$Platform,
    [Parameter(Mandatory=$true)]
    [string]$InstallPath
  )

  Write-Host "Starting GraalVM installation with parameters:"
  Write-Host "Version: $Version"
  Write-Host "Platform: $Platform"
  Write-Host "Install path: $InstallPath"

  $BaseUrl = "https://github.com/graalvm/graalvm-ce-builds/releases/download/"
  $PlatformOS = $Platform -replace '-.*', ''
  $PlatformArch = $Platform -replace '.*-', ''
  $FileExtension = "tar.gz"
  $Suffix = ""

  if ($Version -match "^java") {
    $ReleaseTag = $Version -replace '.*-(\d+\.\d+\.\d+)', '$1'
    $VersionTag = "vm-$ReleaseTag"
    $DistributionTag = "graalvm-ce-java$($Version -replace '^java(\d+).*', '$1')"
    $Suffix = "-$ReleaseTag.$FileExtension"
  } elseif ($Version -match "^jdk-") {
    $VersionTag = $Version
    $DistributionTag = "graalvm-community-$Version"
    $Suffix = "_bin.$FileExtension"
  } else {
    throw "Invalid GraalVM version format: $Version"
  }

  $OSTag = switch ($PlatformOS) {
    "linux" { "-linux" }
    "macos" { "-darwin" }
    "osx"   { "-darwin" }
    "win"   { "-windows"; $FileExtension = "zip" }
    default { throw "Invalid OS: $PlatformOS" }
  }

  $ArchTag = switch ($PlatformArch) {
    "x64"   { "-amd64" }
    "amd64" { "-amd64" }
    "arm64" { "-aarch64" }
    "aarch64" { "-aarch64" }
    default { throw "Invalid architecture: $PlatformArch" }
  }

  $DownloadUrl = "$BaseUrl/$VersionTag/$DistributionTag$OSTag$ArchTag$Suffix"

  Write-Host "Download URL: $DownloadUrl"

  if (-not (Test-Path -Path $InstallPath)) {
    New-Item -ItemType Directory -Path $InstallPath | Out-Null
  }

  Invoke-WebRequest -Uri $DownloadUrl -OutFile "$($InstallPath)\graalvm.$FileExtension"
  if ($FileExtension -eq "zip") {
    Expand-Archive -LiteralPath "$($InstallPath)\graalvm.$FileExtension" -DestinationPath $InstallPath -Force
  } else {
    tar -xzf "$($InstallPath)\graalvm.$FileExtension" -C $InstallPath --strip-components=1
  }
  Remove-Item "$($InstallPath)\graalvm.$FileExtension"
}

function Install-VSBuildTools {
  param (
    [Parameter(Mandatory=$true)]
    [string]$Version,
    [Parameter(Mandatory=$true)]
    [string]$InstallPath
  )

  Write-Host "Starting VSBuildTools installation with parameters:"
  Write-Host "Version: $Version"
  Write-Host "Install path: $InstallPath"

  $BaseUrl = "https://aka.ms/vs/"
  $VSBuildTools = "vs_buildtools.exe"
  $DownloadUrl = "$BaseUrl/$Version/release/$VSBuildTools"

  Write-Host "Download URL: $DownloadUrl"

  Invoke-WebRequest -Uri $DownloadUrl -OutFile $VSBuildTools
  try {
    Start-Process -FilePath $VSBuildTools -ArgumentList @(
      '--quiet', '--wait', '--norestart', '--nocache',
      "--installPath $InstallPath",
      '--includeRecommended',
      '--add Microsoft.VisualStudio.Workload.VCTools',
      '--add Microsoft.VisualStudio.Component.VC.Tools.x86.x64',
      '--add Microsoft.VisualStudio.Component.Windows11SDK.22621'
    ) -Wait
  } finally {
    Remove-Item -Path $VSBuildTools -Force
  }
}