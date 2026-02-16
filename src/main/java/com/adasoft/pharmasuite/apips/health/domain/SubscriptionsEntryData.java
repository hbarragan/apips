package com.adasoft.pharmasuite.apips.health.domain;

import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
public class SubscriptionsEntryData extends DataEntry {
    public List<String> sessions;
    public Map<String, List<String>> subsByMsg;
    public Map<String, List<String>> subsBySession;

    public SubscriptionsEntryData(
            List<String> sessions,
            Map<String, List<String>> subsByMsg,
            Map<String, List<String>> subsBySession
    ) {
        super();
        this.sessions = sessions;
        this.subsByMsg = subsByMsg;
        this.subsBySession = subsBySession;
    }

}

