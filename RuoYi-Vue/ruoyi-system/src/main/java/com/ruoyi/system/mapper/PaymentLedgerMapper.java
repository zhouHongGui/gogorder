package com.ruoyi.system.mapper;

import com.ruoyi.system.domain.PaymentLedger;

public interface PaymentLedgerMapper
{
    int insert(PaymentLedger ledger);
    PaymentLedger selectByIdempotentKey(String idempotentKey);
}
