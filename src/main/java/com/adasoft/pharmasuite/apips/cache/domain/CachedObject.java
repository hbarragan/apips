package com.adasoft.pharmasuite.apips.cache.domain;

import com.adasoft.pharmasuite.apips.core.utils.LogManagement;
import lombok.Data;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Data
public class CachedObject<T> {
    private final T data;
    private final Instant timestamp;

    public CachedObject(T data, long timeCache) {
        this.data = data;
        this.timestamp = Instant.now().plusMillis(timeCache);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss.SSS")
                .withZone(ZoneId.systemDefault());
        LogManagement.info("Cache expirara a la hora :"+fmt.format(timestamp),this);
    }
}
