function Invoke-DownloadWithProgress
{
  param(
    [Parameter(Mandatory = $true)][string]$Uri,
    [Parameter(Mandatory = $true)][string]$OutFile
  )

  $request = [System.Net.HttpWebRequest]::Create($Uri)
  $response = $request.GetResponse()
  $totalBytes = $response.ContentLength
  $responseStream = $response.GetResponseStream()
  $fileStream = [System.IO.File]::Create($OutFile)

  $buffer = New-Object byte[] 1MB
  $totalRead = 0L
  $lastPercent = -5

  try
  {
    while (($read = $responseStream.Read($buffer, 0, $buffer.Length)) -gt 0)
    {
      $fileStream.Write($buffer, 0, $read)
      $totalRead += $read

      if ($totalBytes -gt 0)
      {
        $percent = [math]::Floor(($totalRead / $totalBytes) * 100)
        if ($percent -ge $lastPercent + 5)
        {
          Write-Host ("Downloaded {0:N0} MB / {1:N0} MB ({2}%)" -f ($totalRead / 1MB), ($totalBytes / 1MB), $percent)
          $lastPercent = $percent
        }
      }
      else
      {
        Write-Host ("Downloaded {0:N0} MB" -f ($totalRead / 1MB))
      }
    }
  }
  finally
  {
    $fileStream.Close()
    $responseStream.Close()
    $response.Close()
  }
}

function Install-Maven
{
  param (
    [Parameter(Mandatory = $true)]
    [string]$Version,
    [Parameter(Mandatory = $true)]
    [string]$InstallPath
  )

  Write-Host "Starting Maven installation with parameters:"
  Write-Host "Version: $Version"
  Write-Host "Install path: $InstallPath"

  # $BaseUrl = "https://dlcdn.apache.org/maven/"
  $BaseUrl = "https://archive.apache.org/dist/maven"
  $DownloadUrl = "$BaseUrl/maven-$($Version.Substring(0, 1) )/$Version/binaries/apache-maven-$Version-bin.tar.gz"

  Write-Host "Download URL: $DownloadUrl"

  if (-not (Test-Path -Path $InstallPath))
  {
    New-Item -ItemType Directory -Path $InstallPath | Out-Null
  }

  Invoke-DownloadWithProgress -Uri $DownloadUrl -OutFile "$($InstallPath)\maven.tar.gz"
  # Invoke-WebRequest -Uri $DownloadUrl -OutFile "$( $InstallPath )\maven.tar.gz"
  tar -xzf "$( $InstallPath )\maven.tar.gz" -C $InstallPath --strip-components=1
  Remove-Item "$( $InstallPath )\maven.tar.gz"
}

function Install-GraalVM
{
  param (
    [Parameter(Mandatory = $true)][string]$Version,
    [Parameter(Mandatory = $true)][string]$Platform,
    [Parameter(Mandatory = $true)][string]$InstallPath
  )

  $ErrorActionPreference = 'Stop'
  $BaseUrl = "https://github.com/graalvm/graalvm-ce-builds/releases/download/"

  $PlatformOS, $PlatformArch = $Platform -split '-', 2

  $FileExtension = "tar.gz"
  $VersionTag = ""
  $DistributionTag = ""
  $OSTag = ""
  $ArchTag = ""
  $Suffix = ""

  if ($Version -match "^java")
  {
    if ($Version -match "(\d+\.\d+\.\d+)$")
    {
      $ReleaseTag = $Matches[1]
      $VersionTag = "vm-$ReleaseTag"
    }
    else
    {
      Write-Error "Cannot extract release tag from version string"
    }

    if ($Version -match "java(\d+)")
    {
      $JavaVersion = $Matches[1]
      $DistributionTag = "graalvm-ce-java$JavaVersion"
    }

    switch ( $PlatformOS.ToLower())
    {
      "linux" {
        $OSTag = "-linux"
      }
      "macos" {
        $OSTag = "-darwin"
      }
      "osx"   {
        $OSTag = "-darwin"
      }
      "win"   {
        $OSTag = "-windows"; $FileExtension = "zip"
      }
      default {
        throw "Unknown OS tag '$PlatformOS'"
      }
    }

    switch ( $PlatformArch.ToLower())
    {
      "x64"     {
        $ArchTag = "-amd64"
      }
      "amd64"   {
        $ArchTag = "-amd64"
      }
      "arm64"   {
        $ArchTag = "-aarch64"
      }
      "aarch64" {
        $ArchTag = "-aarch64"
      }
      default   {
        throw "Unknown arch '$PlatformArch'"
      }
    }

    $Suffix = "-$ReleaseTag.$FileExtension"
  }
  elseif ($Version -match "^jdk-")
  {
    $VersionTag = $Version
    $DistributionTag = "graalvm-community-$Version"

    switch ( $PlatformOS.ToLower())
    {
      "linux" {
        $OSTag = "_linux"
      }
      "osx"   {
        $OSTag = "_macos"
      }
      "win"   {
        $OSTag = "_windows"; $FileExtension = "zip"
      }
      default {
        throw "Unknown OS tag '$PlatformOS'"
      }
    }

    switch ( $PlatformArch.ToLower())
    {
      "x64"     {
        $ArchTag = "-x64"
      }
      "amd64"   {
        $ArchTag = "-x64"
      }
      "arm64"   {
        $ArchTag = "-aarch64"
      }
      "aarch64" {
        $ArchTag = "-aarch64"
      }
      default   {
        throw "Unknown arch '$PlatformArch'"
      }
    }

    $Suffix = "_bin.$FileExtension"
  }
  else
  {
    Write-Error "Invalid GraalVM version format: '$Version'. Allowed formats:
        java<java_version>-<version> (legacy format)
        jdk-<jdk_version>"
  }

  New-Item -ItemType Directory -Force -Path $InstallPath | Out-Null
  $DownloadUrl = "$BaseUrl/$VersionTag/$DistributionTag$OSTag$ArchTag$Suffix"

  Write-Host "Downloading from $DownloadUrl"
  $TempFile = "$([System.IO.Path]::GetTempFileName() ).$FileExtension"
  Write-Host "Temp file name: $TempFile"
  Invoke-DownloadWithProgress -Uri $DownloadUrl -OutFile $TempFile
  # Invoke-WebRequest -Uri $DownloadUrl -OutFile $TempFile -UseBasicParsing

  if ($FileExtension -eq "zip")
  {
    Expand-Archive -Path $TempFile -DestinationPath $InstallPath -Force
    # --- strip-components=1 for zip ---
    $items = Get-ChildItem -LiteralPath $InstallPath
    $dirs  = $items | Where-Object { $_.PSIsContainer }
    $hasBin = Test-Path -LiteralPath (Join-Path $InstallPath "bin")

    if (-not $hasBin -and $dirs.Count -eq 1)
    {
      $root = $dirs[0].FullName
      Write-Host "Fixing zip layout: moving '$root' contents to '$InstallPath'"
      Get-ChildItem -LiteralPath $root -Force | ForEach-Object {
        Move-Item -LiteralPath $_.FullName -Destination $InstallPath -Force
      }
      Remove-Item -LiteralPath $root -Force -Recurse
    }
  }
  else
  {
    # Requires tar (usually available in modern Windows or via WSL or Git bash)
    tar -xzf $TempFile -C $InstallPath --strip-components=1
  }

  Remove-Item $TempFile -Force

  $guCmd = Join-Path $InstallPath "bin\gu.cmd"
  $guExe = Join-Path $InstallPath "bin\gu.exe"

  if (Test-Path $guCmd) {
    & $guCmd install native-image
  } elseif (Test-Path $guExe) {
    & $guExe install native-image
  } else {
    Write-Host "gu not found in $InstallPath\bin - assuming native-image is bundled (GraalVM 23+), skipping"
  }
}


function Install-VSBuildTools
{
  param (
    [Parameter(Mandatory = $true)][string]$Version,
    [Parameter(Mandatory = $true)][string]$InstallPath
  )

  $ErrorActionPreference = 'Stop'

  if ([string]::IsNullOrWhiteSpace($Version) -or [string]::IsNullOrWhiteSpace($InstallPath))
  {
    Write-Host "Usage: Install-VSBuildTools -Version <version> -InstallPath <install_path>"
    return 1
  }

  $BaseUrl = "https://aka.ms/vs/"
  $InstallerName = "vs_buildtools.exe"
  $DownloadUrl = "$BaseUrl/$Version/release/$InstallerName"
  $InstallerPath = Join-Path -Path $PWD -ChildPath $InstallerName

  try
  {
    Write-Host "Downloading Visual Studio Build Tools from: $DownloadUrl"
    Invoke-WebRequest -Uri $DownloadUrl -OutFile $InstallerPath -UseBasicParsing -ErrorAction Stop

    Write-Host "Starting installer..."
    Start-Process -FilePath $InstallerPath `
            -ArgumentList @(
      "--quiet",
      "--wait",
      "--norestart",
      "--nocache",
      "--installPath", "`"$InstallPath`"",
      "--includeRecommended",
      "--add", "Microsoft.VisualStudio.Workload.VCTools",
      "--add", "Microsoft.VisualStudio.Component.VC.Tools.x86.x64",
      "--add", "Microsoft.VisualStudio.Component.Windows11SDK.22621"
    ) `
            -Wait -NoNewWindow

    Write-Host "Installation completed successfully."
  }
  finally
  {
    if (Test-Path $InstallerPath)
    {
      Remove-Item $InstallerPath -Force
      Write-Host "Cleaned up installer: $InstallerName"
    }
  }
}
