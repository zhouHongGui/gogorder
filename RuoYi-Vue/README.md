# gogorder 服务端

gogorder 点单系统的 Java 服务端。

## 技术栈

- Java 21
- Spring Boot
- Spring Security
- MyBatis
- Maven

## 编译

```bash
mvn clean package -DskipTests
```

## 启动

先通过环境变量配置 MySQL 与 Redis：

```powershell
$env:DB_HOST='localhost'
$env:DB_PORT='3306'
$env:DB_NAME='gogorder'
$env:DB_USERNAME='root'
$env:DB_PASSWORD='your-password'

$env:REDIS_HOST='localhost'
$env:REDIS_PORT='6379'
$env:REDIS_DATABASE='0'
# Redis 无密码时无需设置 REDIS_PASSWORD
$env:REDIS_PASSWORD='your-redis-password'

# 可选，未设置时默认使用当前工作目录下的 logs
$env:LOG_PATH='E:\JavaProject\gogorder\RuoYi-Vue\logs'

# 可选，门店和商品图片的本地保存目录
$env:UPLOAD_PATH='D:\gogorder\uploadPath'
```

然后运行：

```bash
mvn -pl ruoyi-admin -am spring-boot:run
```
