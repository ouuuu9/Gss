<#
.SYNOPSIS
  按 前端 → 后端 → 数据库 顺序停止（数据库用 mysqladmin shutdown，不 kill）
#>
param(
    [int]$FrontendPort = 5173,
    [int]$BackendPort = 8080,
    [int]$DbPort = 3307,
    [string]$MySqlBin = 'C:\Program Files\MySQL\MySQL Server 8.0\bin'
)

$env:MYSQL_PWD = '123456'
$ErrorActionPreference = 'Continue'
$root = Split-Path -Parent $PSScriptRoot

# 读取端口监听者 PID（用 netstat 解析，避免不同会话下 Get-NetTCPConnection 受权限影响）
function Get-PortOwnerPid([int]$port) {
    $lines = netstat -ano | Select-String -Pattern "LISTENING" | Select-String -Pattern (":{0}\s" -f $port)
    if (-not $lines) { return $null }
    $pidText = ($lines[0].Line -split '\s+')[-1]
    if ($pidText -match '^\d+$') { return [int]$pidText }
    return $null
}
function Stop-PortOwner($port, $label) {
    $pidOwner = Get-PortOwnerPid $port
    if (-not $pidOwner) {
        Write-Host "[stop-all] $label（$port）未在运行"
        return
    }
    $pname = (Get-Process -Id $pidOwner -ErrorAction SilentlyContinue).ProcessName
    Write-Host "[stop-all] 停止 $label：PID=$pidOwner ($pname)"
    try {
        Stop-Process -Id $pidOwner -Force -ErrorAction Stop
        Start-Sleep -Milliseconds 500
    } catch {
        Write-Host "[stop-all]   Stop-Process 失败（$($_.Exception.Message)），改用 taskkill /T" -ForegroundColor Yellow
        & taskkill /PID $pidOwner /T /F 2>&1 | Out-Null
    }
    if (Get-PortOwnerPid $port) {
        Write-Host "[stop-all]   警告：$label 端口 $port 仍被占用（可能需要管理员权限）" -ForegroundColor Red
    }
}

Stop-PortOwner $FrontendPort '前端'
Stop-PortOwner $BackendPort '后端'

# 数据库优雅关闭
$alive = & "$MySqlBin\mysqladmin.exe" -h 127.0.0.1 -P $DbPort -u root --protocol=tcp ping 2>$null
if ($alive -match 'alive') {
    Write-Host "[stop-all] 优雅关闭 MySQL（$DbPort）..."
    & "$MySqlBin\mysqladmin.exe" -h 127.0.0.1 -P $DbPort -u root --protocol=tcp shutdown 2>&1 | Out-Null
    Start-Sleep -Seconds 3
    $after = Get-PortOwnerPid $DbPort
    if ($after) { Write-Host "[stop-all] 警告：MySQL 仍在监听" -ForegroundColor Yellow }
    else { Write-Host "[stop-all] MySQL 已关闭，数据保留在 work\mysql-data" -ForegroundColor Green }
} else {
    Write-Host "[stop-all] 数据库（$DbPort）未在运行"
}

Write-Host "[stop-all] 完成"