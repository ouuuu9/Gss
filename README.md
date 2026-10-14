# 基于 Spring Boot 的集装箱码头闸口预约与办单管理系统

B/S 架构 Web 信息管理系统，模拟集装箱码头闸口无纸化作业全流程：

**单证录入 → 派单 → 司机预约 → 刷卡进闸 → 办单放行 → 出闸 → 收费结算 → 审单完成**

## 一、技术栈

| 端 | 技术 |
|---|---|
| 后端 | Java 17、Spring Boot 3.2.x、MyBatis、MySQL 8、JWT（jjwt 0.12.x）、BCrypt |
| 前端 | Vue 3、Vite 5、Element Plus、Axios、Pinia、Vue Router |
| 架构 | 前后端分离，后端单体应用；无 Redis / 消息队列等中间件 |

## 二、环境要求

- JDK 17 及以上
- Maven 3.8 及以上
- Node.js 18 及以上（含 npm）
- MySQL 8.x

## 三、数据库初始化

1. 登录 MySQL，执行建库脚本（自动建库 `gss`、7 张表、种子数据）：

```bash
mysql -u root -p < sql/gss.sql
```

也可使用 Navicat / DataGrip 等工具打开 [sql/gss.sql](sql/gss.sql) 直接运行。

2. 所有字段均带中文 COMMENT 注释。种子数据包含 2 家运输公司、3 台拖车、3 名司机、2 张示例单证。

## 四、后端启动

1. 修改数据库连接配置 [backend/src/main/resources/application.yml](backend/src/main/resources/application.yml)：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/gss?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: 你的MySQL密码
```

2. 启动（任选一种方式）：

```bash
# 方式一：Maven 命令
cd backend
mvn spring-boot:run

# 方式二：IDE 中运行启动类
com.gss.GssApplication
```

后端启动成功后地址：`http://localhost:8080`，接口统一前缀 `/api`。

3. **首次启动自动初始化系统账号**（见 DataInitializer），密码使用 BCrypt 加密入库。

## 五、前端启动

```bash
cd frontend
npm install        # 安装依赖
npm run dev        # 启动开发服务器 -host
```

浏览器自动打开 `http://localhost:5173`。开发环境已配置代理：`/api` → `http://localhost:8080`，无需处理跨域。

生产构建：`npm run build`，产物在 `frontend/dist`。

## 六、默认账号（密码均为 123456）

| 角色 | 用户名 | 说明 |
|---|---|---|
| 码头管理员 ADMIN | admin | 录入单证、用户管理、审单、全部报表 |
| 闸口办单员 GATE | gate | 刷卡进闸/出闸办单 |
| 运输公司用户 COMPANY | company1 | 第一运输有限公司：派单、预约、缴费 |
| 运输公司用户 COMPANY | company2 | 第二运输有限公司 |
| 司机 DRIVER | 13800000001 | 司机张伟（IC 卡 IC0001） |
| 司机 DRIVER | 13800000002 | 司机陈强（IC 卡 IC0002） |
| 司机 DRIVER | 13800000003 | 司机李明（IC 卡 IC0003） |

## 七、业务流程体验路径

建议按以下顺序走完整条链路：

1. **admin 登录** → 「EIR单证管理」→ 录入单证（选择业务类型与作业公司；或直接使用列表中初始单证）
2. **company1 登录** → 「EIR单证管理」→ 对单证点【派单】，选择本公司司机与拖车
   - 若单证为 **GR-F 还重单**，派单后自动生成一笔 50 元未缴费收费记录
3. **company1 / 司机** → 「进港预约」→ 创建预约（选择已派单单证、预约日期）
4. **company1**（仅还重单需要）→ 「收费管理」→ 单笔或勾选多笔【批量缴费】
5. **gate 登录** → 「闸口办单」→ 输入 IC 卡号（如 IC0001）点【进闸办单】
   - 未预约 / 还重单未缴费 → 系统拦截并提示具体原因
6. 作业完成后输入同一 IC 卡号点【出闸办单】
7. **admin 登录** → EIR 列表点【审单】→ 单证完成
8. 「收费报表」可按公司查看缴费笔数、金额与收缴率；「首页看板」查看统计

## 八、核心设计说明

- **统一响应**：`Result<T>` 包装，格式 `{ code, msg, data }`；分页使用 `PageResult`
- **JWT 认证**：请求头 `Authorization: Bearer <token>`；JwtInterceptor 完成认证与 `@RequireRole` 角色校验
- **数据权限（后端强制，不依赖前端）**：
  - COMPANY 只能查询/操作本公司的单证、拖车、司机、收费数据
  - DRIVER 只能查询本人单证、创建/取消本人预约
- **状态机校验**：状态流转规则全部写在 Service 层，每个写操作先校验前置状态，禁止越级
- **收费规则**：固定 50 元/单，仅 GR-F 还重单在派单时生成；取消派单自动回滚收费记录

## 九、目录结构

```
Gss/
├── sql/gss.sql                 # 建库脚本（7表+注释+种子数据）
├── backend/                    # Spring Boot 后端（包名 com.gss）
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/gss/
│       │   ├── GssApplication.java
│       │   ├── common/         # Result / PageResult / Constants / 异常 / 全局异常处理
│       │   ├── config/         # WebConfig / DataInitializer
│       │   ├── security/       # JwtUtil / JwtInterceptor / RequireRole / UserContext
│       │   ├── entity/         # 7 个实体
│       │   ├── mapper/         # 7 个 Mapper 接口
│       │   ├── dto/            # 请求 DTO
│       │   ├── service/        # 业务逻辑（状态机/数据权限/收费）
│       │   └── controller/     # REST 接口
│       └── resources/
│           ├── application.yml
│           └── mapper/         # 7 个 MyBatis XML
└── frontend/                   # Vue3 前端
    └── src/
        ├── api/                # axios 封装与接口定义
        ├── router/ store/ utils/ layout/
        └── views/              # 10 个业务页面
```
