<#
.SYNOPSIS
  打包后端（jar）与前端（dist）。注意：不带 -o，离线仓库缺 spring-boot-maven-plugin 会失败。
#>
param()

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot

Write-Host "[package] 打包后端 ..."
Push-Location (Join-Path $root 'backend')
& mvn package -DskipTests
$backendExit = $LASTEXITCODE
Pop-Location
if ($backendExit -ne 0) { throw "后端打包失败（exit $backendExit）" }

Write-Host "[package] 构建前端 ..."
Push-Location (Join-Path $root 'frontend')
& npm run build
$frontendExit = $LASTEXITCODE
Pop-Location
if ($frontendExit -ne 0) { throw "前端构建失败（exit $frontendExit）" }

$jar = Get-ChildItem (Join-Path $root 'backend\target') -Filter '*-1.0.0.jar' -ErrorAction SilentlyContinue |
       Where-Object { $_.Name -notlike '*sources*' } | Select-Object -First 1
$dist = Join-Path $root 'frontend\dist'

Write-Host ""
Write-Host "[package] 产物：" -ForegroundColor Green
if ($jar) { Write-Host ("  后端 jar : " + $jar.FullName + "  (" + [math]::Round($jar.Length / 1MB, 1) + " MB)") }
Write-Host ("  前端 dist: " + $dist)
Write-Host ""
Write-Host "运行 jar：java -jar <jar路径>  （需设置 SPRING_DATASOURCE_URL / DB_PASSWORD）"