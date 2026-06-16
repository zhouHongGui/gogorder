package com.ruoyi.system.service;

import java.util.Map;
import com.ruoyi.system.domain.dto.SpecValidationResult;

/**
 * 规格校验与定价服务契约。实现见 {@link com.ruoyi.system.service.impl.SpecValidationServiceImpl}。
 */
public interface ISpecValidationService
{
    /** 校验规格合法性并由后端重算单价（不信任前端价格），返回校验结果与规格快照。 */
    SpecValidationResult validateAndPrice(Long shopId, Long productId, Map<String, Object> specs);
}
