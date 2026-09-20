param([string]$BaseUrl='http://localhost:8080')
$ErrorActionPreference='Stop'
function Api([string]$Method,[string]$Path,$Body=$null,[string]$Token='',[string]$Key='') {
    $headers=@{}
    if ($Token) {$headers.Authorization="Bearer $Token"}
    if ($Key) {$headers['Idempotency-Key']=$Key}
    $args=@{Uri="$BaseUrl$Path";Method=$Method;Headers=$headers}
    if ($null -ne $Body) {$args.Body=($Body|ConvertTo-Json -Depth 8);$args.ContentType='application/json; charset=utf-8'}
    Invoke-RestMethod @args
}
function ExpectFailure([scriptblock]$Action,[int]$Expected) {
    try { & $Action | Out-Null; throw "Se esperaba HTTP $Expected, pero la operación fue aceptada." }
    catch {
        if ($null -eq $_.Exception.Response -or [int]$_.Exception.Response.StatusCode -ne $Expected) {throw}
        Write-Host "OK: rechazo esperado HTTP $Expected"
    }
}
Write-Host '1. Salud de la aplicación'
$health=Api GET '/actuator/health'
if ($health.status -ne 'UP') {throw 'Aplicación no disponible'}
ExpectFailure { Api GET '/api/workshops' } 401
$email="demo-$([guid]::NewGuid().ToString('N'))@example.test"
$password='StudentDemo123!'
$user=Api POST '/api/auth/register' @{name='Estudiante demo';email=$email;password=$password}
$session=Api POST '/api/auth/login' @{email=$email;password=$password}
$token=$session.token
Write-Host '2. Registro y login correctos'
ExpectFailure { Api POST '/api/auth/login' @{email=$email;password='Incorrecta123!'} } 401
ExpectFailure { Api POST '/api/groups' @{name='No autorizado'} $token } 403
$workshop=Api GET '/api/workshops/10000000-0000-0000-0000-000000000001' $null $token
$scenario=$workshop.scenarios | Where-Object {$_.title -eq 'API mal configurada'} | Select-Object -First 1
$attempt=Api POST '/api/attempts' @{scenarioId=$scenario.id} $token
Write-Host '3. Acción incorrecta aceptada como parte del aprendizaje'
$wrong=Api POST "/api/attempts/$($attempt.id)/actions" @{code='RESTART'} $token ([guid]::NewGuid().ToString())
if ($wrong.result_state -ne 'IN_PROGRESS') {throw 'La acción incorrecta resolvió el intento'}
Api POST "/api/attempts/$($attempt.id)/hint" $null $token | Out-Null
$key=[guid]::NewGuid().ToString()
$success=Api POST "/api/attempts/$($attempt.id)/actions" @{code='FIX_URL'} $token $key
if ($success.score -ne 85 -or $success.result_state -ne 'RESOLVED') {throw 'Evaluación inesperada'}
Write-Host '4. Incidente resuelto: 85 puntos (un error y una pista)'
$replay=Api POST "/api/attempts/$($attempt.id)/actions" @{code='FIX_URL'} $token $key
if ($replay.id -ne $success.id) {throw 'Se duplicó el evento'}
Write-Host '5. Reintento de red devuelve el mismo evento sin duplicar puntos'
ExpectFailure { Api POST "/api/attempts/$($attempt.id)/actions" @{code='RESTART'} $token ([guid]::NewGuid().ToString()) } 409
$report=Api POST '/api/reports' $null $token
for ($i=0;$i -lt 30;$i++) {
    $job=Api GET "/api/jobs/$($report.id)" $null $token
    if ($job.state -eq 'SUCCEEDED') {break}
    if ($job.state -eq 'FAILED') {throw 'Falló el reporte'}
    Start-Sleep -Milliseconds 500
}
if ($job.state -ne 'SUCCEEDED') {throw 'Tiempo de espera del reporte agotado'}
$csv=Api GET "/api/jobs/$($report.id)/download" $null $token
if ($csv -notmatch 'RESOLVED') {throw 'El reporte no contiene el intento'}
Write-Host '6. Reporte asincrónico generado y descargado'
Api POST '/api/auth/logout' $null $token | Out-Null
ExpectFailure { Api GET '/api/auth/me' $null $token } 401
Write-Host 'DEMOSTRACIÓN EXITOSA: flujo completo y rechazos verificados.'
