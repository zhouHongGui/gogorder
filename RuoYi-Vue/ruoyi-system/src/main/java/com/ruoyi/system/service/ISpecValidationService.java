package com.ruoyi.system.service;

import java.util.Map;
import com.ruoyi.system.domain.dto.SpecValidationResult;

public interface ISpecValidationService
{
    SpecValidationResult validateAndPrice(Long shopId, Long productId, Map<String, Object> specs);
}
