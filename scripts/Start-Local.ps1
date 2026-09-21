param([int]$ApiPort = 8080, [int]$DatabasePort = 55440, [string]$PostgresBin = '', [switch]$CoreOnly)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$dataDir = Join-Path $projectRoot 'data'
New-Item -ItemType Directory -Force -Path $dataDir | Out-Null
if (-not $PostgresBin) {
    $installations = Get-ChildItem 'C:/Program Files/PostgreSQL' -Directory -ErrorAction SilentlyContinue | Sort-Object Name -Descending
    if (-not $installations) { throw 'Instala PostgreSQL 17 o utiliza docker compose. Consulta README.md.' }
    $PostgresBin = Join-Path $installations[0].FullName 'bin'
}
if (-not (Test-Path (Join-Path $PostgresBin 'pg_ctl.exe'))) { throw 'No se encontró pg_ctl.exe.' }
$configPath = Join-Path $dataDir 'local-config.json'
if (-not (Test-Path $configPath)) {
    $config = @{
        databasePassword = [guid]::NewGuid().ToString('N')
        instructorEmail = 'profesor@deploylab.local'
        instructorPassword = 'Demo-' + [guid]::NewGuid().ToString('N')
        databasePort = $DatabasePort
        postgresBin = $PostgresBin
    }
    $config | ConvertTo-Json | Set-Content -LiteralPath $configPath -Encoding UTF8
}
$config = Get-Content -LiteralPath $configPath -Raw | ConvertFrom-Json
if ($config.databasePort -ne $DatabasePort) { throw 'La base local ya usa otro puerto. Reutiliza el puerto guardado en data/local-config.json.' }
$pgData = Join-Path $dataDir 'postgres'
$env:PGPASSWORD = $config.databasePassword
if (-not (Test-Path (Join-Path $pgData 'PG_VERSION'))) {
    $passwordFile = Join-Path $dataDir 'init-password.tmp'
    try {
        [IO.File]::WriteAllText($passwordFile, $config.databasePassword)
        & (Join-Path $PostgresBin 'initdb.exe') -D $pgData -U deploylab -A scram-sha-256 --encoding=UTF8 --locale=C --pwfile=$passwordFile
        if ($LASTEXITCODE -ne 0) { throw 'No se pudo inicializar PostgreSQL.' }
    } finally { if (Test-Path $passwordFile) { Remove-Item -LiteralPath $passwordFile } }
}
& (Join-Path $PostgresBin 'pg_ctl.exe') -D $pgData status *> $null
if ($LASTEXITCODE -ne 0) {
    $pgArguments = @('-D', ('"' + $pgData + '"'), '-l', ('"' + (Join-Path $dataDir 'postgres.log') + '"'), '-o', ('"-p ' + $DatabasePort + ' -h 127.0.0.1"'), 'start')
    $starter = Start-Process -FilePath (Join-Path $PostgresBin 'pg_ctl.exe') -ArgumentList $pgArguments -WindowStyle Hidden -PassThru
    if (-not $starter.WaitForExit(15000) -or $starter.ExitCode -ne 0) { throw 'No se pudo iniciar la base local; revisa data/postgres.log.' }
}
$exists = & (Join-Path $PostgresBin 'psql.exe') --no-password -h 127.0.0.1 -p $DatabasePort -U deploylab -d postgres -Atc "SELECT 1 FROM pg_database WHERE datname='deploylab'"
if ($LASTEXITCODE -ne 0) { throw 'No se pudo conectar a la base local.' }
if ($exists -ne '1') {
    & (Join-Path $PostgresBin 'createdb.exe') -h 127.0.0.1 -p $DatabasePort -U deploylab deploylab
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo crear la base deploylab.' }
}
$portInUse = Get-NetTCPConnection -LocalPort $ApiPort -State Listen -ErrorAction SilentlyContinue
if ($portInUse) { throw "El puerto $ApiPort ya está ocupado. Detén la instancia o elige -ApiPort." }
Push-Location $projectRoot
try {
    & .\mvnw.cmd -B -q package '-DskipTests'
    if ($LASTEXITCODE -ne 0) { throw 'Falló la compilación.' }
    $env:DB_URL = "jdbc:postgresql://127.0.0.1:$DatabasePort/deploylab"
    $env:DB_USER = 'deploylab'
    $env:DB_PASSWORD = $config.databasePassword
    $env:INSTRUCTOR_EMAIL = $config.instructorEmail
    $env:INSTRUCTOR_PASSWORD = $config.instructorPassword
    $env:PORT = "$ApiPort"
    $env:MAIL_ENABLED = 'false'
    $env:EXTRAS_ENABLED = if ($CoreOnly) { 'false' } else { 'true' }
    $jarPath = Join-Path $projectRoot 'target/deploylab-backend-1.0.0.jar'
    $javaPath = (Get-Command java.exe -ErrorAction Stop).Source
    $process = Start-Process -FilePath $javaPath -ArgumentList @('-jar', ('"' + $jarPath + '"'), '--server.address=127.0.0.1') -WorkingDirectory $projectRoot -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $dataDir 'api.log') -RedirectStandardError (Join-Path $dataDir 'api-error.log')
    $process.Id | Set-Content -LiteralPath (Join-Path $dataDir 'api.pid')
    $ready = $false
    for ($i=0; $i -lt 60; $i++) {
        try {
            $health = Invoke-RestMethod -Uri "http://127.0.0.1:$ApiPort/actuator/health" -TimeoutSec 2
            if ($health.status -eq 'UP') { $ready=$true; break }
        } catch { Start-Sleep -Milliseconds 500 }
        if ($process.HasExited) { break }
    }
    if (-not $ready) { throw 'La API no inició. Revisa data/api.log y data/api-error.log.' }
    Write-Host "Backend iniciado: http://localhost:$ApiPort/swagger-ui/index.html"
    Write-Host 'Credenciales de instructor: data/local-config.json (archivo local excluido de Git).'
    Write-Host "Prueba: ./scripts/Demo.ps1 -BaseUrl http://127.0.0.1:$ApiPort"
} finally {
    Pop-Location
    Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
}
