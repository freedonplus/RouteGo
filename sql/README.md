# RouteGo 数据库

单体多模块架构，所有业务共用一个 `routego` 数据库。
MySQL 8.0，统一字符集 `utf8mb4`，排序规则 `utf8mb4_general_ci`。

## 初始化

Linux/macOS:

```bash
mysql -u root -p < sql/routego.sql
```

Windows PowerShell:

```powershell
Get-Content .\sql\routego.sql -Raw | mysql -u root -p
```

## 结构

- 1 个库：`routego`
- 36 张业务表 + 1 张 Seata AT `undo_log` 基础表
- 按原业务域分组：系统权限 15、线路 8、交易 5、客户 3、运营 5
- 不建物理外键，跨业务域关系由应用层保证
- 业务表统一包含审计字段（`create_by/create_time/update_by/update_time`）
  与逻辑删除字段 `deleted`
- `version` 乐观锁字段只加在需要并发控制的表上

## 历史脚本

`routego_admin.sql`、`routego_catalog.sql`、`routego_trade.sql`、
`routego_customer.sql`、`routego_operations.sql`、`schema.sql` 是原微服务版
的分库脚本，保留作参考；单体部署请使用 **`routego.sql`**。

## 数据边界

- 所有表在同一个库，本地事务可以覆盖多个业务域。
- 不建物理外键，关系由应用层维护。
- 跨业务域读写直接走本地 Service 方法，不再有 OpenFeign 的强制边界。
- 后续需要异步解耦时，继续使用 RocketMQ。
