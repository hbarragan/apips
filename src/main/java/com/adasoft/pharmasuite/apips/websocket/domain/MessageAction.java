package com.adasoft.pharmasuite.apips.websocket.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MessageAction {

    @JsonProperty("action")
    private Action action;

    @JsonProperty("varName")
    private String varName;

    public boolean isAdd() {
        return action == Action.ADD;
    }
    public boolean isRemove() {
        return action == Action.REMOVE;
    }
}
