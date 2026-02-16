package com.adasoft.pharmasuite.apips.api.recipe.mapper;

import com.adasoft.pharmasuite.apips.api.common.domain.ErrorApi;
import com.adasoft.pharmasuite.apips.api.common.domain.Material;
import com.adasoft.pharmasuite.apips.api.common.domain.MeasureValue;
import com.adasoft.pharmasuite.apips.api.common.mapper.BaseMapper;
import com.adasoft.pharmasuite.apips.api.recipe.domain.BomItems;
import com.adasoft.pharmasuite.apips.api.recipe.domain.Recipe;
import com.adasoft.pharmasuite.apips.core.constant.UdaConstant;
import com.adasoft.pharmasuite.apips.core.utils.LogManagement;
import com.datasweep.compatibility.client.AccessPrivilege;
import com.datasweep.compatibility.client.DatasweepException;
import com.datasweep.compatibility.client.MasterRecipe;
import com.datasweep.compatibility.client.MeasuredValue;
import com.datasweep.compatibility.client.ProcessBomItem;
import com.datasweep.compatibility.manager.ServerImpl;
import com.datasweep.compatibility.ui.Time;
import com.rockwell.mes.commons.base.ifc.functional.MeasuredValueUtilities;
import com.rockwell.mes.services.s88.impl.recipe.MESMasterRecipe;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;

public class RecipeMapper extends BaseMapper {
    Recipe recipe = new Recipe();
    List<ErrorApi> errors = new ArrayList<>();

    public Recipe toDto(MasterRecipe item, ServerImpl server) {
        if (item != null) {
            MESMasterRecipe mr = new MESMasterRecipe(item);
            List<ErrorApi> errors = new ArrayList<>();
            recipe = Recipe.builder()
                    .key(Optional.of(item.getKey()).orElse(0L))
                    .name(Optional.ofNullable(item.getName()).orElse(""))
                    .status(getState(item, errors))
                    .revision(Optional.ofNullable(item.getRevision()).orElse(""))
                    .creationDate(getDateTime(getCreationTime(item, errors)))
                    .quantity(item.getPart() != null ? buildMeasureValue(getXPlannedQuantity(item, errors), errors) : new MeasureValue())
                    .effectivityStartTime(item.getEffectivityStart() != null ? getDateTime(item.getEffectivityStart()) : null)
                    .effectivityEndTime(item.getEffectivityEnd() != null ? getDateTime(item.getEffectivityEnd()) : null)
                    .material(item.getPart() != null ? getMaterial(item.getPart(), errors) : new Material())
                    .procedure(buildProcedureName(mr, errors))
                    .accessPrivilege(getAccessPrivilegeName(getAccessPrivilegeKey(item, errors),server,errors))
                    .bomItemsList(buildBoomItemsList(item, errors))
                    .build();
        }
        recipe.setErrors(errors);
        recipe.setTimeServer(OffsetDateTime.now(ZoneOffset.UTC));
        return recipe;
    }


    private static Time getCreationTime(MasterRecipe masterRecipe, List<ErrorApi> errors) {
        try {
            return masterRecipe.getCreationTime();
        } catch (Exception e) {
            errors.add(ErrorApi.builder().message(e.getMessage()).build());
        }
        return null;
    }

    private static MeasuredValue getXPlannedQuantity(MasterRecipe masterRecipe, List<ErrorApi> errors) {
        try {
            return (MeasuredValue) masterRecipe.getUDA("X_plannedQuantity");
        } catch (DatasweepException e) {
            errors.add(ErrorApi.builder().description("getXPlannedQuantity").message(e.getMessage()).build());
            return MeasuredValueUtilities.createMV("");
        }
    }

    private static String getAccessPrivilegeKey(MasterRecipe masterRecipe, List<ErrorApi> errors) {
        try {

            Object accessPrivilege =  masterRecipe.getUDA(UdaConstant.ACCESS_PRIVILEGE);
            if(accessPrivilege instanceof AccessPrivilege){
                return String.valueOf(((AccessPrivilege) accessPrivilege).getKey());
            }
            return "";
        } catch (DatasweepException e) {
            errors.add(ErrorApi.builder().description("getAccessPrivilegeKey").message(e.getMessage()).build());
            return "";
        }
    }

    private List<BomItems> buildBoomItemsList(MasterRecipe masterRecipe, List<ErrorApi> errors) {

        List<BomItems> items = new java.util.ArrayList<>();
        if (masterRecipe != null &&
                masterRecipe.getProcessBOM() != null &&
                masterRecipe.getProcessBOM().getBomItems() != null) {
            for (Object boomItem : masterRecipe.getProcessBOM().getBomItems()) {
                if (boomItem instanceof ProcessBomItem bi) {
                    try {
                        items.add(BomItems.builder()
                                .quantity(buildMeasureValue(bi.getQuantity(), errors))
                                .materialIdentifier(bi.getPartNumber())
                                .materialDescription(bi.getPart() != null ? bi.getPart().getDescription() : "")
                                .position(String.valueOf(bi.getUDA("X_position")))
                                .build());
                    } catch (Exception e) {
                        errors.add(ErrorApi.builder().description("buildBoomItemsList").message(e.getMessage()).build());
                        LogManagement.info("## ERROR - buildBoomItemsList", this);
                    }
                }
            }
        }
        return items;
    }

    private static String buildProcedureName(MESMasterRecipe mr, List<ErrorApi> errors) {
        try {
            return mr.getProcedure().getProcedureName();
        } catch (Exception e) {
            errors.add(ErrorApi.builder().description("buildProcedureName").message(e.getMessage()).build());
            LogManagement.info("## ERROR - buildProcedureName", RecipeMapper.class);
            return "";
        }
    }


    private String getState(MasterRecipe item, List<ErrorApi> errors) {
        try {
            if (item.getCurrentStates() != null && !item.getCurrentStates().isEmpty()
                    && item.getCurrentStates().get(0) != null) {
                return safeString(item.getCurrentStates().get(0).toString());
            }
        } catch (Exception e) {
            LogManagement.error(format(ERROR_MAPPER, e.getMessage()), this.getClass());
            errors.add(ErrorApi.builder().description("getState").message(e.getMessage()).build());
        }
        return "";

    }
}
