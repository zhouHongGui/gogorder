package com.ruoyi.system.service;

import com.ruoyi.system.domain.dto.CCartAddRequest;
import com.ruoyi.system.domain.dto.CCartUpdateRequest;
import com.ruoyi.system.domain.dto.CCartView;

/**
 * C 端购物车服务契约（基于 Redis，每门店一车）。实现见 {@link com.ruoyi.system.service.impl.CCartServiceImpl}。
 */
public interface ICCartService
{
    /** 查询购物车（非空续期 TTL）。 */
    CCartView getCart(Long userId, Long shopId);

    /** 加入购物车（同商品同规格合并）。 */
    CCartView addItem(Long userId, CCartAddRequest request);

    /** 修改条目数量（quantity=0 删除）。 */
    CCartView updateItem(Long userId, CCartUpdateRequest request);

    /** 删除条目。 */
    CCartView removeItem(Long userId, Long shopId, String cartItemId);

    /** 清空购物车。 */
    CCartView clearCart(Long userId, Long shopId);
}
