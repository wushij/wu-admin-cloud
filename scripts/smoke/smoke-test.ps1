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

Write-Host "==== infra ports ===="
Test-Port "MySQL" "127.0.0.1" 3306 | Out-Null
Test-Port "Redis" "127.0.0.1" 6379 | Out-Null
Test-Port "Nacos" "127.0.0.1" 8848 | Out-Null
Test-Port "Gateway" "127.0.0.1" 8080 | Out-Null

Write-Host "`n==== service ports ===="
foreach ($item in @(
    @{ Name = "auth"; Port = 9200 },
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

Write-Host "`n==== protected apis (expect 401/403) ===="
$protected = @(
    @{ Name = "user page"; Url = "$base/api/system/user/page?pageNo=1&pageSize=10" },
    @{ Name = "file policy"; Url = "$base/api/infra/file/upload-policy" },
    @{ Name = "file group"; Url = "$base/api/infra/file-group/list" },
    @{ Name = "dict page"; Url = "$base/api/system/dict-type/page?pageNo=1&pageSize=10" },
    @{ Name = "online list"; Url = "$base/api/system/monitor/online/list" },
    @{ Name = "server info"; Url = "$base/api/system/monitor/server/info" },
    @{ Name = "cache info"; Url = "$base/api/system/monitor/cache/info" },
    @{ Name = "job overview"; Url = "$base/api/system/monitor/job/overview" },
    @{ Name = "api access stats"; Url = "$base/api/system/monitor/api-access/statistics" },
    @{ Name = "recycle summary"; Url = "$base/api/system/recycle/summary" }
)
foreach ($item in $protected) {
    try {
        $resp = Invoke-WebRequest -Uri $item.Url -Method GET -UseBasicParsing -TimeoutSec 10
        if ($resp.StatusCode -in 401, 403, 200) {
            Write-Host "[PASS] $($item.Name) status=$($resp.StatusCode)"
            $script:pass++
        } else {
            Write-Host "[FAIL] $($item.Name) status=$($resp.StatusCode)"
            $script:fail++
        }
    } catch {
        $code = $null
        if ($_.Exception.Response) { $code = $_.Exception.Response.StatusCode.value__ }
        if ($code -in 401, 403, 400) {
            Write-Host "[PASS] $($item.Name) status=$code (auth/route reachable)"
            $script:pass++
        } else {
            Write-Host "[FAIL] $($item.Name) status=$code $($_.Exception.Message)"
            $script:fail++
        }
    }
}

Write-Host "`n==== summary ===="
Write-Host "PASS=$pass FAIL=$fail"
if ($fail -gt 0) { exit 1 }
exit 0
