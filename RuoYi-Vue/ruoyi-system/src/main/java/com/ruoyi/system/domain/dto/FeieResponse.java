package com.ruoyi.system.domain.dto; // 飞鹅接口响应 DTO 所在包

public class FeieResponse // 飞鹅开放平台接口统一返回体的映射对象
{
    private Integer ret; // 飞鹅业务返回码：0 表示成功，非 0 表示失败
    private String msg; // 飞鹅返回的提示信息（成功或错误描述）
    private Object data; // 飞鹅返回的业务数据，不同接口结构不同，因此统一用 Object 接收
    private Long serverExecutedTime; // 飞鹅服务端执行耗时（毫秒），仅用于排查，业务上不依赖

    public boolean isSuccess() // 判断本次飞鹅调用是否成功
    {
        return Integer.valueOf(0).equals(ret); // 返回码 ret 为 0 时视为成功（用 equals 避免 ret 为 null 时的空指针）
    }

    public Integer getRet() { return ret; } // 获取返回码
    public void setRet(Integer ret) { this.ret = ret; } // 设置返回码
    public String getMsg() { return msg; } // 获取提示信息
    public void setMsg(String msg) { this.msg = msg; } // 设置提示信息
    public Object getData() { return data; } // 获取业务数据
    public void setData(Object data) { this.data = data; } // 设置业务数据
    public Long getServerExecutedTime() { return serverExecutedTime; } // 获取服务端执行耗时
    public void setServerExecutedTime(Long serverExecutedTime) { this.serverExecutedTime = serverExecutedTime; } // 设置服务端执行耗时
} // FeieResponse 类定义结束
