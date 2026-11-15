# 一键启停与打包脚本

项目为「后端 Spring Boot + 前端 Vue3 + 独立 MySQL 实例」三段式，脚本把启动顺序、端口、
数据库参数都固化下来，避免每次靠记忆手敲。

## 前置条件

| 依赖 | 路径（默认） | 说明 |
|---|---|---|
| JDK 17+ | `JAVA_HOME` 指向 | 后端编译运行 |
| Maven 3.9 | `C:\Document\Develop\Environment\Maven\apache-maven-3.9.16\bin\mvn.cmd` | 也可自行改脚本里的路径 |
| Node 18+ | `node` / `npm` 在 PATH | 前端 dev/build |
| MySQL 8.0 | `C:\Program Files\MySQL\MySQL Server 8.0` | 只用它的 mysqld/mysqladmin，不依赖 3306 系统服务 |

数据库实例：**3307**，数据目录在项目内 `work/mysql-data`（已加入 .gitignore），root 密码 **123456**。

## 脚本

| 脚本 | 作用 |
|---|---|
| `scripts/start-db.ps1` | 起 3307 实例；首次会自动 `--initialize-insecure` 初始化（随后脚本把 root 设为 123456 由应用配置使用）；轮询直到就绪 |
| `scripts/start-backend.ps1` | 起后端 8080，连 3307 上指定库（默认 `gss_demo`）；轮询 `/api/auth/login` |
| `scripts/start-frontend.ps1` | 起 vite 5173（`strictPort`，端口占用直接报错，不会再跳 5174）；轮询首页 |
| `scripts/stop-all.ps1` | 按 前端 → 后端 → 数据库 顺序停止；数据库用 `mysqladmin shutdown` 优雅关闭，数据保留 |
| `scripts/package.ps1` | `mvn package -DskipTests` + `npm run build`，打印 jar 与 dist 路径 |

## 常用命令

```powershell
# 起整套
.\scripts\start-db.ps1
.\scripts\start-backend.ps1                 # 默认 gss_demo
.\scripts\start-backend.ps1 -DbName gss     # 测试库
.\scripts\start-frontend.ps1

# 打开
http://localhost:5173    admin / 123456
                         gate / 123456
                         company1 / 123456
                         13800000001 / 123456（司机）

# 停整套
.\scripts\stop-all.ps1

# 打包
.\scripts\package.ps1
#  产物：backend\target\gss-backend-1.0.0.jar
#        frontend\dist\
```

## 数据库

| 库 | 用途 |
|---|---|
| `gss` | 全量测试库（可跑集成测试） |
| `gss_demo` | 演示库（含少量演示数据） |
| `gss_stats` | 统计验证库（20 条对账数据集） |

建库：`mysql -h 127.0.0.1 -P 3307 -u root -p123456 < sql/gss.sql`
老库升级顺序：`06018c7` 基线 → `20261114_eir_approval_migration.sql` → `20261115_m2_voucher_migration.sql` → `20261115_m3_lane_migration.sql`

## 常见故障

**1. 端口被占用**
`start-*.ps1` 会打印占用进程的 PID 与进程名并退出，不会静默换端口。
要重启就先 `.\scripts\stop-all.ps1`；若提示需要管理员权限，用任务管理器结束该 PID。

**2. 数据库密码**
3307 实例 root 密码固定 `123456`，与 `application.yml` 默认一致。
早期用 `--initialize-insecure` 起的是空密码，PowerShell 里把环境变量设成空串会**删除变量**，
导致应用回落到默认 123456 而报 `Access denied ... using password: YES`。脚本已统一改成 123456。

**3. Maven 离线**
`package.ps1` **不带 `-o`**：离线仓库里没有 `spring-boot-maven-plugin` 时 `mvn -o package` 会失败。
跑单元测试可以用 `mvn -o test`（默认排除 integration 标签，不需要数据库）。

**4. 集成测试**
`mvn -o test` 只跑 Mockito 单测（无数据库也能全绿）。
真库集成测试：`mvn -o test -Pintegration`，需先起 3307 且库里有表结构。

**5. JDBC URL 不要带 & 参数**
脚本里 URL 写的是 `jdbc:mysql://127.0.0.1:3307/gss_demo`。
带 `?useUnicode=true&...` 的 URL 经过多层引号（PowerShell → cmd → mvn）容易被截断成
`Identifier name '=true&characterencoding...' is too long`，本机用驱动默认参数即可。