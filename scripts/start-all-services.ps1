# 启动全部后端微服务
# - 控制台输出：重定向到 logs/<service>.log（已在 .gitignore 中忽略）
# - 应用自身日志：logging.file.name 指向 logs/，同样落到仓库根 logs/ 目录
#   （各服务工作目录不同，必须用绝对路径 LOG_DIR，否则日志会散落到各模块目录）
$ErrorActionPreference = 'Continue'
$root = 'E:\wu-admin-cloud'
$logDir = Join-Path $root 'logs'
New-Item -ItemType Directory -Path $logDir -Force | Out-Null

# 应用内日志目录（绝对路径），通过环境变量传给各服务
$env:LOG_DIR = ($root -replace '\\', '/') + '/logs'

$services = @(
    @{ name = 'gateway';    dir = 'wu-cloud-gateway';                     port = 8080 },
    @{ name = 'auth';       dir = 'wu-cloud-auth';                        port = 9200 },
    @{ name = 'wu-admin';   dir = 'wu-admin';                             port = 9100 },
    @{ name = 'system';     dir = 'wu-cloud-service/wu-service-system';   port = 9201 },
    @{ name = 'ticket';     dir = 'wu-cloud-service/wu-service-ticket';   port = 9202 },
    @{ name = 'message';    dir = 'wu-cloud-service/wu-service-message';  port = 9203 },
    @{ name = 'ai';         dir = 'wu-cloud-service/wu-service-ai';       port = 9204 },
    @{ name = 'pay';        dir = 'wu-cloud-service/wu-service-pay';      port = 9205 },
    @{ name = 'order';      dir = 'wu-cloud-service/wu-service-order';    port = 9206 },
    @{ name = 'file';       dir = 'wu-cloud-service/wu-service-file';     port = 9207 },
    @{ name = 'infra';      dir = 'wu-cloud-service/wu-service-infra';    port = 9300 }
)

foreach ($svc in $services) {
    $work = Join-Path $root $svc.dir
    if (-not (Test-Path $work)) {
        Write-Host "[跳过] $($svc.name)：目录不存在 $work"
        continue
    }
    $log = Join-Path $logDir "$($svc.name).log"
    Start-Process -FilePath 'cmd.exe' `
        -ArgumentList '/c', "mvn spring-boot:run -DskipTests > `"$log`" 2>&1" `
        -WorkingDirectory $work -WindowStyle Hidden
    Write-Host "[启动] $($svc.name) (:$($svc.port)) -> $log"
    Start-Sleep -Seconds 2
}
