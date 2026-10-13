param([switch]$AppImage)

$ErrorActionPreference = 'Stop'
$projectRoot = [IO.Path]::GetFullPath($PSScriptRoot)
$targetRoot = [IO.Path]::GetFullPath((Join-Path $projectRoot 'target'))
$mavenCommand = Get-Command mvn -ErrorAction SilentlyContinue
if ($mavenCommand) {
  $maven = $mavenCommand.Source
} else {
  $maven = Join-Path $targetRoot 'toolchain/apache-maven-3.9.11/bin/mvn.cmd'
  if (-not (Test-Path -LiteralPath $maven)) { throw 'Maven 3 não encontrado. Instale-o ou mantenha o toolchain em target/toolchain.' }
}
if (-not $env:JAVA_HOME) {
  $jdk = Get-ChildItem -LiteralPath (Join-Path $targetRoot 'toolchain') -Directory -Filter 'jdk-*' -ErrorAction SilentlyContinue | Select-Object -First 1
  if ($jdk) { $env:JAVA_HOME = $jdk.FullName }
}
foreach ($name in @('iamonitor-desktop-1.0.2.jar', 'iamonitor-desktop-1.0.2-shaded.jar', 'original-iamonitor-desktop-1.0.2.jar')) {
  $jar = Join-Path $targetRoot $name
  if (Test-Path -LiteralPath $jar) { Remove-Item -LiteralPath $jar -Force }
}
foreach ($name in @('classes', 'test-classes')) {
  $generated = [IO.Path]::GetFullPath((Join-Path $targetRoot $name))
  if (-not $generated.StartsWith($targetRoot + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
    throw "Caminho fora de target: $generated"
  }
  if (Test-Path -LiteralPath $generated) { Remove-Item -LiteralPath $generated -Recurse -Force }
}
& $maven -f (Join-Path $projectRoot 'pom.xml') package
if ($LASTEXITCODE -ne 0) { throw 'A compilação Maven falhou.' }

if ($AppImage) {
  $inputRoot = [IO.Path]::GetFullPath((Join-Path $targetRoot 'jpackage-input'))
  $packageRoot = [IO.Path]::GetFullPath((Join-Path $targetRoot 'package'))
  $imageRoot = [IO.Path]::GetFullPath((Join-Path $packageRoot 'IAMonitor'))
  $activeExe = [IO.Path]::GetFullPath((Join-Path $imageRoot 'IAMonitor.exe'))
  $imageInUse = @(Get-Process IAMonitor -ErrorAction SilentlyContinue |
    Where-Object { $_.Path -and $_.Path.Equals($activeExe, [StringComparison]::OrdinalIgnoreCase) }).Count -gt 0
  if ($imageInUse) {
    $packageRoot = [IO.Path]::GetFullPath((Join-Path $targetRoot ('package-' + (Get-Date -Format 'yyyyMMdd-HHmmss'))))
    $imageRoot = [IO.Path]::GetFullPath((Join-Path $packageRoot 'IAMonitor'))
    Write-Host "Pacote anterior em uso; criando nova versão em $imageRoot"
  }
  foreach ($candidate in @($inputRoot, $imageRoot)) {
    if (-not $candidate.StartsWith($targetRoot + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
      throw "Caminho fora de target: $candidate"
    }
    if (Test-Path -LiteralPath $candidate) {
      try { Remove-Item -LiteralPath $candidate -Recurse -Force -ErrorAction Stop }
      catch {
        if ($candidate -ne $imageRoot) { throw }
        $packageRoot = [IO.Path]::GetFullPath((Join-Path $targetRoot ('package-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + $PID)))
        $imageRoot = [IO.Path]::GetFullPath((Join-Path $packageRoot 'IAMonitor'))
        Write-Host "Pacote anterior bloqueado; criando nova versão em $imageRoot"
      }
    }
  }
  New-Item -ItemType Directory -Force -Path $inputRoot,$packageRoot | Out-Null
  Copy-Item -LiteralPath (Join-Path $targetRoot 'iamonitor-desktop-1.0.2.jar') -Destination $inputRoot
  $jpackage = if ($env:JAVA_HOME -and (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin/jpackage.exe'))) {
    Join-Path $env:JAVA_HOME 'bin/jpackage.exe'
  } else {
    (Get-Command jpackage -ErrorAction Stop).Source
  }
  & $jpackage --type app-image --name IAMonitor --app-version 1.0.2 --input $inputRoot --main-jar 'iamonitor-desktop-1.0.2.jar' --dest $packageRoot
  if ($LASTEXITCODE -ne 0) { throw 'O empacotamento da aplicação falhou.' }
  $relativeExe = [IO.Path]::GetRelativePath($targetRoot, (Join-Path $imageRoot 'IAMonitor.exe'))
  [IO.File]::WriteAllText((Join-Path $targetRoot 'current-app.txt'), $relativeExe, [Text.UTF8Encoding]::new($false))
  Write-Host "Aplicativo: $(Join-Path $imageRoot 'IAMonitor.exe')"
}
