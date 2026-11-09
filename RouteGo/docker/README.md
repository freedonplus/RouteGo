# RouteGo · 生产部署（Docker Compose）

一套 compose 拉起 **MySQL 8 + Redis 7 + MinIO + 后端 + 管理后台**。
管理后台由 nginx 托管静态产物，**不做 API 反代**——浏览器直接请求后端，
接口地址来自 `RouteGo-admin/.env.production` 的 `VITE_API_BASE_URL`，前后端可分别部署。

```
RouteGo/docker/
├── docker-compose.yml     # 五个服务 + 数据卷 + 健康检查
├── Dockerfile             # 后端（Maven 多阶段 → JRE 17 Alpine，非 root 运行）
├── Dockerfile.admin       # 管理后台（Node 构建 → nginx 托管静态产物）
├── Dockerfile.mysql       # MySQL（把 my.cnf 烘进镜像，避免挂载后权限被改写而失效）
├── nginx/routego.conf     # 纯静态托管：SPA 回退、静态缓存、健康检查
├── mysql/my.cnf           # utf8mb4、时区、慢查询、严格模式（构建时 COPY 进镜像）
├── .env.example           # 变量模板（提交入库）
├── .env                   # 实际配置，含随机密钥（已被 .gitignore 忽略）
└── deploy.sh              # init / up / ps / logs / restart / down / backup
```

## 一、前置条件

- Docker Engine 24+，Docker Compose v2（`docker compose version` 有输出即可）
- 建议 **4 核 8G** 起：MySQL 2G + 后端 2G + MinIO/Redis/nginx 各几百兆
- 服务器放行 `80`（或 `443`，如果前面有 SLB/自己加 TLS）

## 二、三步上线

```bash
cd RouteGo-admin
vim .env.production   # 唯一必改：VITE_API_BASE_URL 改成浏览器能访问到的后端地址

cd ../RouteGo/docker
cp .env.example .env
./deploy.sh init      # 自动生成 MySQL/Redis/MinIO/加密密钥，并检查必填项
vim .env              # 必改：ROUTEGO_SECURITY_ALLOWED_ORIGINS 改成前端访问地址
./deploy.sh up        # 构建 + 启动 + 等健康检查
```

`up` 结束后访问 `http://<服务器IP>:${WEB_HTTP_PORT}`，用初始化脚本里的超管账号
`taishangxue520 / 12345678Ss` 登录，**第一件事是改密码，并给每个员工建独立账号**。

> 前端和后端是两个入口：管理后台在 `${WEB_HTTP_PORT}`，接口在 `${APP_PORT}`。
> `VITE_API_BASE_URL` 必须写浏览器能访问到的地址；写成 `127.0.0.1` 只有服务器本机浏览器能用，
> 远程访问会登录失败——`deploy.sh check` 会给出提示。

> 首次启动会自动执行 `sql/routego.sql`（建表）与 `sql/admin-init.sql`（角色/权限/菜单/超管），
> **只在 MySQL 数据卷为空时执行**。已有数据库请勿用本方式覆盖，见「升级与迁移」。

### 不装 bash 的机器（Windows）

```powershell
cd RouteGo\docker
Copy-Item .env.example .env
# 手工把 .env 里留空的密钥填上（openssl 或任意随机串），然后：
docker compose build
docker compose up -d
docker compose ps
```

## 三、.env 关键项

| 变量 | 说明 |
| --- | --- |
| `WEB_HTTP_PORT` | 管理后台对外端口，默认 80 |
| `MYSQL_ROOT_PASSWORD` / `MYSQL_PASSWORD` | 数据库 root 与业务账号密码，`init` 自动生成 |
| `MYSQL_INNODB_BUFFER_POOL` | InnoDB 缓冲池，建议机器内存 50%~60% |
| `REDIS_PASSWORD` | Redis 密码，`init` 自动生成 |
| `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD` | 对象存储账号，**应用用的就是这组** |
| `ROUTEGO_CRYPTO_AES_KEY` | AES-256 密钥，**base64 解码后必须 32 字节**；丢了历史手机号解不开 |
| `ROUTEGO_CRYPTO_HMAC_KEY` | 盲索引密钥，同上，且不能与 AES 相同 |
| `ROUTEGO_TEMP_URL_SECRET` | 文件预览链接签名密钥 |
| `ROUTEGO_SECURITY_ALLOWED_ORIGINS` | CORS 白名单，逗号分隔，**必须改成真实域名** |
| `SA_TOKEN_COOKIE_SECURE` | 上了 HTTPS 保持 `true`；纯 HTTP 试运行改 `false`，否则 Cookie 登录态不生效 |
| `ROUTEGO_SMS_ENABLED` | 填好 `ROUTEGO_SMS_ID/KEY` 后改 `true`，否则 C 端手机号验证码登录不可用 |
| `JAVA_OPTS` | 默认按容器内存 75% 设置堆，小机器不用改 |

`.env` 只存在于服务器上，已被 `.gitignore` 忽略。**请把它和密钥备份进密码管理器**：
AES/HMAC 密钥换掉后，库里已有的手机号、证件号密文将无法解密。

## 四、端口与网络

| 服务 | 容器内 | 宿主机 | 说明 |
| --- | --- | --- | --- |
| web | 80 | `${WEB_HTTP_PORT}` | 只托管前端静态文件，**不做 API 反代** |
| app | 8080 | `${APP_BIND_HOST}:${APP_PORT}` | 浏览器直连，默认监听 `0.0.0.0` |
| app 管理端点 | 9090 | 不发布 | `/actuator/health`、`/actuator/prometheus` |
| mysql | 3306 | `127.0.0.1:${MYSQL_PUBLISH_PORT}` | 只绑本机 |
| redis | 6379 | 不发布 | 只在容器网络内 |
| minio | 9000 / 9001 | 控制台绑 `127.0.0.1:9001` | 对象下载一律走后端签名链接 |

需要 Prometheus 采集时，给 `app` 加一行 `- "127.0.0.1:9090:9090"` 再 `./deploy.sh restart`。

### 前后端分离：接口地址怎么配

管理后台不再经 nginx 反代 `/api`，浏览器直接请求后端，接口地址在**前端项目的 env 文件**里：

```ini
# RouteGo-admin/.env.production
VITE_API_BASE_URL=https://api.example.com/api
```

这是**构建期**注入：改完要重新构建 web 镜像（`./deploy.sh up` 会自动重建），
因为地址已经被 Vite 编译进静态产物。

两种常见形态：

| 场景 | VITE_API_BASE_URL |
| --- | --- |
| 后端独立域名 | `https://api.example.com/api` |
| 同机不同端口 | `http://1.2.3.4:8080/api` |
| 外层 nginx 同域反代 `/api` | `/api` |

后端要能被浏览器访问到，`.env` 里对应两项：

```ini
APP_BIND_HOST=0.0.0.0            # 后端监听所有网卡（只允许本机排查时改 127.0.0.1）
APP_PORT=8080
```

跨域相关的三件事必须一起对上，否则表现为「能打开页面但登录不上」：

1. `ROUTEGO_SECURITY_ALLOWED_ORIGINS` 要包含前端访问地址（含协议和端口），多个用逗号分隔；
2. 前端 axios 已开 `withCredentials`，后端 CORS 已开 `allowCredentials`，不能再用 `*`；
3. Sa-Token 用 HttpOnly Cookie 存登录态。**前后端跨站**（域名不同）时必须上 HTTPS 并把
   `SA_TOKEN_COOKIE_SECURE` 设为 `true`，同时 Cookie 需要 `SameSite=None`；同站不同端口不受影响。

登录后的写操作还会经过 `CsrfOriginFilter` 校验 `Origin`，同样依赖上面第 1 条的名单。

### 关于 MySQL 配置

`mysql/my.cnf` 是**构建进镜像**的（`Dockerfile.mysql` 里 `COPY` 后 `chmod 644`），不是 bind mount。
原因是宿主机文件挂进容器后权限会变成 `0777`，MySQL 判定「全局可写」直接忽略该配置，
只在日志里留一行 `World-writable config file ... is ignored`，`sql_mode`、慢查询、`max_allowed_packet`
就全部悄悄失效。改完 `my.cnf` 需要重建镜像才生效：

```bash
docker compose build mysql && docker compose up -d mysql
```

验证方式：`docker compose exec mysql mysql -uroot -p -e "select @@sql_mode, @@slow_query_log, @@max_allowed_packet"`。

## 五、HTTPS

本 compose 不带证书。前后端分离后，有两个要暴露的入口，常见做法：

1. **前面挂 SLB / 云负载均衡（推荐）**：前端域名回源到本机 `${WEB_HTTP_PORT}`，
   后端域名回源到本机 `${APP_PORT}`，证书都放云上。
   注意把 SLB 网段加进 `application-prod.yaml` 的 `routego.security.trusted-proxies`，
   否则登录日志里的 IP 是 SLB 地址。此时 `VITE_API_BASE_URL=https://api.example.com/api`。

2. **本机再加一层 nginx + certbot，同域反代**：本套 compose 的 `WEB_HTTP_PORT` 改成 `8081`、
   `APP_BIND_HOST` 改回 `127.0.0.1`，外层 nginx 用同一个域名分流——静态走前端，`/api` 走后端：

```nginx
server {
    listen 443 ssl;
    server_name admin.example.com;

    location / {
        proxy_pass http://127.0.0.1:8081;
    }

    # 客服实时推送：必须关缓冲、放长超时，否则消息「攒一批才到」或 60s 断连
    location /api/operations/cs/stream {
        proxy_pass         http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header   Connection '';
        proxy_buffering    off;
        proxy_read_timeout 1h;
        gzip               off;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host              $host;
        proxy_set_header X-Real-IP         $remote_addr;
        proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_request_buffering off;
    }
}
```

这种形态下 `VITE_API_BASE_URL` 写 `/api` 即可（同源），重新构建 web 镜像生效。上了 HTTPS 之后记得把 `.env` 的
`ROUTEGO_SECURITY_ALLOWED_ORIGINS` 换成 `https://...`。

## 六、日常运维

```bash
./deploy.sh ps                 # 服务与健康状态
./deploy.sh logs app           # 跟后端日志（web / mysql 同理）
./deploy.sh restart            # 重启全部
./deploy.sh backup             # 导出库到 ./backup/routego-时间.sql.gz
./deploy.sh backup-install     # 安装每日 03:17 自动备份（crontab，保留 14 天），幂等可重复执行
./deploy.sh down               # 停止，保留数据卷
./deploy.sh destroy            # 停止并删卷（会丢库，需输入 DESTROY 确认）
```

容器内日志同时写到 `/app/logs/routego.log`（按 100MB 滚动、保留 30 天），
宿主机 `docker compose logs` 也能看；需要落盘到宿主时把 `app-logs` 卷换成
`./logs:/app/logs` 并 `chown 100:101 ./logs`。

## 七、升级与迁移

**只改代码**：

```bash
git pull
cd RouteGo/docker && ./deploy.sh up      # 重新构建镜像并滚动重启
```

**有结构变更**（`sql/upgrade-*.sql`）：先备份，再手工执行，最后重启后端：

```bash
./deploy.sh backup
docker compose exec -T mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" routego' \
  < ../../sql/upgrade-cs-assign-permission.sql
./deploy.sh restart
```

> 权限点是写库 + Redis 缓存的：直接改库不会让在线用户立即生效，
> 要么在「角色管理」里保存一次角色，要么清掉 `admin:config:SaToken:*` 缓存。

**从已有库迁移进来**：把 `.env` 的数据库指向旧实例，或在 compose 里删掉
`docker-entrypoint-initdb.d` 的两行挂载，再用 `./deploy.sh backup` 的产物导入。

## 八、故障排查

| 现象 | 原因与处理 |
| --- | --- |
| `app` 一直 unhealthy | `./deploy.sh logs app`。常见是 MySQL 还没就绪、或 `.env` 的密钥为空导致启动失败 |
| 页面能开，登录 401/白屏 | `ROUTEGO_SECURITY_ALLOWED_ORIGINS` 没改成实际域名；或 `SA_TOKEN_COOKIE_SECURE=true` 但在用 HTTP |
| 上传图片失败 | MinIO 未起或账号密码与 `MINIO_ROOT_*` 不一致；也可临时 `ROUTEGO_STORAGE_TYPE=local` 改用卷 |
| 客服消息延迟到达 | 外层代理没关 `proxy_buffering`，或 `proxy_read_timeout` 小于心跳间隔 |
| 时间差 8 小时 | 确认 `TZ`、`--default-time-zone`、`connection-init-sql` 三处都是东八区（本配置已处理） |
| MySQL 启动后立刻退出 | `MYSQL_INNODB_BUFFER_POOL` 超过容器内存；小机器调到 `256M` |
| 改了代码但页面没变 | 浏览器强刷；`web` 镜像需要重新 `build` 才会更新静态产物 |

## 九、上线前安全清单

- [ ] 改掉超管初始密码，删除或停用演示账号
- [ ] `.env` 里 5 个密钥全部为非默认随机值，并已离线备份
- [ ] `ROUTEGO_SECURITY_ALLOWED_ORIGINS` 只包含真实域名
- [ ] 生产默认关闭 Swagger（`ROUTEGO_OPENAPI_ENABLED` 不设或 `false`）
- [ ] MySQL / Redis / MinIO 端口没有直接暴露到公网（保持 `127.0.0.1` 绑定）
- [ ] 已执行 `./deploy.sh backup-install` 安装每日自动备份（03:17，保留 14 天）
- [ ] 已把 `admin:config:SaToken:*` 之外的敏感缓存策略确认一遍（Redis 设了密码、未发布端口）
