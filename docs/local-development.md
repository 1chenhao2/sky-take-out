# 本地开发说明

## 依赖

- JDK 11
- Maven 3.6+
- MySQL 8
- Redis 6+

## 初始化与配置

```powershell
mysql -u root -p < resources/database/sky.sql
$env:SKY_JWT_ADMIN_SECRET="replace-with-a-random-admin-secret"
$env:SKY_JWT_USER_SECRET="replace-with-a-random-user-secret"
```

MySQL 设置密码时使用 `$env:SKY_DB_PASSWORD`；智能对话使用 `$env:SKY_AI_API_KEY`。

## 构建与启动

```powershell
mvn -DskipTests package
mvn -pl sky-server spring-boot:run
```

接口文档：`http://localhost:8080/doc.html`。
