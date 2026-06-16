package com.ruoyi.common.exception;

/**
 * 业务异常（预期内的业务校验失败，如「库存不足」「余额不足」「订单状态不允许支付」）。
 *
 * <p>用法：业务层 {@code throw new ServiceException("xxx")} 或带 code {@code new ServiceException("余额不足", 400)}，
 * 可链式 {@code .setData(...)} 携带业务数据（如 {balance, required}）。
 * 由 {@code GlobalExceptionHandler} 统一捕获转成 AjaxResult 返回前端。
 *
 * <p><b>注意</b>：这是「正常业务流程」而非系统故障，全局异常处理器以 WARN 级别记录（不带堆栈），
 * 与真正的 RuntimeException/Exception（系统故障，ERROR + 堆栈 + 返回「系统繁忙」）区分。
 *
 * @author ruoyi
 */
public final class ServiceException extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     */
    private Integer code;

    /**
     * 错误提示
     */
    private String message;

    /**
     * 错误明细，内部调试错误
     *
     * 和 {@link CommonResult#getDetailMessage()} 一致的设计
     */
    private String detailMessage;

    /**
     * 可安全返回给客户端的业务数据
     */
    private Object data;

    /**
     * 空构造方法，避免反序列化问题
     */
    public ServiceException()
    {
    }

    public ServiceException(String message)
    {
        this.message = message;
    }

    public ServiceException(String message, Integer code)
    {
        this.message = message;
        this.code = code;
    }

    public String getDetailMessage()
    {
        return detailMessage;
    }

    @Override
    public String getMessage()
    {
        return message;
    }

    public Integer getCode()
    {
        return code;
    }

    public Object getData()
    {
        return data;
    }

    public ServiceException setMessage(String message)
    {
        this.message = message;
        return this;
    }

    public ServiceException setDetailMessage(String detailMessage)
    {
        this.detailMessage = detailMessage;
        return this;
    }

    public ServiceException setData(Object data)
    {
        this.data = data;
        return this;
    }
}
