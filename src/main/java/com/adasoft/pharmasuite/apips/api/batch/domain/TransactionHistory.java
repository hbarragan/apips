package com.adasoft.pharmasuite.apips.api.batch.domain;

import com.adasoft.pharmasuite.apips.api.common.domain.BaseDomain;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.OffsetDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TransactionHistory extends BaseDomain implements Serializable {
    private Long key;
    @Schema(nullable = true)
    private OffsetDateTime time;
    private String type;
    private String subtype;
    private String orderStep;
    private String batchIdNew;
    private String batchIdOld;
    private String sublotIdNew;
    private String sublotIdOld;
}
