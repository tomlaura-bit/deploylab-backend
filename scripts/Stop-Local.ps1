$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$dataDir = Join-Path $projectRoot 'data'
$pidFile = Join-Path $dataDir 'api.pid'
if (Test-Path $pidFile) {
    $apiProcessId = [int](Get-Content -LiteralPath $pidFile)
    $process = Get-CimInstance Win32_Process -Filter "ProcessId = $apiProcessId" -ErrorAction SilentlyContinue
    $expectedJar = Join-Path $projectRoot 'target/deploylab-backend-1.0.0.jar'
    if ($process -and $process.CommandLine.Contains($expectedJar)) { Stop-Process -Id $apiProcessId }
    elseif ($process) { throw 'El PID pertenece a otro proceso. No se detuvo.' }
    Remove-Item -LiteralPath $pidFile
}
$configFile = Join-Path $dataDir 'local-config.json'
if (Test-Path $configFile) {
    $config = Get-Content -LiteralPath $configFile -Raw | ConvertFrom-Json
    & (Join-Path $config.postgresBin 'pg_ctl.exe') -D (Join-Path $dataDir 'postgres') -m fast stop
}
Write-Host 'Instancia local detenida. Los datos se conservan.'
