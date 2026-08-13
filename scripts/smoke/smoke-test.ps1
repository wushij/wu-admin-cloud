param(
    [string]$Gateway = "http://127.0.0.1:8080"
)

$ErrorActionPreference = "Continue"
$pass = 0
$fail = 0

function Test-Port {
    param([string]$Name, [string]$HostName, [int]$Port)
    try {
        $client = New-Object System.Net.Sockets.TcpClient
        $iar = $client.BeginConnect($HostName, $Port, $null, $null)
        $ok = $iar.AsyncWaitHandle.WaitOne(2000, $false)
        if ($ok -and $client.Connected) {
            $client.Close()
            Write-Host "[PASS] $Name ${HostName}:${Port}"
            $script:pass++
            return $true
        }
        Write-Host "[FAIL] $Name ${HostName}:${Port} unreachable"
        $script:fail++
        return $false
    } catch {
        Write-Host "[FAIL] $Name ${HostName}:${Port} $($_.Exception.Message)"
        $script:fail++
        return $false
    }
}

function Test-Http {
    param([string]$Name, [string]$Url, [int]$ExpectCode = 200)
    try {
        $resp = Invoke-WebRequest -Uri $Url -Method GET -UseBasicParsing -TimeoutSec 10
        if ($resp.StatusCode -eq $ExpectCode) {
            Write-Host "[PASS] $Name status=$($resp.StatusCode)"
            $script:pass++
            return $true
        }
        Write-Host "[FAIL] $Name status=$($resp.StatusCode) expected=$ExpectCode"
        $script:fail++
        return $false
    } catch {
        if ($_.Exception.Response -and $_.Exception.Response.StatusCode.value__ -eq $ExpectCode) {
            Write-Host "[PASS] $Name status=$ExpectCode"
            $script:pass++
            return $true
        }
        $code = $null
        if ($_.Exception.Response) { $code = $_.Exception.Response.StatusCode.value__ }
        Write-Host "[FAIL] $Name status=$code $($_.Exception.Message)"
        $script:fail++
        return $false
    }
}

function Test-HttpReachable {
    param(
        [string]$Name,
        [string]$Url,
        [int[]]$AcceptCodes = @(200, 401, 403)
    )
    try {
        $resp = Invoke-WebRequest -Uri $Url -Method GET -UseBasicParsing -TimeoutSec 10
        if ($resp.StatusCode -in $AcceptCodes) {
            Write-Host "[PASS] $Name status=$($resp.StatusCode)"
            $script:pass++
            return $true
        }
        Write-Host "[FAIL] $Name status=$($resp.StatusCode) accepted=$($AcceptCodes -join ',')"
        $script:fail++
        return $false
    } catch {
        $code = $null
        if ($_.Exception.Response) { $code = $_.Exception.Response.StatusCode.value__ }
        if ($code -in ($AcceptCodes + @(400))) {
            Write-Host "[PASS] $Name status=$code (reachable)"
            $script:pass++
            return $true
        }
        Write-Host "[FAIL] $Name status=$code $($_.Exception.Message)"
        $script:fail++
        return $false
    }
}

Write-Host "==== infra ports ===="
Test-Port "MySQL" "127.0.0.1" 3306 | Out-Null
Test-Port "Redis" "127.0.0.1" 6379 | Out-Null
Test-Port "Nacos API" "127.0.0.1" 8848 | Out-Null
Test-Port "Nacos Console" "127.0.0.1" 8849 | Out-Null
Test-Port "Gateway" "127.0.0.1" 8080 | Out-Null

Write-Host "`n==== service ports ===="
foreach ($item in @(
    @{ Name = "auth"; Port = 9200 },
    @{ Name = "wu-admin(BFF)"; Port = 9100 },
    @{ Name = "system"; Port = 9201 },
    @{ Name = "ticket"; Port = 9202 },
    @{ Name = "message"; Port = 9203 },
    @{ Name = "ai"; Port = 9204 },
    @{ Name = "pay"; Port = 9205 },
    @{ Name = "order"; Port = 9206 },
    @{ Name = "file"; Port = 9207 },
    @{ Name = "infra"; Port = 9300 }
)) {
    Test-Port $item.Name "127.0.0.1" $item.Port | Out-Null
}

$base = $Gateway.TrimEnd('/')
Write-Host "`n==== public auth apis ===="
Test-Http "auth config" "$base/api/auth/config" | Out-Null
Test-Http "auth captcha" "$base/api/auth/captcha" | Out-Null
Test-Http "auth slider" "$base/api/auth/slider-challenge" | Out-Null

Write-Host "`n==== BFF dashboard (via gateway) ===="
# 未登录期望 401/403；BFF 未启动常见 502/503 → FAIL
Test-HttpReachable "dashboard stats" "$base/api/admin/dashboard/stats" @(200, 401, 403) | Out-Null
Test-HttpReachable "dashboard legacy rewrite" "$base/api/system/dashboard/stats" @(200, 401, 403) | Out-Null

Write-Host "`n==== P1 canonical paths (expect 401/403) ===="
$canonical = @(
    @{ Name = "monitor server"; Url = "$base/api/monitor/server/info" },
    @{ Name = "monitor cache"; Url = "$base/api/monitor/cache/info" },
    @{ Name = "infra job"; Url = "$base/api/infra/job/overview" },
    @{ Name = "infra online"; Url = "$base/api/infra/online/list" },
    @{ Name = "infra access-log"; Url = "$base/api/infra/access-log/statistics" },
    @{ Name = "infra codegen"; Url = "$base/api/infra/codegen/page?pageNo=1&pageSize=10" },
    @{ Name = "infra config-log sms"; Url = "$base/api/infra/config-log/sms-logs/recent?limit=1" },
    @{ Name = "ai model"; Url = "$base/api/ai/model/page?pageNo=1&pageSize=10" },
    @{ Name = "dict type"; Url = "$base/api/system/dict/type/page?pageNo=1&pageSize=10" }
)
foreach ($item in $canonical) {
    Test-HttpReachable $item.Name $item.Url @(200, 401, 403) | Out-Null
}

Write-Host "`n==== P1.5 ticket canonical paths (expect 401/403) ===="
$ticketCanonical = @(
    @{ Name = "ticket page"; Url = "$base/api/ticket/page?pageNo=1&pageSize=10" },
    @{ Name = "approval page"; Url = "$base/api/approval/page?pageNo=1&pageSize=10" },
    @{ Name = "notice my-list"; Url = "$base/api/notice/my-list" },
    @{ Name = "legacy ticket rewrite"; Url = "$base/api/system/ticket/page?pageNo=1&pageSize=10" }
)
foreach ($item in $ticketCanonical) {
    Test-HttpReachable $item.Name $item.Url @(200, 401, 403) | Out-Null
}

Write-Host "`n==== protected apis (expect 401/403) ===="
$protected = @(
    @{ Name = "user page"; Url = "$base/api/system/user/page?pageNo=1&pageSize=10" },
    @{ Name = "file policy"; Url = "$base/api/infra/file/upload-policy" },
    @{ Name = "file group"; Url = "$base/api/infra/file-group/list" },
    @{ Name = "recycle summary"; Url = "$base/api/system/recycle/summary" },
    @{ Name = "auth profile"; Url = "$base/api/auth/profile" }
)
foreach ($item in $protected) {
    Test-HttpReachable $item.Name $item.Url @(200, 401, 403) | Out-Null
}

Write-Host "`n==== summary ===="
Write-Host "PASS=$pass FAIL=$fail"
if ($fail -gt 0) { exit 1 }
exit 0
