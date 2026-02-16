package com.adasoft.pharmasuite.apips.api.batch.mapper;

import com.adasoft.pharmasuite.apips.api.batch.domain.Batch;
import com.adasoft.pharmasuite.apips.api.common.domain.ErrorApi;
import com.adasoft.pharmasuite.apips.api.common.mapper.BaseMapper;
import com.adasoft.pharmasuite.apips.core.constant.UdaConstant;
import com.adasoft.pharmasuite.apips.core.utils.LogManagement;
import com.datasweep.compatibility.manager.ServerImpl;
import com.datasweep.plantops.common.dataobjects.DBatch;
import com.datasweep.plantops.common.dataobjects.DUDAInstanceItem;
import com.rockwell.mes.services.inventory.impl.SublotService;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;

public class BatchMapper extends BaseMapper {

    public Batch toDto(com.datasweep.compatibility.client.Batch item, SublotService sublotService, List<String> orderAssociateType, List<String> orderAssociateSubType, ServerImpl server) {
        DUDAInstanceItem[] list = null;
        List<ErrorApi> errors = new ArrayList<>();
        Batch batch = new Batch();
        try {
            list = getDudaInstanceItems(item, list, errors);
            batch = Batch.builder()
                    .key(Optional.of(item.getKey()).orElse(0L))
                    .name(Optional.ofNullable(item.getName()).orElse(""))
                    .status(getState(item, errors))
                    .material(item.getPart() != null ? getMaterial(item.getPart(), errors) : null)
                    .quantity(item.getQuantity() != null ? buildMeasureValue(item.getQuantity(), errors) : null)
                    .potency(item.getPotency() != null ? buildMeasureValue(item.getPotency(), errors) : null)
                    .totalConsumed(item.getTotalQuantityConsumed() != null ? buildMeasureValue(item.getTotalQuantityConsumed(), errors) : null)
                    .expiryDate(getDateTime(item.getExpirationTime()))
                    .retestDate(getDateTime(getValueAttribute(UdaConstant.RETEST_DATE, list, errors)))
                    .productionDate(getDateTime(getValueAttribute(UdaConstant.PRODUCTION_DATE, list, errors)))
                    .creationTime(getDateTime(item.getCreationTime()))
                    .transactionHistory(List.of())
                    .build();
        } catch (Exception e) {
            LogManagement.error(format(ERROR_MAPPER, e.getMessage()), this);
            errors.add(ErrorApi.builder().message(e.getMessage()).build());
        }
        batch.setErrors(errors);
        batch.setTimeServer(OffsetDateTime.now(ZoneOffset.UTC));
        return batch;
    }

    private DUDAInstanceItem[] getDudaInstanceItems(com.datasweep.compatibility.client.Batch batchps, DUDAInstanceItem[] list, List<ErrorApi> errors) {
        try {
            list = ((DBatch) batchps.getDataTransferObject()).getUserDefinedAttributes().getDataItems();
        } catch (Exception e) {
            LogManagement.error(e.getMessage(), this);
            errors.add(ErrorApi.builder().description("error getDudaInstanceItems").message(e.getMessage()).build());
        }
        return list;
    }

    private String getState(com.datasweep.compatibility.client.Batch item, List<ErrorApi> errors) {
        try {
            if (item.getCurrentStates() != null && !item.getCurrentStates().isEmpty()
                    && item.getCurrentStates().get(0) != null) {
                return item.getCurrentStates().get(0).toString();
            }
        } catch (Exception e) {
            errors.add(ErrorApi.builder().description("getState").message(e.getMessage()).build());
        }
        return "";
    }
}
