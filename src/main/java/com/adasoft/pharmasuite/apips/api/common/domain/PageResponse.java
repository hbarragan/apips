package com.adasoft.pharmasuite.apips.api.common.domain;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Generic paged response wrapper")
public record PageResponse<T>(
        @Schema(description = "List of elements in current page")
        List<T> content,
        @Schema(description = "Current page number", example = "1")
        int page,
        @Schema(description = "Number of elements per page", example = "20")
        int size,
        @Schema(description = "Total number of elements across all pages", example = "100")
        long totalElements
){}
