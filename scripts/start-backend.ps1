<#
.SYNOPSIS
  启动后端（默认 8080），连 3307 上的指定库
.EXAMPLE
  .\scripts\start-backend.ps1
  .\scripts\start-backend.ps1 -DbName gss_demo
#>
param(
    [int]$Port = 8080,
    [int]$DbPort = 3307,
    [string]$DbName = 'gss_demo',
    [string]$DbUser = 'root',
    [string]$DbPassword = '123456'
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$logDir = Join-Path $root 'work'
New-Item -ItemType Directory -Force -Path $logDir | Out-Null

# 读取端口监听者 PID（用 netstat 解析，避免不同会话下 Get-NetTCPConnection 受权限影响）
function Get-PortOwnerPid([int]$port) {
    $lines = netstat -ano | Select-String -Pattern "LISTENING" | Select-String -Pattern (":{0}\s" -f $port)
    if (-not $lines) { return $null }
    $pidText = ($lines[0].Line -split '\s+')[-1]
    if ($pidText -match '^\d+$') { return [int]$pidText }
    return $null
}
$owner = Get-PortOwnerPid $Port
if ($owner) {
    $name = (Get-Process -Id $owner -ErrorAction SilentlyContinue).ProcessName
    Write-Host "[start-backend] 端口 $Port 已被占用：PID=$owner 进程=$name" -ForegroundColor Red
    Write-Host "  若要重启请先执行 .\scripts\stop-all.ps1" -ForegroundColor Yellow
    exit 1
}

# 注意：JDBC URL 不带 & 参数。本机 3307 用 MySQL 8 默认参数即可；带 & 的 URL 经多层引号容易被打断（踩过）
$env:SPRING_DATASOURCE_URL = "jdbc:mysql://127.0.0.1:$DbPort/$DbName"
$env:DB_USERNAME = $DbUser
$env:DB_PASSWORD = $DbPassword

Write-Host "[start-backend] 启动后端（db=$DbName@$DbPort, port=$Port）..."
Start-Process -FilePath 'C:\Document\Develop\Environment\Maven\apache-maven-3.9.16\bin\mvn.cmd' `
    -ArgumentList @('-q','spring-boot:run','-Dmaven.test.skip=true') `
    -WorkingDirectory (Join-Path $root 'backend') -WindowStyle Hidden `
    -RedirectStandardOutput (Join-Path $logDir 'backend.log') `
    -RedirectStandardError (Join-Path $logDir 'backend.err.log')

$body = '{"username":"admin","password":"123456"}'
$tmp  = Join-Path $env:TEMP 'gss_login_body.json'
[System.IO.File]::WriteAllText($tmp, $body, (New-Object System.Text.UTF8Encoding($false)))

for ($i = 0; $i -lt 45; $i++) {
    Start-Sleep -Seconds 2
    $code = curl.exe -s -o NUL -w "%{http_code}" -X POST "http://127.0.0.1:$Port/api/auth/login" -H "Content-Type: application/json" --data-binary "@$tmp" 2>$null
    if ("$code" -eq '200') {
        Write-Host "[start-backend] 就绪：http://127.0.0.1:$Port/api （日志 work\backend.log）" -ForegroundColor Green
        exit 0
    }
}
throw "[start-backend] 90 秒内登录接口不可用，请查看 work\backend.log"