#!/usr/bin/env sh
# 合成一键初始化脚本 routego-init-all.sql
# 用法：在 sql/ 目录下执行 ./build-init-all.sh
# 产物：routego-init-all.sql = 建表(routego.sql) + 后台数据(admin-init.sql) + 演示数据(demo-data.sql)
set -e
cd "$(dirname "$0")"

cat routego.sql > routego-init-all.sql
cat admin-init.sql >> routego-init-all.sql
cat demo-data.sql >> routego-init-all.sql

echo "生成完成：$(ls -la routego-init-all.sql | awk '{print $5, $9}')"
echo "一键初始化：mysql -u root -p routego < sql/routego-init-all.sql"
