param([string]$BaseUrl='http://127.0.0.1:8080')
$ErrorActionPreference='Stop'
# Runs the core checks and reuses their authenticated, newly registered student.
. (Join-Path $PSScriptRoot 'Demo.ps1') -BaseUrl $BaseUrl
$configPath=Join-Path (Split-Path $PSScriptRoot -Parent) 'data/local-config.json'
if (-not (Test-Path $configPath)) {throw 'Ejecuta Start-Local.ps1 para preparar el instructor local.'}
$config=Get-Content -LiteralPath $configPath -Raw | ConvertFrom-Json
$teacherToken=(Api POST '/login' @{email=$config.instructorEmail;password=$config.instructorPassword}).token
Write-Host 'M1. Instructor crea taller y administra su publicación'
$newWorkshop=Api POST '/api/workshops' @{title='Demo de módulos';description='Taller de demostración';topic='Backend';difficulty='BEGINNER';templates=@('API_URL')} $teacherToken
$edit=@{title='Demo de módulos';description='Taller de demostración';topic='Backend';difficulty='BEGINNER';published=$false}
Api PATCH "/api/instructor/workshops/$($newWorkshop.id)" $edit $teacherToken | Out-Null
ExpectFailure {Api GET "/api/workshops/$($newWorkshop.id)" $null $token} 404
Api GET "/api/instructor/workshops/$($newWorkshop.id)" $null $teacherToken | Out-Null
$edit.published=$true
Api PATCH "/api/instructor/workshops/$($newWorkshop.id)" $edit $teacherToken | Out-Null
Write-Host 'M2. Carga y descarga autenticada de PDF local'
Add-Type -AssemblyName System.Net.Http
$client=New-Object System.Net.Http.HttpClient
$multipart=New-Object System.Net.Http.MultipartFormDataContent
try {
    $client.DefaultRequestHeaders.Authorization=New-Object System.Net.Http.Headers.AuthenticationHeaderValue('Bearer',$teacherToken)
    $pdfBytes=[Text.Encoding]::ASCII.GetBytes("%PDF-1.4`n1 0 obj<</Type/Catalog>>endobj`n%%EOF")
    $part=New-Object System.Net.Http.ByteArrayContent -ArgumentList (,$pdfBytes)
    $part.Headers.ContentType=New-Object System.Net.Http.Headers.MediaTypeHeaderValue('application/pdf')
    $multipart.Add($part,'file','demo.pdf')
    $response=$client.PostAsync("$BaseUrl/api/workshops/$($newWorkshop.id)/materials/local",$multipart).GetAwaiter().GetResult()
    if ([int]$response.StatusCode -ne 201) {throw "Carga PDF falló: $($response.StatusCode)"}
    $material=$response.Content.ReadAsStringAsync().GetAwaiter().GetResult() | ConvertFrom-Json
    $client.DefaultRequestHeaders.Authorization=New-Object System.Net.Http.Headers.AuthenticationHeaderValue('Bearer',$token)
    $download=$client.GetByteArrayAsync("$BaseUrl/api/workshops/$($newWorkshop.id)/materials/$($material.id)/content").GetAwaiter().GetResult()
    if ([Convert]::ToBase64String($download) -ne [Convert]::ToBase64String($pdfBytes)) {throw 'Los bytes del PDF no coinciden'}
} finally {$multipart.Dispose();$client.Dispose()}
Write-Host 'M3. Grupo, membresía por correo y asignación'
$demoGroup=Api POST '/api/groups' @{name='Grupo demo ampliado'} $teacherToken
Api POST "/api/groups/$($demoGroup.id)/members/by-email" @{email=$email} $teacherToken | Out-Null
$assignment=Api POST "/api/groups/$($demoGroup.id)/assignments" @{workshopId='10000000-0000-0000-0000-000000000001';dueAt=[DateTimeOffset]::Now.AddDays(7).ToString('o')} $teacherToken
$statistics=Api GET "/api/groups/$($demoGroup.id)/statistics" $null $teacherToken
if ($statistics[0].resolved -ne 0) {throw 'Las prácticas personales se filtraron al grupo'}
Api GET "/api/assignments/$($assignment.id)" $null $token | Out-Null
Write-Host 'M4. Entrega asignada, revisión y estadísticas'
$assignedAttempt=Api POST "/api/assignments/$($assignment.id)/attempts" @{scenarioId=$scenario.id} $token
Api POST "/intentos/$($assignedAttempt.id)/acciones" @{code='FIX_URL'} $token ([guid]::NewGuid().ToString()) | Out-Null
$evaluation=Api POST "/intentos/$($assignedAttempt.id)/finalizar" $null $token
if ($evaluation.score -ne 100) {throw 'Se esperaba una nota de 100'}
$review=Api GET "/api/assignments/$($assignment.id)/submissions/$($assignedAttempt.id)" $null $teacherToken
if ($review.evaluation.score -ne 100) {throw 'La revisión no coincide'}
$statistics=Api GET "/api/groups/$($demoGroup.id)/statistics" $null $teacherToken
if ($statistics[0].resolved -ne 1) {throw 'La estadística debe contar una entrega'}
Write-Host 'M5. Reporte CSV en segundo plano y notificaciones'
$report=Api POST "/api/groups/$($demoGroup.id)/reports" $null $teacherToken
for ($i=0;$i -lt 30;$i++) {
    $job=Api GET "/api/jobs/$($report.id)" $null $teacherToken
    if ($job.state -eq 'SUCCEEDED' -or $job.state -eq 'FAILED') {break}
    Start-Sleep -Milliseconds 250
}
if ($job.state -ne 'SUCCEEDED') {throw 'El reporte no terminó correctamente'}
$csv=Api GET "/api/jobs/$($report.id)/download" $null $teacherToken
if ($csv -notmatch 'RESOLVED') {throw 'El reporte no contiene la entrega'}
Api PATCH '/api/notifications/read-all' $null $token | Out-Null
$dashboard=Api GET '/api/dashboard' $null $token
if ($dashboard.unreadNotifications -ne 0) {throw 'Quedan avisos sin leer'}
Write-Host 'M6. Cancelación, archivado y retiro de material'
Api DELETE "/api/assignments/$($assignment.id)" $null $teacherToken | Out-Null
ExpectFailure {Api POST "/api/assignments/$($assignment.id)/attempts" @{scenarioId=$scenario.id} $token} 409
Api PATCH "/api/groups/$($demoGroup.id)" @{name='Grupo demo finalizado';archived=$true} $teacherToken | Out-Null
Api DELETE "/api/workshops/$($newWorkshop.id)/materials/$($material.id)" $null $teacherToken | Out-Null
ExpectFailure {Api GET "/api/workshops/$($newWorkshop.id)/materials/$($material.id)/content" $null $token} 404
Write-Host 'OK: módulos ampliados verificados por HTTP; datos ficticios conservados para revisión.'
