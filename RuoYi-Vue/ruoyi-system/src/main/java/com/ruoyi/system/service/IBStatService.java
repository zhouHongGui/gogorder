package com.ruoyi.system.service;

import com.ruoyi.system.domain.dto.BStatOverviewView;

public interface IBStatService
{
    BStatOverviewView getOverview(Long shopId, String range);
}
