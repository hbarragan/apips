package com.adasoft.pharmasuite.apips.cache.service.impl;


import com.adasoft.pharmasuite.apips.cache.domain.CachedObject;
import com.adasoft.pharmasuite.apips.cache.service.MemoryCacheService;
import com.adasoft.pharmasuite.apips.core.utils.LogManagement;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MemoryCacheServiceImpl implements MemoryCacheService {

    private final Map<String, CachedObject<Object>> cache = new ConcurrentHashMap<>();

    private final MessageDigest digest;

    public MemoryCacheServiceImpl(MessageDigest digest) {
        this.digest = digest;
    }

    public boolean isValid(String key, long milliseconds, Object classname) {
        CachedObject<Object> cached = cache.get(key);
        if (cached == null || milliseconds == 0) return false;

        long age = Duration.between(cached.getTimestamp(), Instant.now()).toMillis();
        boolean valid = age <= milliseconds;

        if (!valid) {
            LogManagement.info("Expired memory cache", classname);
        } else {
            LogManagement.info("Valid memory cache", classname);
        }

        return valid;
    }



    public Object get(String key) {
        CachedObject<Object> cached = cache.get(key);
        return cached != null ? cached.getData() : null;
    }

    public void put(String key, Object value, long timeCache) {
        cache.put(key, new CachedObject<>(value,timeCache));
    }

    public void delete(String key) {
        cache.remove(key);
    }

    public String generateCacheKey(String url, String query, Object classname) {
        if (query != null) {
            url += "?" + query;
        }
        byte[] hash = digest.digest(url.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    }
}


