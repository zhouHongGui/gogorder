package com.ruoyi.system.mapper;

import com.ruoyi.system.domain.RefundLedger;

public interface RefundLedgerMapper
{
    int insert(RefundLedger ledger);
    RefundLedger selectByOrderId(Long orderId);
}
