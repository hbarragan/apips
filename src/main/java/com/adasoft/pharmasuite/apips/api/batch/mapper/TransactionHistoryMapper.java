package com.adasoft.pharmasuite.apips.api.batch.mapper;

import com.adasoft.pharmasuite.apips.api.batch.domain.TransactionHistory;
import com.adasoft.pharmasuite.apips.api.common.domain.ErrorApi;
import com.adasoft.pharmasuite.apips.api.common.mapper.BaseMapper;
import com.adasoft.pharmasuite.apips.core.utils.LogManagement;
import com.rockwell.mes.services.inventory.ifc.ITransactionHistoryObject;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

public class TransactionHistoryMapper extends BaseMapper {

    public TransactionHistory toDto(ITransactionHistoryObject item) {
        TransactionHistory transactionHistory = new TransactionHistory();
        List<ErrorApi> errors = new ArrayList<>();
        try {
            transactionHistory =TransactionHistory.builder()
                    .key(item.getKey())
                    .time(getDateTime(item.getTimestamp()))
                    .type(item.getTransactionType() != null ? item.getTransactionType().toString() : null)
                    .subtype(item.getTransactionSubtype() != null ? item.getTransactionSubtype().toString() : null)
                    .orderStep(item.getOrderStepIdentifier())
                    .batchIdNew(item.getBatchIdentifierNew())
                    .batchIdOld(item.getBatchIdentifierOld())
                    .sublotIdNew(item.getSublotIdentifierNew())
                    .sublotIdOld(item.getSublotIdentifierOld())
                    .build();

        } catch (Exception e) {
            LogManagement.error(format("error mapping transaction history %s", e.getMessage()), this.getClass());
            errors.add(ErrorApi.builder().message(e.getMessage()).build());
        }
        transactionHistory.setErrors(errors);
        transactionHistory.setTimeServer(OffsetDateTime.now(ZoneOffset.UTC));

        return transactionHistory;
    }
}
