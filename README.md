# 偶像大师粉丝名片交互平台（Java Web 作业）

本项目是一个面向偶像大师（THE IDOLM@STER）社群的电子粉丝名片互动平台，聚焦“名片展示、分享与评论”三大核心能力。实现采用标准 Java Servlet + JSP 技术栈，配合 HikariCP 连接池、Logback 日志与多层架构（Controller/Filter/DAO/Model），强调安全、可扩展与清晰的数据流。

> 说明：本文档仅介绍实现与设计，不包含任何部署或运行步骤。

---

## 功能概览

- 用户体系
  - 注册（昵称/用户名/密码，需勾选隐私政策）
  - 登录/退出（BCrypt 密码校验，支持登录后安全重定向）
  - 密码修改（旧密码验证 + 新密码哈希存储）
- 名片管理
  - 仪表盘管理多张名片（创建/编辑/删除）
  - 名片展示页（唯一链接 `uniqueLinkId`）、短链 `shortCode` 跳转
  - 可见性：PUBLIC / LINK_ONLY / PRIVATE
  - 图片上传（大小/类型/魔数校验 + 自动判断横/竖版）
  - 自定义 SNS 列表（以 JSON 持久化）
- 评论互动
  - 顶级评论与树形回复（显示相对时间，支持删除）
- 首页浏览
  - 公共名片列表、搜索、分页
  - `?format=1` 返回精简 JSON 以便前端动态加载
- 管理后台
  - 用户/名片双列表分页检索与过滤
  - 用户封禁、删除、角色变更（含“至少保留一名管理员”“超级管理员 admin 规则”等安全约束）
  - 管理名片可见性并自动处理 `shareToken`/`shortCode`

---

## 技术栈与分层

- Web：Java Servlet 4.0 + JSP（JSTL）
- 持久层：JDBC + HikariCP（MySQL）
- 日志：SLF4J + Logback（`src/main/resources/logback.xml`）
- JSON：Gson（轻量序列化/反序列化）
- 密码：BCrypt（`org.mindrot:jbcrypt`）
- 结构：Controller（Servlet）/ Filter / DAO / Model / JSP View

目录结构（节选）：

```
src/main/java/com/example/pcard/
  controller/  # 业务入口（Servlet）
  filter/      # 横切关注点（鉴权、限流、CF 集成、异常）
  dao/         # 数据访问
  model/       # 领域模型
  util/        # 工具（DB/校验/时区/存储）
src/main/webapp/
  *.jsp, /css, /js, /uploads, /WEB-INF/web.xml
src/main/resources/
  db.properties(可选), logback.xml
```

---

## 路由与页面（实现）

- 首页与搜索
  - GET `/`（`HomeServlet`）：
    - 查询参数：`q`（关键词，可空）、`limit`（默认 12）、`offset`（默认 0）
    - `format=1` 时返回 JSON 数组（字段：id, uniqueLinkId, producerName, idolName, region, cardFrontPath）
    - 页面：`index.jsp`
- 账户
  - GET `/login`（登录页：`login.jsp`）；POST `/login`（校验 + 会话）
    - 支持 `redirect` 参数（仅允许相对路径，防开放重定向）
  - GET `/register`（注册页：`register.jsp`）；POST `/register`
  - GET `/logout`（销毁会话）
  - POST `/userAction?action=changePassword`（修改密码）
- 仪表盘与名片
  - GET `/dashboard`（`dashboard.jsp`）：加载当前用户全部名片，可通过 `cardId` 精确选中或 `create=1` 进入创建模式
  - POST `/cardAction`
    - `action=create|update|delete`
    - 图片字段：`cardFront`、`cardBack`（5MB，扩展名与魔数双校验：png/jpeg/gif/webp）
    - 表单字段：`producerName`、`region`、`idolName`、`visibility`、`customSnsName[]`、`customSnsValue[]`
    - 规则：
      - LINK_ONLY 自动补全 `shareToken`
      - PUBLIC/LINK_ONLY 自动生成 7 位 `shortCode`（Base62，碰撞最多重试 5 次）
      - 根据图片自动写入 `imageOrientation`（HORIZONTAL/ VERTICAL）
- 浏览名片与短链
  - GET `/card/{uniqueLinkId}`（`ViewCardServlet` → `viewCard.jsp`）
    - 可见性检查：
      - PUBLIC：所有人可见
      - LINK_ONLY：需匹配 `?token=` 或为拥有者/管理员
      - PRIVATE：仅拥有者/管理员
  - GET `/s/{shortCode}`（`ShortLinkServlet`）：302 跳转至 `/card/{uniqueLinkId}`，如存在 `shareToken` 则追加 `?token=`
- 评论互动
  - POST `/commentAction`
    - `action=add|reply|delete`，参数包含 `cardId`、`cardLink`、`content`，回复时含 `parentId`、`replyToUsername/Nickname`
    - 删除权限：管理员 | 名片所有者 | 评论作者
- 管理后台
  - GET `/admin`（`admin.jsp`）：
    - 用户筛选：`userQ`、`role`（user/admin/all）、`status`（active/banned/all）
    - 名片筛选：`cardQ`、`visibility`（PUBLIC/LINK_ONLY/PRIVATE/all）
    - 分页：`uPage`、`cPage`（每页固定 10）
  - POST `/admin`
    - `action=updateUserStatus|deleteUser|updateUserRole|setCardVisibility`
    - 角色/人员安全约束：
      - 至少保留一名管理员
      - 超级管理员用户名固定为 `admin`：禁止自降级；普通管理员无权升/降他人

---

## 数据模型与数据库

- users
  - id, username(unique), nickname, password(BCrypt), role(user/admin), status(active/banned)
- cards
  - id, user_id(FK), producer_name, region, idol_name
  - card_front_path, card_back_path, image_orientation(HORIZONTAL|VERTICAL)
  - unique_link_id(unique), short_code, share_token
  - custom_sns(JSON), visibility(PUBLIC|LINK_ONLY|PRIVATE)
  - created_at, updated_at
- comments（支持嵌套回复）
  - id, card_id(FK), user_id(FK), username, nickname, content
  - parent_id（自引用 FK）, reply_to_username, reply_to_nickname
  - created_at, updated_at

完整建表与种子数据见 `database/init.sql`（包含默认管理员 `admin/admin` 的密码哈希，首次登录后应修改）。

---

## 文件上传与存储

- 上传校验
  - 大小上限：5MB
  - 扩展名白名单：png/jpg/jpeg/gif/webp
  - 魔数（文件头）二次验证：PNG/JPEG/GIF/WEBP
- 保存路径
  - 优先使用环境变量 `UPLOAD_DIR`，否则默认 `/uploads`
  - 返回路径：
    - 若配置了 `GCS_BUCKET_NAME` 且 `USE_EXTERNAL_STORAGE=true`，返回公开 URL（`https://storage.googleapis.com/{bucket}/uploads/{file}`）
    - 否则返回站点相对路径 `uploads/{file}`
- 额外适配（可选）：`util/CloudStorageUtil` 提供抽象适配 OSS/S3/Azure/GCS/CDN，但当前上传主流程在 `CardServlet#saveUploadedFile` 中直接落盘并按需拼接外链

---

## 过滤器与安全策略

- `CharacterEncodingFilter`（web.xml）
  - 统一请求/响应 UTF-8 编码
- `CloudflareFilter`（@WebFilter, global）
  - 识别 Cloudflare 头（真实 IP、国家、Ray、Bot 分数）并注入 `request` 属性
  - 对受保护路径（/login /register /card /comment /admin）在低 Bot 分数时拦截
- `RateLimitFilter`（@WebFilter, global）
  - 1 分钟滑动窗口限流（默认 1000；登录 50；注册 30；接口 500）
  - 跳过静态资源；超限返回 HTTP 429 + `Retry-After`
- `AuthRefreshFilter`（web.xml）
  - 每次请求刷新会话中的用户信息（来自 DB）
  - 封禁用户强制登出并重定向登录页
  - 阻止直接访问 `admin.jsp`
- `ExceptionHandlingFilter`（@WebFilter, global）
  - 捕获未处理异常，统一日志并返回 500 错误

安全细节：
- 密码存储使用 BCrypt（注册、改密、登录校验）
- 登录重定向仅允许相对路径，防止开放重定向
- 可见性判定在服务端强校验（`ViewCardServlet`）
- DAO 全面使用参数化查询，防 SQL 注入
- 输入清洗与长度限制见 `ValidationUtil`

---

## 配置、连接池与时区

- 数据库连接与连接池
  - `DbUtil` 基于 HikariCP，支持从 `db.properties` 或环境变量读取配置
  - 连接初始化设置 `SET time_zone = '+08:00'`，配合 `TimeZoneUtil` 统一应用时区（默认 Asia/Shanghai）
- 日志
  - `logback.xml` 定义控制台与滚动文件输出，区分普通日志与错误日志

---

## 前端与页面要点

- JSP 视图：`index.jsp`、`login.jsp`、`register.jsp`、`dashboard.jsp`、`admin.jsp`、`viewCard.jsp`
- 组件：`WEB-INF/comment-recursive.jsp` 用于评论树递归渲染
- 静态资源：`/css/style.css`、`/js/script.js`、`/uploads/*`
- 动态加载：主页支持 `?format=1` JSON 流，便于无限滚动/批量加载

---

## 异常与状态码（行为）

- 403：无权访问（例如非管理员访问后台、名片权限不足）
- 404：资源不存在（名片/短链）
- 429：触发应用层限流（RateLimitFilter）
- 500：未捕获异常（ExceptionHandlingFilter 兜底）

---

## 典型交互（实现契约）

- 列表 JSON（首页）
  - GET `/?q={keyword}&limit=12&offset=0&format=1`
  - 返回：`[{ id, uniqueLinkId, producerName, idolName, region, cardFrontPath }]`
- 短链跳转
  - GET `/s/{shortCode}` → 302 → `/card/{uniqueLinkId}[?token=...]`
- 评论新增
  - POST `/commentAction`，表单：`action=add`、`cardId`、`cardLink`、`content`
- 修改可见性（后台）
  - POST `/admin`，表单：`action=setCardVisibility`、`cardId`、`visibility`

