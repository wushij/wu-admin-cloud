<#
 .SYNOPSIS
    Init wu-admin-cloud MySQL (shared DB mode)
 .DESCRIPTION
    Import sql/01-08 into wu-admin by default, or admin_platform.sql with -UseBaseline.
 .EXAMPLE
    .\scripts\init-databases.ps1
    .\scripts\init-databases.ps1 -UseBaseline
    .\scripts\init-databases.ps1 -Password "your-password"
#>
param(
    [string]$Mysql = "mysql",
    [string]$HostName = "127.0.0.1",
    [int]$Port = 3306,
    [string]$User = "root",
    [string]$Password = "root",
    [string]$Database = "wu-admin",
    [switch]$UseBaseline = $false
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$sqlDir = Join-Path $root "sql"

if (-not (Test-Path $sqlDir)) {
    Write-Host "[ERROR] sql dir not found: $sqlDir" -ForegroundColor Red
    exit 1
}

function Get-MysqlArgs {
    return @(
        "--host=$HostName",
        "--port=$Port",
        "--user=$User",
        "--password=$Password",
        "--default-character-set=utf8mb4"
    )
}

function Invoke-MysqlFile {
    param([string]$FilePath)
    if (-not (Test-Path $FilePath)) {
        throw "SQL file not found: $FilePath"
    }
    Write-Host "  import $(Split-Path $FilePath -Leaf) ..." -ForegroundColor Green
    $mysqlArgs = Get-MysqlArgs
    Get-Content -Path $FilePath -Raw -Encoding UTF8 | & $Mysql @mysqlArgs
    if ($LASTEXITCODE -ne 0) {
        throw "import failed: $FilePath (exit=$LASTEXITCODE)"
    }
}

Write-Host "==== init database ($Database @ ${HostName}:$Port) ====" -ForegroundColor Cyan
if ($UseBaseline) {
    Write-Host "mode: baseline admin_platform.sql"
} else {
    Write-Host "mode: scripts sql/01-08"
}

$createSql = "CREATE DATABASE IF NOT EXISTS ``$Database`` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
$mysqlArgs = Get-MysqlArgs
& $Mysql @mysqlArgs -e $createSql
if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] cannot connect MySQL or create database" -ForegroundColor Red
    exit 1
}

try {
    if ($UseBaseline) {
        Invoke-MysqlFile (Join-Path $sqlDir "admin_platform.sql")
    } else {
        $scripts = @(
            "01_wu_system.sql",
            "02_wu_file.sql",
            "03_wu_ticket.sql",
            "04_wu_ai.sql",
            "05_wu_message.sql",
            "06_wu_infra.sql",
            "07_wu_pay.sql",
            "08_wu_order.sql"
        )
        foreach ($name in $scripts) {
            Invoke-MysqlFile (Join-Path $sqlDir $name)
        }
    }
} catch {
    Write-Host "[ERROR] $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "==== database init done ====" -ForegroundColor Cyan
Write-Host "database: $Database (shared-db mode)"
Write-Host "next: .\scripts\start-services.ps1 or .vscode/launch.json"
exit 0