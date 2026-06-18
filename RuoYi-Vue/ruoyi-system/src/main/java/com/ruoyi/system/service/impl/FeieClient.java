package com.ruoyi.system.service.impl; // 飞鹅 HTTP 客户端所在包

import java.io.BufferedReader; // 引入缓冲读取器，用于读取飞鹅接口响应流
import java.io.OutputStreamWriter; // 引入输出流写入器，用于向飞鹅接口写入表单请求体
import java.io.InputStreamReader; // 引入输入流读取器，用于把响应字节流按 UTF-8 解码为字符流
import java.net.URI; // 引入 URI，用于构造飞鹅接口地址
import java.net.URLConnection; // 引入 URL 连接，用于发起 HTTP 请求（飞鹅接口为 form 表单 POST）
import java.net.URLEncoder; // 引入 URL 编码工具，用于对表单参数做百分号编码
import java.nio.charset.StandardCharsets; // 引入标准字符集常量，统一使用 UTF-8 编解码
import java.security.MessageDigest; // 引入消息摘要工具，用于生成飞鹅签名（SHA-1）
import java.util.LinkedHashMap; // 引入有序哈希表，保证表单字段顺序稳定
import java.util.Map; // 引入 Map 类型，承载请求参数
import java.util.stream.Collectors; // 引入流收集器，用于把表单条目拼接为字符串
import org.slf4j.Logger; // 引入日志接口
import org.slf4j.LoggerFactory; // 引入日志工厂
import org.springframework.stereotype.Component; // 引入 Spring 组件注解，注册为 Bean
import com.alibaba.fastjson2.JSON; // 引入 fastjson2，用于把飞鹅响应 JSON 解析为对象
import com.ruoyi.common.exception.ServiceException; // 引入若依业务异常，调用失败时统一抛出
import com.ruoyi.system.config.FeieProperties; // 引入飞鹅配置属性
import com.ruoyi.system.domain.dto.FeieResponse; // 引入飞鹅响应对象

@Component // 声明为 Spring Bean，供 FeiePrintServiceImpl 等注入使用
public class FeieClient // 飞鹅云打印开放平台的底层 HTTP 客户端：负责签名、表单编码、请求与响应解析
{
    private static final Logger log = LoggerFactory.getLogger(FeieClient.class); // 日志记录器

    private final FeieProperties properties; // 飞鹅连接配置（账号、UKEY、接口地址）

    public FeieClient(FeieProperties properties) // 构造器注入飞鹅配置
    {
        this.properties = properties; // 保存配置引用
    }

    public FeieResponse post(String apiName, Map<String, String> params) // 统一的飞鹅接口调用入口：apiName 为接口名，params 为业务参数
    {
        if (!properties.isConfigured()) // 未配置账号或密钥时直接拒绝调用
        {
            throw new ServiceException("飞鹅打印配置未完成，请设置 FEIE_USER/FEIE_UKEY"); // 抛出业务异常，提示运维补全环境变量
        }
        String stime = String.valueOf(System.currentTimeMillis() / 1000); // 当前时间戳（秒），飞鹅签名和防重放依赖该值
        Map<String, String> form = new LinkedHashMap<>(); // 用有序 Map 组装请求表单，保证字段顺序固定
        form.put("user", properties.getUser()); // 飞鹅用户名
        form.put("stime", stime); // 时间戳
        form.put("sig", sha1Hex(properties.getUser() + properties.getUKey() + stime)); // 签名：SHA-1(user + ukey + stime) 小写十六进制
        form.put("apiname", apiName); // 接口名（如 Open_printLabelMsg）
        form.putAll(params); // 追加各接口的业务参数
        String body = encodeForm(form); // 将表单参数编码为 application/x-www-form-urlencoded 字符串
        String response = doPost(properties.getUrl(), body); // 发起 HTTP POST 并拿到响应文本
        if (response == null || response.isBlank()) // 飞鹅返回空响应
        {
            throw new ServiceException("飞鹅接口无响应"); // 抛出异常，避免后续解析空指针
        }
        try
        {
            return JSON.parseObject(response, FeieResponse.class); // 把响应 JSON 解析为 FeieResponse 对象
        }
        catch (Exception e) // 解析失败（返回非 JSON 或字段不符）
        {
            log.warn("飞鹅接口返回解析失败 apiName={} response={}", apiName, response); // 记录原始响应便于排查（不含敏感凭据）
            throw new ServiceException("飞鹅接口返回格式异常"); // 转换为业务异常
        }
    }

    private String doPost(String url, String body) // 执行一次 HTTP POST 请求并返回响应文本
    {
        try
        {
            URLConnection conn = URI.create(url).toURL().openConnection(); // 打开到飞鹅接口的连接
            conn.setConnectTimeout(5000); // 连接超时 5 秒
            conn.setReadTimeout(10000); // 读取超时 10 秒
            conn.setRequestProperty("accept", "*/*"); // 接收任意响应类型
            conn.setRequestProperty("connection", "Keep-Alive"); // 启用长连接
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8"); // 表单编码，UTF-8
            conn.setDoOutput(true); // 允许写出请求体
            conn.setDoInput(true); // 允许读取响应
            try (OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream(), StandardCharsets.UTF_8)) // 以 UTF-8 写出表单体
            {
                out.write(body); // 写入编码后的表单
                out.flush(); // 刷新输出流确保发出
            }
            StringBuilder result = new StringBuilder(); // 拼接响应文本
            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) // 以 UTF-8 读取响应
            {
                String line; // 当前行
                while ((line = in.readLine()) != null) // 逐行读取直到结束
                {
                    result.append(line); // 累加到结果（飞鹅响应通常为单行 JSON）
                }
            }
            return result.toString(); // 返回完整响应文本
        }
        catch (Exception e) // 网络/IO 异常
        {
            log.warn("飞鹅接口调用失败 url={}", url, e); // 记录失败堆栈（仅记录地址，不含密钥）
            throw new ServiceException("飞鹅接口调用失败，请稍后重试"); // 转换为业务异常，便于上层统一处理
        }
    }

    private String encodeForm(Map<String, String> form) // 把表单 Map 编码为 key=value&key=value 形式
    {
        return form.entrySet().stream() // 遍历所有表单条目
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue())) // 对键和值分别做 URL 编码并用 = 连接
                .collect(Collectors.joining("&")); // 用 & 拼接所有条目
    }

    private String encode(String value) // 对单个值做 URL 编码
    {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8); // null 视为空串，统一 UTF-8 编码
    }

    private String sha1Hex(String value) // 计算字符串的 SHA-1 摘要并输出小写十六进制
    {
        try
        {
            byte[] digest = MessageDigest.getInstance("SHA-1").digest(value.getBytes(StandardCharsets.UTF_8)); // 计算 SHA-1 字节数组
            StringBuilder result = new StringBuilder(digest.length * 2); // 预分配缓冲区，每个字节对应两个十六进制字符
            for (byte b : digest) // 遍历每个字节
            {
                result.append(String.format("%02x", b)); // 格式化为两位小写十六进制
            }
            return result.toString(); // 返回 40 位十六进制签名
        }
        catch (Exception e) // SHA-1 算法不可用（理论上不会发生）
        {
            throw new ServiceException("生成飞鹅签名失败"); // 转换为业务异常
        }
    }
} // FeieClient 类定义结束
