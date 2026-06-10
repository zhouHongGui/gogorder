package com.ruoyi.system.domain.dto;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * C端预订单可选取餐时间。
 */
public class CPreorderSlot
{
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime value;

    private String dateLabel;
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
