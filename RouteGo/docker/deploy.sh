#!/usr/bin/env bash
# =============================================================================
# RouteGo 生产部署脚本（Linux / macOS，Windows 用 Git Bash 或 WSL）
#
#   ./deploy.sh init      生成 .env（随机密钥）并做基础校验
#   ./deploy.sh up        构建镜像并后台启动（首次会自动 init）
#   ./deploy.sh ps        查看服务与健康状态
#   ./deploy.sh logs app  跟踪日志（默认 app）
#   ./deploy.sh restart   重启全部服务
#   ./deploy.sh down      停止（保留数据卷）
#   ./deploy.sh destroy   停止并删除数据卷（会丢库，需二次确认）
#   ./deploy.sh backup    备份数据库到 ./backup
#   ./deploy.sh backup-install  安装每日 03:17 自动备份（crontab，保留 14 天）
# =============================================================================
set -euo pipefail

cd "$(dirname "$0")"

COMPOSE_BIN=${COMPOSE_BIN:-docker compose}
ENV_FILE=.env
EXAMPLE=.env.example

log()  { printf '\033[0;32m[routego]\033[0m %s\n' "$*"; }
warn() { printf '\033[0;33m[routego]\033[0m %s\n' "$*"; }
die()  { printf '\033[0;31m[routego]\033[0m %s\n' "$*" >&2; exit 1; }

need_cmd() {
  command -v "$1" >/dev/null 2>&1 || die "缺少命令：$1"
}

rand_b64() { openssl rand -base64 32; }
rand_hex() { openssl rand -hex 32; }

# 生成 .env：已存在则不覆盖，只补齐空项
cmd_init() {
  need_cmd openssl
  if [ ! -f "$ENV_FILE" ]; then
    [ -f "$EXAMPLE" ] || die "找不到 $EXAMPLE"
    cp "$EXAMPLE" "$ENV_FILE"
    log "已从 $EXAMPLE 生成 $ENV_FILE"
  else
    log "$ENV_FILE 已存在，仅补齐留空的密钥"
  fi

  # 逐项补齐，保持 .env 幂等；用 awk 而不是 sed -i，避开 BSD/GNU 差异
  # 三种情况：空值行 → 填入；整行缺失 → 追加；已有非空值 → 不动（避免覆盖线上密钥）
  fill() {
    local key="$1" value="$2" tmp
    if grep -qE "^${key}=$" "$ENV_FILE"; then
      tmp=$(mktemp)
      awk -v k="$key" -v v="$value" -F= 'BEGIN{OFS="="} $1==k && $2=="" {$0=k OFS v} {print}' \
        "$ENV_FILE" > "$tmp" && mv "$tmp" "$ENV_FILE"
      log "  生成 ${key}"
    elif ! grep -qE "^${key}=" "$ENV_FILE"; then
      # 旧版 .env 可能整体缺这一项，只补空值行是不够的（会导致密钥为空却通过校验）
      printf '%s=%s\n' "$key" "$value" >> "$ENV_FILE"
      log "  追加 ${key}"
    fi
  }

  fill MYSQL_ROOT_PASSWORD "$(openssl rand -base64 24 | tr -d '/+=' | cut -c1-24)"
  fill MYSQL_PASSWORD      "$(openssl rand -base64 24 | tr -d '/+=' | cut -c1-24)"
  fill REDIS_PASSWORD      "$(openssl rand -hex 16)"
  fill MINIO_ROOT_USER     "routego$(openssl rand -hex 3)"
  fill MINIO_ROOT_PASSWORD "$(openssl rand -base64 24 | tr -d '/+=' | cut -c1-24)"
  fill ROUTEGO_CRYPTO_AES_KEY   "$(rand_b64)"
  fill ROUTEGO_CRYPTO_HMAC_KEY  "$(rand_hex)"
  fill ROUTEGO_TEMP_URL_SECRET  "$(openssl rand -hex 24)"

  chmod 600 "$ENV_FILE" 2>/dev/null || true
  cmd_check
}

# 上线前把「漏填就出事」的项挑出来
cmd_check() {
  [ -f "$ENV_FILE" ] || die "$ENV_FILE 不存在，先执行 ./deploy.sh init"
  local missing=()
  local key value
  for key in MYSQL_ROOT_PASSWORD MYSQL_PASSWORD REDIS_PASSWORD \
             MINIO_ROOT_USER MINIO_ROOT_PASSWORD \
             ROUTEGO_CRYPTO_AES_KEY ROUTEGO_CRYPTO_HMAC_KEY ROUTEGO_TEMP_URL_SECRET \
             ROUTEGO_SECURITY_ALLOWED_ORIGINS; do
    value=$(grep -E "^${key}=" "$ENV_FILE" | head -1 | cut -d= -f2- || true)
    [ -z "${value}" ] && missing+=("$key")
  done
  if [ ${#missing[@]} -gt 0 ]; then
    die "以下配置为空，请编辑 $ENV_FILE 后重试：${missing[*]}"
  fi

  # AES 密钥必须是 base64 的 16/24/32 字节，否则启动时才报错
  local aes len
  aes=$(grep -E '^ROUTEGO_CRYPTO_AES_KEY=' "$ENV_FILE" | cut -d= -f2-)
  len=$(printf '%s' "$aes" | base64 -d 2>/dev/null | wc -c | tr -d ' ')
  case "$len" in
    16|24|32) : ;;
    *) die "ROUTEGO_CRYPTO_AES_KEY 解码后必须是 16/24/32 字节（当前 ${len:-0}），可用 openssl rand -base64 32 生成" ;;
  esac

  if grep -qE '^ROUTEGO_SECURITY_ALLOWED_ORIGINS=http://localhost' "$ENV_FILE"; then
    warn "ROUTEGO_SECURITY_ALLOWED_ORIGINS 仍是 localhost，正式域名上线后记得改成实际前端地址"
  fi

  # 前端由浏览器直连后端，接口地址写死在 RouteGo-admin/.env.production（构建期注入）
  local front_env="../../RouteGo-admin/.env.production" api_base
  api_base=$(grep -E '^VITE_API_BASE_URL=' "$front_env" 2>/dev/null | head -1 | cut -d= -f2- || true)
  case "${api_base}" in
    ''|/api)
      warn "RouteGo-admin/.env.production 的 VITE_API_BASE_URL 为 ${api_base:-空}，但本套 nginx 已不做反代——请改成后端真实地址，如 http://<服务器IP>:8080/api"
      ;;
    *127.0.0.1*|*localhost*)
      warn "VITE_API_BASE_URL 为 ${api_base}，只有服务器本机浏览器能用，远程访问会请求失败——请改成公网可达地址"
      ;;
  esac
  log "配置检查通过"
}

cmd_up() {
  need_cmd docker
  [ -f "$ENV_FILE" ] || cmd_init
  cmd_check
  log "构建镜像（首次会比较慢，Maven / npm 依赖需要下载）"
  $COMPOSE_BIN build
  log "启动服务"
  $COMPOSE_BIN up -d --remove-orphans

  log "等待后端健康检查通过（最多 3 分钟）"
  local ok=0 i
  for i in $(seq 1 36); do
    if $COMPOSE_BIN ps --format '{{.Service}} {{.Health}}' 2>/dev/null | grep -q '^app healthy'; then
      ok=1; break
    fi
    sleep 5
  done

  if [ "$ok" -eq 1 ]; then
    log "启动完成：http://<服务器IP>:${WEB_HTTP_PORT:-80}"
    warn "初始超管账号 taishangxue520 / 12345678Ss，登录后立刻改密码并给每个员工建独立账号"
  else
    warn "后端健康检查未通过，查看日志：./deploy.sh logs app"
    $COMPOSE_BIN ps
    exit 1
  fi
}

cmd_down()    { $COMPOSE_BIN down; }
cmd_restart() { $COMPOSE_BIN restart; }
cmd_ps()      { $COMPOSE_BIN ps; }
cmd_logs()    { local svc=${1:-app}; $COMPOSE_BIN logs -f --tail=200 "$svc"; }

cmd_destroy() {
  warn "这会删除 MySQL / Redis / MinIO 的数据卷，且不可恢复。"
  read -r -p "确认输入 DESTROY 继续：" answer
  [ "$answer" = "DESTROY" ] || die "已取消"
  $COMPOSE_BIN down -v
}

cmd_backup() {
  need_cmd docker
  mkdir -p backup
  local file="backup/routego-$(date +%Y%m%d-%H%M%S).sql.gz"
  # 只取值、不 source .env：JAVA_OPTS 这类带空格的行 source 会把参数当命令执行
  local dbpass dbname
  dbpass=$(grep -E '^MYSQL_ROOT_PASSWORD=' "$ENV_FILE" | cut -d= -f2-)
  dbname=$(grep -E '^MYSQL_DATABASE=' "$ENV_FILE" | cut -d= -f2- || true)
  dbname=${dbname:-routego}
  log "导出数据库 $dbname 到 $file"
  $COMPOSE_BIN exec -T mysql \
    mysqldump -uroot -p"${dbpass}" \
    --single-transaction --quick --routines --events \
    --default-character-set=utf8mb4 "$dbname" | gzip > "$file"
  log "备份完成：$(du -h "$file" | cut -f1)  $file"
}

# 每日自动备份（审核报告 P1-8）：方案书 12 章要求「每日全量备份」，手动 backup
# 忘了跑等于没有备份。幂等安装：重复执行会替换旧的 cron 条目。
# 保留 14 天（方案书允许 7–30 天），备份失败当天不会清理旧文件（&& 串联）。
BACKUP_KEEP_DAYS=14
cmd_backup_install() {
  need_cmd crontab
  local script_dir
  script_dir=$(cd "$(dirname "$0")" && pwd)
  local cron_line
  cron_line="17 3 * * * cd ${script_dir} && ./deploy.sh backup >> backup/backup.log 2>&1 && find ${script_dir}/backup -name 'routego-*.sql.gz' -mtime +$((BACKUP_KEEP_DAYS - 1)) -delete"
  (crontab -l 2>/dev/null | grep -v 'deploy.sh backup' ; echo "$cron_line") | crontab -
  log "已安装每日 03:17 自动备份，保留 ${BACKUP_KEEP_DAYS} 天。查看：crontab -l"
  warn "cron 环境 PATH 精简，请确认 docker 在 /usr/bin 或 /usr/local/bin 下（command -v docker 验证）；日志在 docker/backup/backup.log"
}

usage() {
  sed -n '2,20p' "$0" | sed 's/^# \{0,1\}//'
  exit 1
}

case "${1:-up}" in
  init)    cmd_init ;;
  check)   cmd_check ;;
  up)      cmd_up ;;
  down)    cmd_down ;;
  destroy) cmd_destroy ;;
  restart) cmd_restart ;;
  ps)      cmd_ps ;;
  logs)    shift; cmd_logs "$@" ;;
  backup)  cmd_backup ;;
  backup-install) cmd_backup_install ;;
  *)       usage ;;
esac
