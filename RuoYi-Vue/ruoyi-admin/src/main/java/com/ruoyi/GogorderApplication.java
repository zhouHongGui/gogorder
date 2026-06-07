package com.ruoyi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;

/**
 * 启动程序
 * 
 * @author gogorder
 */
@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
public class GogorderApplication
{
    public static void main(String[] args)
    {
        // System.setProperty("spring.devtools.restart.enabled", "false");
        SpringApplication.run(GogorderApplication.class, args);
        System.out.println("gogorder 启动成功");
    }
}
