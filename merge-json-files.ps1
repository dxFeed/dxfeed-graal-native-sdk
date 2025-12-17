param(
  [string]$File1,
  [string]$File2,
  [string]$OutputFile
)

$data1 = Get-Content $File1 -Raw | ConvertFrom-Json
$data2 = Get-Content $File2 -Raw | ConvertFrom-Json

$result = @{ }

function Process-Object($obj)
{
  $name = $obj.name

  if (-not $result.ContainsKey($name))
  {
    $result[$name] = $obj | ConvertTo-Json -Depth 10 | ConvertFrom-Json
  }
  else
  {
    $existing = $result[$name]

    $obj.PSObject.Properties | Where-Object {
      $_.Name -ne 'name' -and $_.Name -ne 'methods'
    } | ForEach-Object {
      $propName = $_.Name
      $propValue = $_.Value

      if (-not $existing.PSObject.Properties[$propName]) {
        Add-Member -InputObject $existing -NotePropertyName $propName -NotePropertyValue $propValue
      } else {
        $existing.$propName = $propValue
      }
    }

    if ($obj.methods)
    {
      if (-not $existing.methods) {
        Add-Member -InputObject $existing -NotePropertyName "methods" -NotePropertyValue @()
      }

      $allMethods = @()
      if ($existing.methods) {
        $allMethods += $existing.methods
      }
      $allMethods += $obj.methods

      $uniqueMethods = @{ }
      foreach ($method in $allMethods)
      {
        $key = "$($method.name)-$($method.parameterTypes -join ',')"
        if (-not $uniqueMethods.ContainsKey($key))
        {
          $uniqueMethods[$key] = $method
        }
      }

      # Sort methods by name inside each object
      $sortedMethods = $uniqueMethods.Values | Sort-Object -Property name
      $existing.methods = @($sortedMethods)
    }
  }
}

$data1 | ForEach-Object { Process-Object $_ }
$data2 | ForEach-Object { Process-Object $_ }

# Sort all objects by name
$sortedResult = $result.Values | Sort-Object -Property name

$sortedResult | ConvertTo-Json -Depth 10 | Set-Content $OutputFile

Write-Host "The files merged and sorted successfully to: '$OutputFile'" -ForegroundColor Green