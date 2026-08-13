param(
    [string]$ConsoleAddr = "http://127.0.0.1:8849",
    [string]$Username = "nacos",
    [string]$Password = "nacos",
    [string]$NamespaceId = "dev",
    [string]$Group = "WU_CLOUD",
    [string]$DataId = "wu-cloud-common.yaml",
    [string]$ContentFile = ""
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
if (-not $ContentFile) {
    $ContentFile = Join-Path $root "docs\nacos\wu-cloud-common.yaml"
}
if (-not (Test-Path $ContentFile)) {
    throw "Content file not found: $ContentFile"
}

$content = [System.IO.File]::ReadAllText($ContentFile)
$base = $ConsoleAddr.TrimEnd('/')

Write-Host "Login $base ..."
$login = Invoke-RestMethod -Uri "$base/v1/auth/login" -Method Post -Body @{
    username = $Username
    password = $Password
} -TimeoutSec 15
$token = $login.accessToken
if (-not $token) {
    throw "Nacos login failed (no accessToken)"
}

# Nacos 3.x console API on :8849; clients still use :8848 gRPC for the same config store
$form = @{
    dataId      = $DataId
    groupName   = $Group
    namespaceId = $NamespaceId
    content     = $content
    type        = "yaml"
    accessToken = $token
}

Write-Host "Publish dataId=$DataId group=$Group namespaceId=$NamespaceId"
$resp = Invoke-RestMethod -Uri "$base/v3/console/cs/config" -Method Post -Body $form -Headers @{
    accessToken = $token
} -TimeoutSec 20

Write-Host ($resp | ConvertTo-Json -Compress)
if ($null -eq $resp -or [int]$resp.code -ne 0) {
    exit 1
}
Write-Host "OK"
exit 0
