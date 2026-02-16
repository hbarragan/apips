package com.adasoft.pharmasuite.apips.api.order.domain;

import com.adasoft.pharmasuite.apips.api.common.domain.BaseDomain;
import com.adasoft.pharmasuite.apips.api.common.domain.MeasureValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;


@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Process order data detail")
public class ProcessOrderDetail extends BaseDomain implements Serializable {
    private long key;
    private String name;
    private MeasureValue consumedQuantity;
    private List<String> wfAssociated;
    private String workCenter;
    private String location;
}
