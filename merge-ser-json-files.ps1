param(
  [string]$File1,
  [string]$File2,
  [string]$OutputFile
)

# Load JSON files
$data1 = Get-Content $File1 -Raw | ConvertFrom-Json
$data2 = Get-Content $File2 -Raw | ConvertFrom-Json

# Create result object with all three arrays
$result = @{
  types = @()
  lambdaCapturingTypes = @()
  proxies = @()
}

# Function to merge objects by name from two arrays
function Merge-Objects($array1, $array2) {
  $merged = @{}

  # Process first array
  foreach ($obj in $array1) {
    $name = $obj.name
    if (-not $merged.ContainsKey($name)) {
      $merged[$name] = $obj
    }
    # Note: If duplicate in same array, first one wins
  }

  # Process second array
  foreach ($obj in $array2) {
    $name = $obj.name
    if (-not $merged.ContainsKey($name)) {
      $merged[$name] = $obj
    }
    # Note: If duplicate across arrays, second one wins (overwrites)
  }

  # Return sorted by name
  return $merged.Values | Sort-Object -Property name
}

# Merge types array
if ($data1.types -and $data2.types) {
  $result.types = Merge-Objects $data1.types $data2.types
} elseif ($data1.types) {
  $result.types = $data1.types | Sort-Object -Property name
} elseif ($data2.types) {
  $result.types = $data2.types | Sort-Object -Property name
}

# Merge lambdaCapturingTypes array
if ($data1.lambdaCapturingTypes -and $data2.lambdaCapturingTypes) {
  $result.lambdaCapturingTypes = Merge-Objects $data1.lambdaCapturingTypes $data2.lambdaCapturingTypes
} elseif ($data1.lambdaCapturingTypes) {
  $result.lambdaCapturingTypes = $data1.lambdaCapturingTypes | Sort-Object -Property name
} elseif ($data2.lambdaCapturingTypes) {
  $result.lambdaCapturingTypes = $data2.lambdaCapturingTypes | Sort-Object -Property name
}

# Merge proxies array
if ($data1.proxies -and $data2.proxies) {
  $result.proxies = Merge-Objects $data1.proxies $data2.proxies
} elseif ($data1.proxies) {
  $result.proxies = $data1.proxies | Sort-Object -Property name
} elseif ($data2.proxies) {
  $result.proxies = $data2.proxies | Sort-Object -Property name
}

# Convert hashtable to PSCustomObject for proper JSON serialization
$outputObject = [PSCustomObject]@{
  types = $result.types
  lambdaCapturingTypes = $result.lambdaCapturingTypes
  proxies = $result.proxies
}

# Save result to file
$outputObject | ConvertTo-Json -Depth 10 | Set-Content $OutputFile

Write-Host "JSON files merged and sorted successfully to: '$OutputFile'" -ForegroundColor Green