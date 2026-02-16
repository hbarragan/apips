package com.adasoft.pharmasuite.apips.health.domain;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class HealthWebsocketResponse {
    private SubscriptionsData subscriptionsData;
    private List<Map<String, Object>> jobs;
}
