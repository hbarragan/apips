package com.adasoft.pharmasuite.apips.websocket.domain;

import com.fasterxml.jackson.annotation.JsonRawValue;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MessageResponse {

    String varName;
    @JsonRawValue
    Object data;
}
