package com.adasoft.pharmasuite.apips.api.common.domain.filter;

import com.adasoft.pharmasuite.apips.api.common.domain.HelperSchema;
import com.adasoft.pharmasuite.apips.api.order.domain.OrderFilter;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseFilterCache extends BaseFilter {

    @Schema(
            description = "Variable to subscribe, when send var create and update socket'",
            example = "order_27.oee"
    )
    @Parameter()
    private String varSubscribe;

    @Schema(
            description = "Time job quartz for update data websocket, default 20000ms'",
            example = "20000"
    )
    @Parameter()
    private Long timeQuartz;


    @Schema(description = "Time cache, default 1m in application.yml.")
    @Parameter()
    private Long timeCache;


    @Schema(description = "Enable cache, default false", example = "false")
    @Parameter()
    private boolean enableCache;

    @Schema(hidden = true)
    private transient String requestUri;
    @Schema(hidden = true)
    private transient String queryString;
    @Schema(hidden = true)
    private transient String fullUrl;

    public void setRequestInfo(HttpServletRequest request) {
        if (request != null) {
            this.requestUri = request.getRequestURI();
            this.queryString = request.getQueryString();
            this.fullUrl = request.getRequestURL() != null ? request.getRequestURL().toString() : "";
        }
    }


    public String getCacheKey() {
        if (requestUri == null) {
            return null;
        }
        return requestUri + (queryString != null ? "?" + queryString : "");
    }

    protected static Map<String, Object> buildBaseMapSchema(Map<String, Object> map) {
        map.put("enableCache", false);
        return BaseFilter.buildBaseMapSchema(map);
    }

    public Map<String, Object> getSchemaSwagger(Collection<String> ignoreFields) {
        return HelperSchema.minimalInputSchema(
                BaseFilter.class,
                buildBaseMapSchema(new HashMap<>()),
                ignoreFields);
    }

}
