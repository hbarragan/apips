package com.adasoft.pharmasuite.apips.api.order.mapper;

import com.adasoft.pharmasuite.apips.api.common.domain.ErrorApi;
import com.adasoft.pharmasuite.apips.api.common.mapper.BaseMapper;
import com.adasoft.pharmasuite.apips.api.order.domain.ProcessOrderDetail;
import com.adasoft.pharmasuite.apips.core.constant.ApiConstants;
import com.adasoft.pharmasuite.apips.core.utils.LogManagement;
import com.datasweep.compatibility.client.ControlRecipe;
import com.datasweep.compatibility.client.MeasuredValue;
import com.datasweep.compatibility.client.OrderStep;
import com.datasweep.compatibility.client.OrderStepOutput;
import com.datasweep.compatibility.client.ProcessOrderItem;
import com.datasweep.compatibility.client.Sublot;
import com.datasweep.compatibility.client.UnitOfMeasure;
import com.datasweep.plantops.common.measuredvalue.IMeasuredValue;
import com.rockwell.mes.commons.base.ifc.functional.MeasuredValueUtilities;
import com.rockwell.mes.commons.base.ifc.nameduda.MESNamedUDAPart;
import com.rockwell.mes.services.commons.ifc.order.IOrderStepOutputService;
import com.rockwell.mes.services.order.ifc.OrderUtils;
import com.rockwell.mes.services.s88.ifc.IS88ExecutionService;
import com.rockwell.mes.services.s88.ifc.IS88OrderAppendService;
import com.rockwell.mes.services.s88.ifc.execution.IMESRtOperation;
import com.rockwell.mes.services.s88.ifc.execution.IMESRtProcedure;
import com.rockwell.mes.services.s88.ifc.execution.IMESRtUnitProcedure;
import com.rockwell.mes.services.s88.ifc.recipe.IMESControlRecipe;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;

@SuppressWarnings("deprecation")
public class ProcessOrderDetailMapper extends BaseMapper {
    public ProcessOrderDetail toProcessOrderDetail(ProcessOrderItem item, IOrderStepOutputService orderStepOutputService, IS88OrderAppendService orderAppendService , IS88ExecutionService executionService) {
        ProcessOrderDetail po = new ProcessOrderDetail();
        List<ErrorApi> errors = new ArrayList<>();
        try {
            ControlRecipe controlRecipe = item.getControlRecipe();
            MeasuredValue mv = getConsumedQuantiy(item,errors,orderStepOutputService, controlRecipe);

            po = ProcessOrderDetail.builder()
                    .key(Optional.of(item.getKey()).orElse(0L))
                    .name(Optional.ofNullable(item.getName()).orElse(""))
                    //Acces bbdd
                    .consumedQuantity(buildMeasureValue(mv, errors))
                    .wfAssociated( getAssociatedWorkflows(item, errors, orderAppendService))
                    .workCenter(getWorkCenter(item, controlRecipe, errors))
                    .location(getLocation(controlRecipe, executionService, errors))
                    .build();

        } catch (Exception e) {
            LogManagement.error(format(ERROR_MAPPER, e.getMessage()), this);
            errors.add(ErrorApi.builder().message(e.getMessage()).build());
        }
        po.setErrors(errors);
        return po;
    }

    private String getWorkCenter(ProcessOrderItem poi, ControlRecipe controlRecipe, List<ErrorApi> errors ){
        String workCenter = "";
        try{
            if (controlRecipe != null && controlRecipe.getMasterRecipe() != null && controlRecipe.getMasterRecipe().getName() != null) {
                IMESControlRecipe imesControlRecipe = iS88ExecutionService.getControlRecipe(controlRecipe.getKey());

                List<IMESRtProcedure> allRtProcedures = imesControlRecipe.getAllRtProcedures();

                IMESRtProcedure imesRtProcedure = allRtProcedures.get(allRtProcedures.size() - 1);
                IMESRtUnitProcedure imesRtUnitProcedure = imesRtProcedure.getAllRtUnitProcedures().get(imesRtProcedure.getAllRtUnitProcedures().size() - 1);
                if (imesRtUnitProcedure.getWorkCenter() != null) {
                      workCenter = imesRtUnitProcedure.getWorkCenter().getName();
                }
            }
        } catch (Exception e) {
            LogManagement.error(format(ERROR_MAPPER_VALUE, format("buildWorkCenter not found %s", poi.getName()), e.getMessage()), ProcessOrderDetailMapper.class);
            errors.add(ErrorApi.builder().message(e.getMessage()).build());
        }
        return workCenter;
    }
    private List<String> getAssociatedWorkflows(ProcessOrderItem poi, List<ErrorApi> errors,IS88OrderAppendService orderAppendService){
        List<String> workflowAssociated = new ArrayList<>();

        try {
            if (poi != null && !OrderUtils.isWorkflowOrder(poi)) {
                List<ProcessOrderItem> list=orderAppendService.getAllAppendedOrders(poi);
                if(list!=null && !list.isEmpty()) {
                    for (ProcessOrderItem workflow : list) {
                        workflowAssociated.add(workflow.getName());
                    }
                }
            }
        }
        catch(Exception e){
            String error = format("getAssociatedWorkflows not found %s", poi.getName());
            LogManagement.error(format(ERROR_MAPPER_VALUE, error, e.getMessage()), ProcessOrderDetailMapper.class);
            errors.add(ErrorApi.builder().description(error).message(e.getMessage()).build());
        }
        return workflowAssociated;
    }
    private MeasuredValue getConsumedQuantiy(ProcessOrderItem poi, List<ErrorApi> errors, IOrderStepOutputService orderStepOutputService, ControlRecipe controlRecipe) {
        try {
            IMeasuredValue sublotQty = MeasuredValueUtilities.createZero();

            if (poi.getPart() != null) {
                UnitOfMeasure uom = MESNamedUDAPart.getUnitOfMeasure(poi.getPart());
                if (uom != null) {
                    sublotQty = MeasuredValueUtilities.createMV(BigDecimal.ZERO, uom);
                }
            }
            if (controlRecipe == null) {
                return (MeasuredValue ) sublotQty;
            }

            List<OrderStep> orderStepList = controlRecipe.getOrderSteps();
            if (orderStepList == null || orderStepList.isEmpty()) {
                return (MeasuredValue ) sublotQty;
            }


            for (OrderStep orderStep : orderStepList) {
                List<OrderStepOutput> outputs = orderStep.getOrderStepOutputItems();
                if (outputs == null || outputs.isEmpty()) {
                    continue;
                }
                for (OrderStepOutput oso : outputs) {
                    List<Sublot> sublots = orderStepOutputService.getProducedSublotsRefreshed(oso, true, false);
                    if (sublots == null || sublots.isEmpty()) {
                        continue;
                    }
                    for (Sublot sublot : sublots) {
                        IMeasuredValue consumed = sublot.getQuantity();
                        if (consumed != null) {
                            sublotQty = sublotQty.add(consumed);
                            LogManagement.info("getConsumedQuantiy " + poi.getName(), this);
                        }
                    }
                }
            }
            return  (MeasuredValue) sublotQty;
        } catch (Exception e) {
            String error = format("Error in getConsumedQuantiy for %s", poi.getName());
            LogManagement.error(format(ERROR_MAPPER_VALUE, error, e.getMessage()),
                    ProcessOrderDetailMapper.class);
            errors.add(ErrorApi.builder().description(error).message(e.getMessage()).build());
        }

        return null;
    }


    private String getLocation( ControlRecipe controlRecipe, IS88ExecutionService executionService, List<ErrorApi> errors) {
        try {
            if (controlRecipe != null && controlRecipe.getMasterRecipe() != null && controlRecipe.getMasterRecipe().getName() != null) {
                String result="";
                IMESControlRecipe imesControlRecipe= executionService.getControlRecipe(controlRecipe.getKey());

                if(imesControlRecipe!=null ) {
                    List<IMESRtProcedure> allRtProcedures = imesControlRecipe.getAllRtProcedures();

                    if (allRtProcedures != null && !allRtProcedures.isEmpty()) {
                        IMESRtProcedure imesRtProcedure = allRtProcedures.get(allRtProcedures.size() - 1);
                        result = result + imesRtProcedure.getProcedureName();

                        List<IMESRtUnitProcedure> allRtUnitProcedures = imesRtProcedure.getAllRtUnitProcedures();
                        if (allRtUnitProcedures != null && !allRtUnitProcedures.isEmpty()) {
                            IMESRtUnitProcedure imesRtUnitProcedure = allRtUnitProcedures.get(allRtUnitProcedures.size() - 1);
                            result = result + ApiConstants.SEPARATOR + imesRtUnitProcedure.getUnitProcedureName();

                            List<IMESRtOperation> allRtOperations = imesRtUnitProcedure.getAllRtOperations();
                            if (allRtOperations != null && !allRtOperations.isEmpty()) {
                                IMESRtOperation imesRtOperation = allRtOperations.get(allRtOperations.size() - 1);
                                result = result + ApiConstants.SEPARATOR + imesRtOperation.getOperationName();
                            }
                        }
                    }
                }
                return result;
            }
        } catch (Exception e) {
            LogManagement.error(format(ERROR_MAPPER_VALUE, format("getLocation not found %s", controlRecipe.getName()), e.getMessage()), ProcessOrderDetailMapper.class);
            errors.add(ErrorApi.builder().message(e.getMessage()).build());
        }
        return "";
    }
}
