<#
 .SYNOPSIS
    wu-admin-cloud one-click start (PowerShell / Windows)
 .DESCRIPTION
    Start 11 JVM apps: gateway + auth + BFF(wu-admin) + 8 domain services.
    Prerequisite: MySQL/Redis/Nacos ready; DB initialized via scripts/init-databases.ps1
 .PARAMETER Mvn
    Maven command, default mvn
 .PARAMETER SkipBuild
    Skip build, default false
 .PARAMETER StartGateway
    Start gateway, default true
 .EXAMPLE
    .\start-services.ps1
    .\start-services.ps1 -SkipBuild
#>
param(
    [string]$Mvn = "mvn",
    [switch]$SkipBuild = $false,
    [switch]$StartGateway = $true
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$fileUploadDir = Join-Path $root "wu-cloud-service\wu-service-file\data\file-upload"

function Start-ServiceWindow($name, $modulePath, $extraEnv = @{}) {
    $pomDir = Join-Path $root $modulePath
    $envPrefix = ""
    foreach ($key in $extraEnv.Keys) {
        $envPrefix += "`$env:$key = '$($extraEnv[$key])'; "
    }
    $cmd = "$envPrefix$Mvn spring-boot:run -f `"$pomDir\pom.xml`" -DskipTests"
    Write-Host "  start $name ..." -ForegroundColor Green
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd `"$pomDir`"; $cmd"
    Start-Sleep -Seconds 3
}

if (-not $SkipBuild) {
    Write-Host "==== mvn clean install (skipTests) ====" -ForegroundColor Cyan
    Push-Location $root
    & $Mvn clean install -DskipTests -q
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERROR] build failed" -ForegroundColor Red
        Pop-Location
        exit 1
    }
    Pop-Location
    Write-Host "  build ok`n" -ForegroundColor Green
}

if (-not (Test-Path $fileUploadDir)) {
    New-Item -ItemType Directory -Path $fileUploadDir -Force | Out-Null
}

$services = @(
    @{ Name = "auth"; Path = "wu-cloud-auth" },
    @{ Name = "system"; Path = "wu-cloud-service\wu-service-system" },
    @{ Name = "admin-bff"; Path = "wu-admin" },
    @{ Name = "infra"; Path = "wu-cloud-service\wu-service-infra" },
    @{ Name = "file"; Path = "wu-cloud-service\wu-service-file"; Env = @{ FILE_LOCAL_BASE_DIR = $fileUploadDir } },
    @{ Name = "ticket"; Path = "wu-cloud-service\wu-service-ticket" },
    @{ Name = "ai"; Path = "wu-cloud-service\wu-service-ai" },
    @{ Name = "message"; Path = "wu-cloud-service\wu-service-message" },
    @{ Name = "pay"; Path = "wu-cloud-service\wu-service-pay" },
    @{ Name = "order"; Path = "wu-cloud-service\wu-service-order" }
)

Write-Host "==== start business services ====" -ForegroundColor Cyan
foreach ($s in $services) {
    $envMap = @{}
    if ($s.ContainsKey("Env") -and $null -ne $s.Env) { $envMap = $s.Env }
    Start-ServiceWindow $s.Name $s.Path $envMap
}

if ($StartGateway) {
    Write-Host "`n==== start gateway ====" -ForegroundColor Cyan
    Start-ServiceWindow "gateway" "wu-cloud-gateway"
}

Write-Host "`n==== all service windows launched (includes BFF wu-admin :9100) ====" -ForegroundColor Cyan
Write-Host "gateway: 8080"
Write-Host "bff:     9100"
Write-Host "nacos:   http://127.0.0.1:8848/nacos"
Write-Host "init db: .\scripts\init-databases.ps1"
Write-Host "smoke:   .\scripts\smoke\smoke-test.ps1"