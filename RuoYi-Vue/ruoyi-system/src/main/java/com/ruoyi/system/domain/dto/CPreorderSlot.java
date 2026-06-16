package com.ruoyi.system.domain.dto;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 预订单可选取餐时段（每 30 分钟一档，已过滤出营业时段内的）。
 * 下单时把 value 传回后端作为 scheduledPickupTime。
 */
public class CPreorderSlot
{
    /** 时段值（下单时回传，格式 yyyy-MM-dd HH:mm）。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime value;

    /** 日期展示文案（今天/明天/M月D日）。 */
    private String dateLabel;
    /** 时间展示文案（HH:mm）。 */
    private String timeLabel;

    public CPreorderSlot(LocalDateTime value, String dateLabel, String timeLabel)
    {
        this.value = value;
        this.dateLabel = dateLabel;
        this.timeLabel = timeLabel;
    }

    public LocalDateTime getValue() { return value; }
    public String getDateLabel() { return dateLabel; }
    public String getTimeLabel() { return timeLabel; }
}
