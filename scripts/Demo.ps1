param([string]$BaseUrl='http://127.0.0.1:8080')
$ErrorActionPreference='Stop'
function Api([string]$Method,[string]$Path,$Body=$null,[string]$Token='',[string]$Key='') {
    $headers=@{}
    if ($Token) {$headers.Authorization="Bearer $Token"}
    if ($Key) {$headers['Idempotency-Key']=$Key}
    $requestArgs=@{Uri="$BaseUrl$Path";Method=$Method;Headers=$headers;TimeoutSec=15}
    if ($null -ne $Body) {$requestArgs.Body=($Body|ConvertTo-Json -Depth 8);$requestArgs.ContentType='application/json; charset=utf-8'}
    Invoke-RestMethod @requestArgs
}
function ExpectFailure([scriptblock]$Action,[int]$Expected) {
    try { & $Action | Out-Null; throw "Se esperaba HTTP $Expected, pero la operación fue aceptada." }
    catch {
        if ($null -eq $_.Exception.Response -or [int]$_.Exception.Response.StatusCode -ne $Expected) {throw}
        Write-Host "OK: rechazo esperado HTTP $Expected"
    }
}
Write-Host '1. Registro y autenticación'
ExpectFailure { Api GET '/talleres' } 401
$email="demo-$([guid]::NewGuid().ToString('N'))@example.test"
$password='StudentDemo123!'
Api POST '/auth/register' @{name='Estudiante demo';email=$email;password=$password} | Out-Null
$token=(Api POST '/login' @{email=$email;password=$password}).token
ExpectFailure { Api POST '/login' @{email=$email;password='Incorrecta123!'} } 401
Write-Host '2. Catálogo y detalle del taller'
$catalog=Api GET '/talleres' $null $token
if ($catalog.Count -lt 1) {throw 'Catálogo vacío'}
$workshop=Api GET '/talleres/10000000-0000-0000-0000-000000000001' $null $token
$scenario=$workshop.scenarios | Where-Object {$_.title -eq 'API mal configurada'} | Select-Object -First 1
Write-Host '3. Inicio del intento; rechazar una entrega sin acciones'
$attempt=Api POST "/escenarios/$($scenario.id)/intentos" $null $token
ExpectFailure { Api POST "/intentos/$($attempt.id)/finalizar" $null $token } 409
ExpectFailure { Api POST "/intentos/$($attempt.id)/acciones" @{code='FIX_ROLE'} $token ([guid]::NewGuid().ToString()) } 400
Write-Host '4. Acción incorrecta: evento guardado e incidente abierto'
$wrong=Api POST "/intentos/$($attempt.id)/acciones" @{code='RESTART'} $token ([guid]::NewGuid().ToString())
if ($wrong.result_state -ne 'IN_PROGRESS') {throw 'La acción incorrecta resolvió el intento'}
$key=[guid]::NewGuid().ToString()
$success=Api POST "/intentos/$($attempt.id)/acciones" @{code='FIX_URL'} $token $key
if ($success.score -ne 0 -or $success.result_state -ne 'RESOLVED') {throw 'La evaluación debe esperar a finalizar'}
Write-Host '5. Incidente resuelto; repetir petición devuelve el mismo evento'
$replay=Api POST "/intentos/$($attempt.id)/acciones" @{code='FIX_URL'} $token $key
if ($replay.id -ne $success.id) {throw 'Se duplicó el evento'}
Write-Host '6. Finalizar: guardar nota e historial'
$final=Api POST "/intentos/$($attempt.id)/finalizar" $null $token
if (-not $final.finalized -or $final.score -ne 90 -or $final.events.Count -ne 2) {throw 'Evaluación incorrecta'}
$repeat=Api POST "/intentos/$($attempt.id)/finalizar" $null $token
if ($repeat.evaluation.evaluated_at -ne $final.evaluation.evaluated_at) {throw 'Se recalculó la evaluación'}
ExpectFailure { Api POST "/intentos/$($attempt.id)/acciones" @{code='RESTART'} $token ([guid]::NewGuid().ToString()) } 409
Write-Host '7. Entregar un intento sin resolver produce cero puntos'
$failedAttempt=Api POST "/escenarios/$($scenario.id)/intentos" $null $token
Api POST "/intentos/$($failedAttempt.id)/acciones" @{code='RESTART'} $token ([guid]::NewGuid().ToString()) | Out-Null
$failed=Api POST "/intentos/$($failedAttempt.id)/finalizar" $null $token
if ($failed.score -ne 0 -or $failed.evaluation.solved -or -not $failed.finalized) {throw 'El intento sin resolver debe cerrarse con cero puntos'}
Write-Host 'DEMOSTRACIÓN EXITOSA: siete endpoints, nota 90/100, intento fallido 0/100, historial e idempotencia.'
