package com.adasoft.pharmasuite.apips.api.dashboard.domain;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "dashboard.states")
@Getter
@Setter
public class DashboardStatesProperties {
    private List<String> finished;
    private List<String> running;
}
