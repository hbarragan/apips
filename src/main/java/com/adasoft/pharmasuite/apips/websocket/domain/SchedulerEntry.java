package com.adasoft.pharmasuite.apips.websocket.domain;


import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchedulerEntry {
    private Long time;
    private String uri;
    private String queryString;
}