package com.ruoyi.system.config; // 飞鹅云打印配置属性类所在包

import org.springframework.boot.context.properties.ConfigurationProperties; // 引入 Spring Boot 配置属性绑定注解，用于把 yml 配置映射到字段
import org.springframework.stereotype.Component; // 引入 Spring 组件注解，把本类注册为容器管理的 Bean

@Component // 声明为 Spring Bean，以便被飞鹅客户端（FeieClient）等组件注入使用
@ConfigurationProperties(prefix = "feie.config") // 绑定 application.yml 中 feie.config 前缀下的配置项到本类字段
public class FeieProperties // 飞鹅云打印开放平台的连接配置：账号、UKEY、接口地址
{
    private String user; // 飞鹅开放平台用户名（通过 feie.config.user 绑定，生产建议由环境变量 FEIE_USER 提供）
    private String uKey; // 飞鹅开放平台 UKEY 密钥（通过 feie.config.uKey 绑定，生产建议由环境变量 FEIE_UKEY 提供），用于生成接口签名
    private String url = "https://api.feieyun.cn/Api/Open/"; // 飞鹅开放平台统一接口入口地址，默认为官方地址

    public boolean isConfigured() // 判断飞鹅账号与密钥是否已正确配置（调用接口前的前置校验）
    {
        return user != null && !user.isBlank() && uKey != null && !uKey.isBlank(); // 用户名和 UKEY 均非 null 且非空白时才视为已配置
    }

    public String getUser() { return user; } // 获取飞鹅用户名
    public void setUser(String user) { this.user = user; } // 设置飞鹅用户名（由 Spring 配置绑定调用）
    public String getUKey() { return uKey; } // 获取飞鹅 UKEY
    public void setUKey(String uKey) { this.uKey = uKey; } // 设置飞鹅 UKEY（由 Spring 配置绑定调用）
    public String getUrl() { return url; } // 获取飞鹅接口地址
    public void setUrl(String url) { this.url = url; } // 设置飞鹅接口地址（一般保持官方默认值）
} // FeieProperties 类定义结束
