package com.adasoft.pharmasuite.apips.cache.service;

public interface MemoryCacheService {
    boolean isValid(String key, long minutes, Object classname);
    Object get(String key);
    void put(String key, Object value, long timeCache);
    String generateCacheKey(String url, String query, Object classname);
    void delete(String key);
}