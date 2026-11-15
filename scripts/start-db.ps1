<#
.SYNOPSIS
  启动 3307 独立 MySQL 实例（项目 work/ 下，数据与临时目录解耦）
.EXAMPLE
  .\scripts\start-db.ps1
  .\scripts\start-db.ps1 -Port 3307
#>
param(
    [int]$Port = 3307,
    [string]$MySqlBin = 'C:\Program Files\MySQL\MySQL Server 8.0\bin',
    [string]$MySqlBase = 'C:\Program Files\MySQL\MySQL Server 8.0'
)

$env:MYSQL_PWD = '123456'
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$dataDir = Join-Path $root 'work\mysql-data'
$logDir  = Join-Path $root 'work\mysql-log'
New-Item -ItemType Directory -Force -Path $logDir | Out-Null

# 已在运行就不重复起
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
    Write-Host "[start-db] 端口 $Port 已在监听（PID $owner），视为已在运行，跳过" -ForegroundColor Yellow
    exit 0
}

# 首次使用：初始化数据目录（root / 123456）
if (-not (Test-Path (Join-Path $dataDir 'mysql'))) {
    Write-Host "[start-db] 数据目录不存在，初始化 $dataDir ..."
    & "$MySqlBin\mysqld.exe" --no-defaults --initialize-insecure --basedir="$MySqlBase" --datadir="$dataDir" --console
    if ($LASTEXITCODE -ne 0) { throw "初始化 MySQL 失败（exit $LASTEXITCODE）" }
}

$ini = Join-Path $logDir 'my.ini'
$iniText = @"
[mysqld]
basedir=$($MySqlBase -replace '\\','/')
datadir=$($dataDir -replace '\\','/')
port=$Port
bind-address=127.0.0.1
log-error=$((Join-Path $logDir 'err.log') -replace '\\','/')
pid-file=$((Join-Path $logDir 'mysqld.pid') -replace '\\','/')
lc-messages-dir=$($MySqlBase -replace '\\','/')/share
character-set-server=utf8mb4
default-time-zone=+08:00
"@
[System.IO.File]::WriteAllText($ini, $iniText, (New-Object System.Text.UTF8Encoding($false)))

Write-Host "[start-db] 启动 mysqld（port=$Port, datadir=$dataDir）..."
Start-Process -FilePath "$MySqlBin\mysqld.exe" -ArgumentList @("--defaults-file=$ini") -WindowStyle Hidden

for ($i = 0; $i -lt 40; $i++) {
    Start-Sleep -Milliseconds 800
    $out = & "$MySqlBin\mysqladmin.exe" -h 127.0.0.1 -P $Port -u root --protocol=tcp ping 2>$null
    if ($out -match 'alive') {
        Write-Host "[start-db] 就绪：127.0.0.1:$Port （root / 123456）" -ForegroundColor Green
        exit 0
    }
}
throw "[start-db] 30 秒内未就绪，请查看 $logDir\err.log"