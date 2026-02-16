package com.adasoft.pharmasuite.apips.api.order.mapper;

import com.adasoft.pharmasuite.apips.api.common.domain.ErrorApi;
import com.adasoft.pharmasuite.apips.api.common.domain.Material;
import com.adasoft.pharmasuite.apips.api.common.domain.MeasureValue;
import com.adasoft.pharmasuite.apips.api.common.mapper.BaseMapper;
import com.adasoft.pharmasuite.apips.api.order.domain.ProcessOrder;
import com.adasoft.pharmasuite.apips.core.constant.ApiConstants;
import com.adasoft.pharmasuite.apips.core.constant.UdaConstant;
import com.adasoft.pharmasuite.apips.core.utils.LogManagement;
import com.datasweep.compatibility.client.Batch;
import com.datasweep.compatibility.client.ControlRecipe;
import com.datasweep.compatibility.client.MeasuredValue;
import com.datasweep.compatibility.client.OrderStep;
import com.datasweep.compatibility.client.OrderStepOutput;
import com.datasweep.compatibility.client.ProcessOrderItem;
import com.datasweep.compatibility.client.Sublot;
import com.datasweep.compatibility.client.UnitOfMeasure;
import com.datasweep.compatibility.manager.ServerImpl;
import com.datasweep.compatibility.ui.Time;
import com.datasweep.plantops.common.dataobjects.DProcessOrderItem;
import com.datasweep.plantops.common.dataobjects.DUDAInstance;
import com.datasweep.plantops.common.dataobjects.DUDAInstanceItem;
import com.datasweep.plantops.common.measuredvalue.IMeasuredValue;
import com.rockwell.mes.commons.base.ifc.functional.MeasuredValueUtilities;
import com.rockwell.mes.commons.base.ifc.nameduda.MESNamedUDAPart;
import com.rockwell.mes.services.commons.ifc.order.IOrderStepOutputService;
import com.rockwell.mes.services.commons.ifc.order.ProcessOrderHelper;
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
public class ProcessOrderMapper extends BaseMapper {
    public ProcessOrder toProcessOrder(ProcessOrderItem item, ServerImpl server, IOrderStepOutputService orderStepOutputService, IS88OrderAppendService orderAppendService , IS88ExecutionService executionService, boolean allFilter) {
        ProcessOrder po = new ProcessOrder();
        List<ErrorApi> errors = new ArrayList<>();
        try {
            String batchName  = getBatchName(item, errors);
            DUDAInstanceItem[] list = getListAttributes(item, po);
            ControlRecipe controlRecipe = item.getControlRecipe();
            MeasuredValue mv = allFilter ? getConsumedQuantiy(item,errors,orderStepOutputService, controlRecipe) : null;

            po = ProcessOrder.builder()
                    .key(Optional.of(item.getKey()).orElse(0L))
                    .name(Optional.ofNullable(item.getName()).orElse(""))
                    .material(item.getPart()!=null?getMaterial(item.getPart(), errors):new Material())
                    .quantity(item.getQuantity() != null ? buildMeasureValue(item.getQuantity(),errors) : new MeasureValue())
                    .quantityFinal(getValueAttribute("X_actualQuantity", list, errors))
                    .status(getState(item, errors))
                    .creationDate(getDateTime(getCreationDate(item, errors)))
                    .plannedStart(getDateTime(getValueAttribute("X_plannedStartDate", list, errors)))
                    .plannedFinish(getDateTime(getValueAttribute("X_plannedFinishDate", list, errors)))
                    .actualStart(getDateTime(getValueAttribute("X_actualStartDate", list, errors)))
                    .actualFinish(getDateTime(getValueAttribute("X_actualFinishDate", list, errors)))
                    .erpFinishDate(getDateTime(getValueAttribute("X_erpFinishDate", list, errors)))
                    .erpStartDate(getDateTime(getValueAttribute("X_erpStartDate", list, errors)))
                    .accessPrivilege(getAccessPrivilegeName(getValueAttribute(UdaConstant.ACCESS_PRIVILEGE, list, errors),server,errors))
                    .targetBatch(batchName!=null?batchName:getValueAttribute("X_batch", list, errors))
                    .batchName(batchName)

                    .recipeName(buildRecipeName(controlRecipe, errors))
                    .recipeDescription(buildRecipeDescription(controlRecipe, errors))

                    //Acces bbdd
                    .consumedQuantity(buildMeasureValue(mv, errors))
                    .wfAssociated(allFilter ? getAssociatedWorkflows(item, errors, orderAppendService) : null)
                    .workCenter(allFilter ?getWorkCenter(item, controlRecipe, errors) : null)
                    .location(allFilter ? getLocation(controlRecipe, executionService, errors): null)
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
            LogManagement.error(format(ERROR_MAPPER_VALUE, format("buildWorkCenter not found %s", poi.getName()), e.getMessage()), ProcessOrderMapper.class);
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
            LogManagement.error(format(ERROR_MAPPER_VALUE, error, e.getMessage()), ProcessOrderMapper.class);
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
                    ProcessOrderMapper.class);
            errors.add(ErrorApi.builder().description(error).message(e.getMessage()).build());
        }

        return null;
    }
    private Time getCreationDate(ProcessOrderItem poi, List<ErrorApi> errors) {
        try {
            return poi.getParent().getCreationTime();
        } catch (Exception e) {
            LogManagement.error(format(ERROR_MAPPER, e.getMessage()), ProcessOrderMapper.class);
            errors.add(ErrorApi.builder().message(e.getMessage()).build());
        }
        return null;
    }

    private static String getBatchName(ProcessOrderItem poi, List<ErrorApi> errors) {
        try {
            Batch batch = ProcessOrderHelper.getBatch(poi);
            if (batch != null) {
                return batch.getName();
            }
        } catch (Exception e) {
            String error = format("getBatchName not found %s", poi.getName());
            LogManagement.error(format(ERROR_MAPPER_VALUE, error, e.getMessage()), ProcessOrderMapper.class);
            errors.add(ErrorApi.builder().description(error).message(e.getMessage()).build());
        }
        return "";
    }

    private static String buildRecipeName(ControlRecipe controlRecipe, List<ErrorApi> errors) {
        try {

            if (controlRecipe != null && controlRecipe.getMasterRecipe() != null && controlRecipe.getMasterRecipe().getName() != null) {
                return controlRecipe.getMasterRecipe().getName();
            }
        } catch (Exception e) {
            String error = format("buildRecipeName not found %s", controlRecipe.getName());
            LogManagement.error(format(ERROR_MAPPER_VALUE, error, e.getMessage()), ProcessOrderMapper.class);
            errors.add(ErrorApi.builder().description(error).message(e.getMessage()).build());
        }
        return "";
    }


    private static String buildRecipeDescription(ControlRecipe controlRecipe, List<ErrorApi> errors) {
        try {
            if (controlRecipe != null && controlRecipe.getMasterRecipe() != null && controlRecipe.getMasterRecipe().getName() != null && controlRecipe.getMasterRecipe().getDescription()!=null) {
                return controlRecipe.getMasterRecipe().getDescription();
            }
        } catch (Exception e) {
            LogManagement.error(format(ERROR_MAPPER_VALUE, format("buildRecipeDescription not found %s", controlRecipe.getName()), e.getMessage()), ProcessOrderMapper.class);
            errors.add(ErrorApi.builder().message(e.getMessage()).build());
        }
        return "";
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
            LogManagement.error(format(ERROR_MAPPER_VALUE, format("getLocation not found %s", controlRecipe.getName()), e.getMessage()), ProcessOrderMapper.class);
            errors.add(ErrorApi.builder().message(e.getMessage()).build());
        }
        return "";
    }

    private DUDAInstanceItem[] getListAttributes(ProcessOrderItem item, ProcessOrder po) {
        try {

            Object obj = item.getDataTransferObject();
            if (obj instanceof DProcessOrderItem dto) {
                DProcessOrderItem dataTransferObject = (DProcessOrderItem) item.getDataTransferObject();
                DUDAInstance dudaInstance = dataTransferObject.getUserDefinedAttributes();
                return dudaInstance.getDataItems();
            }else{
                return null;
            }
        } catch (Exception e) {
            LogManagement.error(format(ERROR_MAPPER_UDA, e.getMessage()), ProcessOrderMapper.class);
            po.addError(ErrorApi.builder().message(e.getMessage()).build());
        }
        return new DUDAInstanceItem[0];
    }
}
