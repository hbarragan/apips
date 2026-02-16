package com.adasoft.pharmasuite.apips.api.common.mapper;

import com.adasoft.pharmasuite.apips.api.common.domain.ErrorApi;
import com.adasoft.pharmasuite.apips.api.common.domain.Material;
import com.adasoft.pharmasuite.apips.api.common.domain.MeasureValue;
import com.adasoft.pharmasuite.apips.api.order.mapper.ProcessOrderMapper;
import com.adasoft.pharmasuite.apips.core.utils.LogManagement;
import com.datasweep.compatibility.client.AccessPrivilege;
import com.datasweep.compatibility.client.MeasuredValue;
import com.datasweep.compatibility.client.Part;
import com.datasweep.compatibility.client.ProcessOrderItem;
import com.datasweep.compatibility.manager.AccessPrivilegeManager;
import com.datasweep.compatibility.manager.ServerImpl;
import com.datasweep.compatibility.ui.Time;
import com.datasweep.plantops.common.dataobjects.DUDAInstanceItem;
import com.rockwell.mes.commons.base.ifc.services.ServiceFactory;
import com.rockwell.mes.services.s88.ifc.IS88ExecutionService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;

public class BaseMapper {

    public static final String ERROR_MAPPER = "Error mapper: %s";
    public static final String ERROR_MAPPER_VALUE = "Error mapper: %s %s";
    public static final String ERROR_MAPPER_UDA = "Error mapper search uda: %s";

    protected static final IS88ExecutionService iS88ExecutionService = ServiceFactory.getService(IS88ExecutionService.class);

    public static String safeString(String value) {
       try{
        return (value != null && !value.trim().isEmpty()) ? value : "";
       }catch (Exception e){
           return "";
       }
    }

//    protected LocalDateTime getDateTime(Time date) {
//        if (date != null) {
//            Calendar calendar = date.getCalendar();
//            return LocalDateTime.ofInstant(
//                    calendar.toInstant(),
//                    calendar.getTimeZone().toZoneId()
//            );
//        }
//        return null;
//    }

    protected OffsetDateTime getDateTime(Time date) {
        if (date == null) return null;
        Calendar cal = date.getCalendar();
        return OffsetDateTime.ofInstant(cal.toInstant(), ZoneOffset.UTC);
    }

    protected OffsetDateTime getDateTime(String date) {
        if (date == null || date.isEmpty()) return null;

        String[] parts = date.split(";");
        String dateTimePart = parts[0];
        try {
            return OffsetDateTime.parse(dateTimePart).withOffsetSameInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException ignore) {
            LocalDateTime ldt = LocalDateTime.parse(dateTimePart);
            return ldt.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime();
        }
    }

//    protected LocalDateTime getDateTime(String date) {
//        if (date != null && !date.isEmpty()) {
//            String[] parts = date.split(";");
//            String dateTimePart = parts[0];
//            OffsetDateTime offsetDateTime = getOffsetDateTime(dateTimePart);
//            return offsetDateTime.toLocalDateTime();
//        }
//        return null;
//    }

    protected String getValueAttribute(String name, DUDAInstanceItem[] list, List<ErrorApi> errors) {
        try {
            if (list != null && list.length >= 1) {
                for (DUDAInstanceItem item : list) {
                    if (item.getName().equals(name)) {
                        return item.getValue();
                    }
                }
            }
        } catch (Exception e) {
            errors.add(ErrorApi.builder().description(format("error getValueAttribute by %s",name)).message(e.getMessage()).build());
        }
        return "";
    }

    protected MeasureValue buildMeasureValue(MeasuredValue mv, List<ErrorApi> errors) {
        try {
            return MeasureValue.builder()
                    .unitOfMeasure(extractUnitFromMeasuredValue(mv))
                    .value(mv != null ? mv.getValue() : BigDecimal.valueOf(0))
                    .scale(mv != null ? mv.getScale() : 0)
                    .build();
        } catch (Exception e){
            errors.add(ErrorApi.builder().description("error buildMeasureValue").message(e.getMessage()).build());
        }
        return new MeasureValue();
    }

    private String extractUnitFromMeasuredValue(MeasuredValue quantity) {
        if (quantity == null) return "";
        try {
            return quantity.getSymbol();
        } catch (Exception e) {
            return "";
        }
    }

    public String getState(ProcessOrderItem poi, List<ErrorApi> errors){
        try {
            return getState(poi);
        } catch (Exception e) {
            LogManagement.error(format(ERROR_MAPPER, e.getMessage()), this.getClass());
            errors.add(ErrorApi.builder().message(e.getMessage()).build());
        }
        return "";
    }

    public static String getState(ProcessOrderItem poi){
        if (poi.getCurrentStates() != null && !poi.getCurrentStates().isEmpty()
                && poi.getCurrentState("orderStatus").getState().getName()  != null) {
            return poi.getCurrentState("orderStatus").getState().getName();
        }
        return "";
    }


    public static Material getMaterial(Part item, List<ErrorApi> errors) {
        try {
            return Optional.ofNullable(item).map(part ->
                    Material.builder()
                            .id(Optional.of(part.getKey()).orElse(0L))
                            .name(Optional.ofNullable(part.getPartNumber()).orElse(""))
                            .category(Optional.ofNullable(part.getCategory()).orElse(""))
                            .unitOfMeasure(getXUnitOfMeasure(item, errors))
                            .description(Optional.ofNullable(part.getDescription()).orElse(""))
                            .build()
            ).orElse(new Material());
        } catch (Exception e) {
            LogManagement.error(format(ERROR_MAPPER, e.getMessage()), BaseMapper.class);
            errors.add(ErrorApi.builder().description("error getMaterial").message(e.getMessage()).build());
        }
        return new Material();
    }

    private static String getXUnitOfMeasure(Part item, List<ErrorApi> errors)  {
        try {
        return Optional.ofNullable(item.getUDA("X_UnitOfMeasure").toString()).orElse("");
        } catch (Exception e) {
            LogManagement.error(format(ERROR_MAPPER, e.getMessage()), BaseMapper.class);
            errors.add(ErrorApi.builder().description("error X_UnitOfMeasure").message(e.getMessage()).build());
        }
        return "";
    }

    protected static String getAccessPrivilegeName(String key, ServerImpl server, List<ErrorApi> errors) {
        try {
            AccessPrivilegeManager accessPrivilegeManager = new AccessPrivilegeManager(server);
            if (key != null) {
                AccessPrivilege accessPrivilege=accessPrivilegeManager.getAccessPrivilegeFromDB(Long.parseLong(key));
                if(accessPrivilege!=null){
                    return accessPrivilege.getName();
                }
            }
        } catch (Exception e) {
            String error = format("getAccessPrivilegeName not found %s", key);
            LogManagement.error(format(ERROR_MAPPER_VALUE, error, e.getMessage()), ProcessOrderMapper.class);
            errors.add(ErrorApi.builder().description(error).message(e.getMessage()).build());
        }
        return "";
    }
}

