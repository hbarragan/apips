package com.adasoft.pharmasuite.apips.health.domain;

import java.util.List;
import java.util.Map;

public class SubscriptionsData {
       public List<String> sessions;
       public Map<String, List<String>> subsByMsg;
       public Map<String, List<String>> subsBySession;

    public SubscriptionsData(
            List<String> sessions,
            Map<String, List<String>> subsByMsg,
            Map<String, List<String>> subsBySession
    ) {
        this.sessions = sessions;
        this.subsByMsg = subsByMsg;
        this.subsBySession = subsBySession;
    }
}
