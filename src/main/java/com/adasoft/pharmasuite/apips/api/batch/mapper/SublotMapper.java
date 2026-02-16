package com.adasoft.pharmasuite.apips.api.batch.mapper;

import com.adasoft.pharmasuite.apips.api.batch.domain.Sublot;
import com.adasoft.pharmasuite.apips.api.common.domain.ErrorApi;
import com.adasoft.pharmasuite.apips.api.common.domain.Material;
import com.adasoft.pharmasuite.apips.api.common.mapper.BaseMapper;
import com.adasoft.pharmasuite.apips.core.constant.UdaConstant;
import com.adasoft.pharmasuite.apips.core.utils.LogManagement;
import com.datasweep.plantops.common.dataobjects.DSublot;
import com.datasweep.plantops.common.dataobjects.DUDAInstanceItem;
import com.rockwell.mes.services.inventory.impl.SublotService;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

public class SublotMapper extends BaseMapper {

    public Sublot toDto(com.datasweep.compatibility.client.Sublot item, SublotService sublotService) {
        DUDAInstanceItem[] list = null;
        List<ErrorApi> errors = new ArrayList<>();
        Sublot sublot = new Sublot();

        try {
            list = ((DSublot) item.getDataTransferObject()).getUserDefinedAttributes().getDataItems();
            sublot =Sublot.builder()
                    .name(item.getName())
                    .key(item.getKey())
                    .quantity(buildMeasureValue(item.getQuantity(), errors))
                    .quantityConsumed(buildMeasureValue(item.getQuantityConsumed(), errors))
                    .material(item.getPart() != null ? getMaterial(item.getPart(), errors) : new Material())
                    .batchIdentifier(item.getBatchName())
                    .productionOrderStep(String.valueOf(item.getUDA("X_producedInOrderStep")))
                    .productionDate(list != null ? getDateTime(getValueAttribute(UdaConstant.PRODUCTION_DATE, list, errors)) : null)
                    .storageLocation(sublotService.getStorageLocation(item).getLocation())
                    .storageArea(sublotService.getStorageLocation(item).getParentLocation().getLocation())
                    .warehouse(sublotService.getStorageLocation(item).getParentLocation().getParentLocation().getLocation())
                    .tare(String.valueOf(item.getUDA("X_tare")))
                    .build();
        } catch (Exception e) {
            LogManagement.error(format("error mapping sublot %s", e.getMessage()), this.getClass());
            errors.add(ErrorApi.builder().message(e.getMessage()).build());
        }
        sublot.setErrors(errors);
        sublot.setTimeServer(OffsetDateTime.now(ZoneOffset.UTC));

        return sublot;
    }
}
