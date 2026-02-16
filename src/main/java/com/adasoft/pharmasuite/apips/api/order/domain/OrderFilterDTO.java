package com.adasoft.pharmasuite.apips.api.order.domain;
import java.util.List;

import com.adasoft.pharmasuite.apips.api.common.domain.filter.BaseFilterCache;
import com.adasoft.pharmasuite.apips.api.common.domain.filter.BaseFilterDTO;
import com.adasoft.pharmasuite.apips.api.recipe.domain.RecipeFilter;
import lombok.Getter;
import lombok.Setter;
import org.springframework.ai.tool.annotation.ToolParam;

@Getter
@Setter
public class OrderFilterDTO extends BaseFilterDTO {

    @ToolParam(
            description = "Name or partial name used to filter orders. " +
                    "If not specified, no filtering by name will be applied.",
            required = false
    )
    private String name;

    @ToolParam(
            description = "List of order status values used to filter results. " +
                    "Only orders whose status matches one of the provided values will be returned. " +
                    "If not specified, all statuses will be included.",
            required = false
    )
    private List<String> status;

    @ToolParam(
            description = "ERP start date (inclusive) used to filter orders by date range. " +
                    "Recommended format: ISO 8601 (e.g. '2025-01-01T00:00:00Z'). " +
                    "If not specified, no lower ERP date limit will be applied.",
            required = false
    )
    private String erpStartDate;

    @ToolParam(
            description = "ERP end date (inclusive) used to filter orders by date range. " +
                    "Recommended format: ISO 8601 (e.g. '2025-01-31T23:59:59Z'). " +
                    "If not specified, no upper ERP date limit will be applied.",
            required = false
    )
    private String erpFinishDate;

    @ToolParam(
            description = "Access privilege required to retrieve orders. " +
                    "Used to validate that the caller has sufficient permissions before returning data. " +
                    "If not specified, default access control rules will be applied.",
            required = false
    )
    private String accessPrivilege;

    @ToolParam(
            description = "Indicates whether extended order details should be retrieved. " +
                    "If set to true, additional fields such as consumedQuantity, workCenter, " +
                    "location and consumed quantities will be included in the response. " +
                    "Enabling this option may increase response time, as additional queries " +
                    "are executed per record. It is recommended to use true only when detailed " +
                    "information is strictly required.",
            required = false
    )
    private boolean all = false;

    //TODO quitar OrderFilter y usar siempre DTO
    public static OrderFilter toOrderFilter(OrderFilterDTO dto) {
        if (dto == null) {
            return null;
        }

        OrderFilter orderFilter = new OrderFilter();

        // Mapeo explícito (sin checks innecesarios)
        orderFilter.setName(dto.getName());
        orderFilter.setStatus(dto.getStatus());
        orderFilter.setErpStartDate(dto.getErpStartDate());
        orderFilter.setErpFinishDate(dto.getErpFinishDate());
        orderFilter.setAccessPrivilege(dto.getAccessPrivilege());
        orderFilter.setAll(dto.isAll());


        orderFilter.setLastMonthsInterval(dto != null ? dto.getLastMonthsInterval() : null);
        orderFilter.setInitCreationDate(dto != null ? dto.getInitCreationDate() : null);
        orderFilter.setFinishCreationDate(dto != null ? dto.getFinishCreationDate() : null);

        return orderFilter;
    }
}
