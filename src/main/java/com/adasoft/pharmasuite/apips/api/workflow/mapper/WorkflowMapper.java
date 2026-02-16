package com.adasoft.pharmasuite.apips.api.workflow.mapper;

import com.adasoft.pharmasuite.apips.api.common.domain.ErrorApi;
import com.adasoft.pharmasuite.apips.api.common.domain.MeasureValue;
import com.adasoft.pharmasuite.apips.api.common.mapper.BaseMapper;
import com.adasoft.pharmasuite.apips.api.workflow.domain.Workflow;
import com.adasoft.pharmasuite.apips.core.constant.UdaConstant;
import com.adasoft.pharmasuite.apips.core.utils.LogManagement;
import com.datasweep.compatibility.client.ControlRecipe;
import com.datasweep.compatibility.client.ProcessOrderItem;
import com.datasweep.compatibility.manager.ServerImpl;
import com.datasweep.compatibility.ui.Time;
import com.datasweep.plantops.common.dataobjects.DProcessOrderItem;
import com.datasweep.plantops.common.dataobjects.DUDAInstance;
import com.datasweep.plantops.common.dataobjects.DUDAInstanceItem;
import com.rockwell.mes.services.commons.ifc.order.ProcessOrderHelper;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.lang.String.format;

public class WorkflowMapper extends BaseMapper {
    public Workflow toWorkflow(ProcessOrderItem poi,ServerImpl server) {
        Workflow wf = Workflow.builder().build();
        List<ErrorApi> errors = new ArrayList<>();
        try {
            DUDAInstanceItem[] list = getListAttributes(poi, errors);
            wf = Workflow.builder()
                    .key(Optional.of(poi.getKey()).orElse(0L))
                    .name(Optional.ofNullable(poi.getName()).orElse(""))
                    .status(getState(poi, errors))
                    .masterWorkflowName(getWorkflowName(poi, errors))
                    .masterWorkflowDescription(getWorkflowDescription(poi, errors))
                    .creationDate(getDateTime(getCreationTime(poi, errors)))
                    .actualStart(getDateTime(getValueAttribute(UdaConstant.ACTUAL_START_DATE,list, errors)))
                    .actualFinish(getDateTime(getValueAttribute(UdaConstant.ACTUAL_FINISH_DATE,list,errors)))
                    .accessPrivilege(getAccessPrivilegeName(getValueAttribute(UdaConstant.PROCESSING_TYPE, list, errors),server,errors))
                    .upAssociated(List.of())
                    .productionRelevant(isWorkflowProductionRelevant(poi,errors))
                    .quantity(poi.getQuantity() != null ? buildMeasureValue(poi.getQuantity(), errors) : new MeasureValue())
                    .build();

        } catch (Exception e) {
            LogManagement.error(format(ERROR_MAPPER, e.getMessage()), this);
            errors.add(ErrorApi.builder().message(e.getMessage()).build());
        }
        wf.setErrors(errors);
        wf.setTimeServer(OffsetDateTime.now(ZoneOffset.UTC));
        return wf;
    }

    private static boolean isWorkflowProductionRelevant(ProcessOrderItem poi, List<ErrorApi> errors) {
        try {
            return ProcessOrderHelper.isWorkflowProductionRelevant(poi);
        }catch (Exception e) {
            errors.add(ErrorApi.builder().message(e.getMessage()).build());
        }
        return false;
    }

    private static Time getCreationTime(ProcessOrderItem poi, List<ErrorApi> errors) {
        try {
            return poi.getParent().getCreationTime();
        }catch (Exception e) {
            errors.add(ErrorApi.builder().message(e.getMessage()).build());
        }
        return null;
    }

    private String getWorkflowDescription(ProcessOrderItem poi, List<ErrorApi> errors) {
        try {
            return Objects.requireNonNull(getControlRecipe(poi, errors)).getMasterRecipe().getDescription();
        }catch (Exception e) {
            errors.add(ErrorApi.builder().description(format(ERROR_MAPPER_VALUE,this.getClass().getName(),"getWorkflowDescription")).message(e.getMessage()).build());
        }
        return "";
    }

    private String getWorkflowName(ProcessOrderItem poi, List<ErrorApi> errors) {
        try {
            return Objects.requireNonNull(getControlRecipe(poi, errors)).getMasterRecipe().getName();
        }catch (Exception e) {
            errors.add(ErrorApi.builder().description(format(ERROR_MAPPER_VALUE,this.getClass().getName(),"getWorkflowName")).message(e.getMessage()).build());
        }
        return "";
    }

    @SuppressWarnings("deprecation")
    private ControlRecipe getControlRecipe(ProcessOrderItem poi, List<ErrorApi> errors){
        try {
            return poi.getControlRecipe();
        }catch (Exception e) {
            errors.add(ErrorApi.builder().description(format(ERROR_MAPPER_VALUE,this.getClass().getName(),"getControlRecipe")).message(e.getMessage()).build());
        }
        return null;
    }

    private DUDAInstanceItem[] getListAttributes(ProcessOrderItem item, List<ErrorApi> errors) {
        try {
            DProcessOrderItem dataTransferObject = (DProcessOrderItem) item.getDataTransferObject();
            DUDAInstance dudaInstance = dataTransferObject.getUserDefinedAttributes();
            return dudaInstance.getDataItems();
        } catch (Exception e) {
            errors.add(ErrorApi.builder().description(format(ERROR_MAPPER_VALUE,this.getClass().getName(),"getListAttributes")).message(e.getMessage()).build());
        }
        return new DUDAInstanceItem[0];
    }

}
