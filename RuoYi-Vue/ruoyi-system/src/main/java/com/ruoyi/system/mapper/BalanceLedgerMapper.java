package com.ruoyi.system.mapper;

import com.ruoyi.system.domain.BalanceLedger;

public interface BalanceLedgerMapper
{
    int insert(BalanceLedger ledger);
    BalanceLedger selectByIdempotentKey(String idempotentKey);
}
