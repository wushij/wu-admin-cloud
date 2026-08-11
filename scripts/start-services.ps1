<#
 .SYNOPSIS
    wu-admin-cloud 微服务一键启动脚本（PowerShell / Windows）
 .DESCRIPTION
    按依赖顺序编译并启动 10 个微服务（gateway + auth + 9 业务服务，含 infra）。
    每个服务在独立 PowerShell 窗口执行 mvn spring-boot:run，便于观察独立日志。
    前置条件：MySQL/Redis/Nacos 已就绪，9 库已初始化（见 init-databases.ps1）。
 .PARAMETER Mvn
    Maven 命令，默认 mvn（若用 wrapper 可传 ./mvnw.cmd）
 .PARAMETER SkipBuild
    跳过编译（直接启动），默认 $false
 .PARAMETER StartGateway
    是否启动网关与认证服务，默认 $true
 .EXAMPLE
    .\start-services.ps1
    .\start-services.ps1 -Mvn "./mvnw.cmd" -SkipBuild
#>
param(
    [string]$Mvn = "mvn",
    [switch]$SkipBuild = $false,
    [switch]$StartGateway = $true
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot

function Start-ServiceWindow($name, $modulePath, $profileArg) {
    $pomDir = Join-Path $root $modulePath
    $cmd = "$Mvn spring-boot:run -f `"$pomDir\pom.xml`" $profileArg"
    Write-Host "  启动 $name ..." -ForegroundColor Green
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd `"$pomDir`"; $cmd"
    Start-Sleep -Seconds 3
}

# 0. 全量编译（含 wu-cloud-common / wu-cloud-api，确保契约改动生效）
if (-not $SkipBuild) {
    Write-Host "==== 全量编译（跳过测试）====" -ForegroundColor Cyan
    Push-Location $root
    & $Mvn clean install -DskipTests -q
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERROR] 编译失败，退出。" -ForegroundColor Red
        Pop-Location
        exit 1
    }
    Pop-Location
    Write-Host "  编译成功。`n" -ForegroundColor Green
}

# 启动清单（按依赖顺序：infra 无强依赖先起亦可；system/auth 需最先被 gateway 转发）
$services = @(
    @{ Name = "auth";   Path = "wu-cloud-auth" },
    @{ Name = "system"; Path = "wu-cloud-service\wu-service-system" },
    @{ Name = "infra";  Path = "wu-cloud-service\wu-service-infra" },
    @{ Name = "file";   Path = "wu-cloud-service\wu-service-file" },
    @{ Name = "ticket"; Path = "wu-cloud-service\wu-service-ticket" },
    @{ Name = "ai";     Path = "wu-cloud-service\wu-service-ai" },
    @{ Name = "message";Path = "wu-cloud-service\wu-service-message" },
    @{ Name = "pay";    Path = "wu-cloud-service\wu-service-pay" },
    @{ Name = "order";  Path = "wu-cloud-service\wu-service-order" }
)

Write-Host "==== 启动业务服务（独立窗口）====" -ForegroundColor Cyan
foreach ($s in $services) {
    Start-ServiceWindow $s.Name $s.Path ""
}

if ($StartGateway) {
    Write-Host "`n==== 启动网关 ====" -ForegroundColor Cyan
    Start-ServiceWindow "gateway" "wu-cloud-gateway" ""
}

Write-Host "`n==== 全部服务窗口已弹出 ====" -ForegroundColor Cyan
Write-Host "网关默认端口: 8080（见 wu-cloud-gateway 配置）"
Write-Host "Nacos 控制台: http://127.0.0.1:8848/nacos"
Write-Host "冒烟测试:    .\scripts\smoke\smoke-test.ps1"
