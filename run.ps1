param([string]$DataDir)

$ErrorActionPreference = 'Stop'
$projectRoot = [IO.Path]::GetFullPath($PSScriptRoot)
$dataDirPath = if ($DataDir) {
  [IO.Path]::GetFullPath($DataDir)
} elseif ($env:IAMONITOR_DATA_DIR) {
  [IO.Path]::GetFullPath($env:IAMONITOR_DATA_DIR)
} else {
  Join-Path $projectRoot 'data'
}

$targetRoot = [IO.Path]::GetFullPath((Join-Path $projectRoot 'target'))
$appExe = Join-Path $targetRoot 'package/IAMonitor/IAMonitor.exe'
$marker = Join-Path $targetRoot 'current-app.txt'
if (Test-Path -LiteralPath $marker) {
  $relativeExe = (Get-Content -LiteralPath $marker -Raw).Trim()
  $candidate = [IO.Path]::GetFullPath((Join-Path $targetRoot $relativeExe))
  if ($candidate.StartsWith($targetRoot + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase) -and
      (Test-Path -LiteralPath $candidate)) { $appExe = $candidate }
}
if (Test-Path -LiteralPath $appExe) {
  Start-Process -FilePath $appExe -WorkingDirectory $projectRoot -ArgumentList @('--data-dir', ('"' + $dataDirPath + '"')) -WindowStyle Normal
  return
}

$jar = Join-Path $PSScriptRoot 'target/iamonitor-desktop-1.0.1.jar'
$java = Get-Command java -ErrorAction SilentlyContinue
if ($java -and (Test-Path -LiteralPath $jar)) {
  & $java.Source -jar $jar --data-dir $dataDirPath
  return
}

throw 'O aplicativo Windows não foi encontrado. Gere-o com .\build.ps1 -AppImage ou extraia o pacote portátil em target\package\IAMonitor.'
