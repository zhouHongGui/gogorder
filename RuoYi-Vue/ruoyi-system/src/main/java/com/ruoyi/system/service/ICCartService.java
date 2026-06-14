package com.ruoyi.system.service;

import com.ruoyi.system.domain.dto.CCartAddRequest;
import com.ruoyi.system.domain.dto.CCartUpdateRequest;
import com.ruoyi.system.domain.dto.CCartView;

public interface ICCartService
{
    CCartView getCart(Long userId, Long shopId);

    CCartView addItem(Long userId, CCartAddRequest request);

    CCartView updateItem(Long userId, CCartUpdateRequest request);

    CCartView removeItem(Long userId, Long shopId, String cartItemId);

    CCartView clearCart(Long userId, Long shopId);
}
