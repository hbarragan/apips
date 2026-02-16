package com.adasoft.pharmasuite.apips.api.batch.domain;

import com.adasoft.pharmasuite.apips.api.common.domain.filter.BaseFilterDTO;
import lombok.Getter;
import lombok.Setter;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.List;

@Getter
@Setter
public class BatchFilterDTO extends BaseFilterDTO {

    @ToolParam(description = "Filter by state. [OUTPUT, INPUT ...]", required = false)
    private List<String> orderAssociateType;

    @ToolParam(description = "Filter by state. [PRODUCTION_OF_OUTPUT_MATERIAL, BATCH_GENERATION ...]", required = false)
    private List<String> orderAssociateSubtype;


    //TODO quitar OrderFilter y usar siempre DTO
    public static BatchFilter toBatchFilter(BatchFilterDTO dto) {
        if (dto == null) {
            return null;
        }

        BatchFilter filter = new BatchFilter();

        filter.setOrderAssociateType(dto.getOrderAssociateType());
        filter.setOrderAssociateSubtype(dto.getOrderAssociateSubtype());

        filter.setLastMonthsInterval(dto != null ? dto.getLastMonthsInterval() : null);
        filter.setInitCreationDate(dto != null ? dto.getInitCreationDate() : null);
        filter.setFinishCreationDate(dto != null ? dto.getFinishCreationDate() : null);

        return filter;
    }
}
