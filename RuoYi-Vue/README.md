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

完成 MySQL 与 Redis 配置后运行：

```bash
mvn -pl ruoyi-admin -am spring-boot:run
```
