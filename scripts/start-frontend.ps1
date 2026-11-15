<#
.SYNOPSIS
  启动前端 dev server（固定 5173，端口被占用直接报错，不自动跳端口）
#>
param([int]$Port = 5173)

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
    Write-Host "[start-frontend] 端口 $Port 已被占用：PID=$owner 进程=$name" -ForegroundColor Red
    Write-Host "  若是上次遗留的 vite，执行 .\scripts\stop-all.ps1 再起" -ForegroundColor Yellow
    exit 1
}

Write-Host "[start-frontend] 启动 vite（port=$Port）..."
Start-Process -FilePath 'C:\Windows\System32\cmd.exe' -ArgumentList @('/c','npm run dev') `
    -WorkingDirectory (Join-Path $root 'frontend') -WindowStyle Hidden `
    -RedirectStandardOutput (Join-Path $logDir 'frontend.log') `
    -RedirectStandardError (Join-Path $logDir 'frontend.err.log')

for ($i = 0; $i -lt 30; $i++) {
    Start-Sleep -Seconds 1
    $code = curl.exe -s -o NUL -w "%{http_code}" "http://127.0.0.1:$Port/" 2>$null
    if ("$code" -eq '200') {
        Write-Host "[start-frontend] 就绪：http://localhost:$Port/" -ForegroundColor Green
        exit 0
    }
}
throw "[start-frontend] 30 秒内未就绪，请查看 work\frontend.log"