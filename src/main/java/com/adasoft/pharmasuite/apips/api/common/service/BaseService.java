package com.adasoft.pharmasuite.apips.api.common.service;

import com.adasoft.pharmasuite.apips.api.common.domain.BaseDomain;
import com.adasoft.pharmasuite.apips.api.common.domain.ErrorApi;
import com.adasoft.pharmasuite.apips.api.common.domain.filter.BaseFilterCache;
import com.adasoft.pharmasuite.apips.api.common.domain.odata.OdataPage;
import com.adasoft.pharmasuite.apips.api.common.mapper.odata.ODataPrefer;
import com.adasoft.pharmasuite.apips.api.common.mapper.odata.OlingoQueryParser;
import com.adasoft.pharmasuite.apips.api.common.util.ODataPaging;
import com.adasoft.pharmasuite.apips.cache.service.MemoryCacheService;
import com.adasoft.pharmasuite.apips.core.constant.ApiConstants;
import com.adasoft.pharmasuite.apips.core.constant.UdaConstant;
import com.adasoft.pharmasuite.apips.core.utils.LogManagement;
import com.adasoft.pharmasuite.apips.websocket.config.WebSocketHandler;
import com.adasoft.pharmasuite.apips.websocket.domain.SchedulerEntry;
import com.adasoft.pharmasuite.apips.websocket.service.JobQuartzService;
import com.datasweep.compatibility.client.AccessPrivilegeFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.rockwell.mes.commons.base.ifc.services.IFunctionsEx;
import jakarta.servlet.http.HttpServletRequest;
import net.sf.ehcache.util.concurrent.ConcurrentHashMap;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.core.uri.parser.UriParserException;
import org.apache.olingo.server.core.uri.validator.UriValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import pnuts.lang.PnutsException;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import static java.lang.String.format;


public abstract class BaseService {

    public static final String NEW_JOB_QUARTZ = "New job Quartz ";
    public static final String ERROR_BUILD_ENTRY = "Error build entry %s";
    public static final String ERROR_UPDATE_MSG = "ERROR UPDATE MSG : %s";
    protected Gson gsonMapper;
    protected ObjectMapper objectMapper;
    protected MemoryCacheService memoryCacheService;
    protected JobQuartzService jobQuartzService;
    protected WebSocketHandler webSocketHandler;
    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("d/M/uuuu");
    private static final DateTimeFormatter DMY_HM = DateTimeFormatter.ofPattern("d/M/uuuu HH:mm");
    private static final ZoneId SERVER_ZONE = ZoneId.of("Europe/Madrid");

    @Value("${cache.custom.batch.all}")
    public long timeCache;

    @Value("${app.custom.async.semaphore.pool}")
    public int semaphorePool;

    @Value("${app.custom.async.thread.timeout}")
    public int timeoutThread;

    @Value("${filters.default.monthInterval}")
    public int monthInterval;

    protected static final String BG_REFRESH_DONE = "Background cache refresh done: ";
    protected static final String BG_REFRESH_FAIL = "Background cache refresh failed: ";
    private static final ExecutorService CACHE_REFRESH_POOL =
            Executors.newFixedThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors() / 4), r -> {
                Thread t = new Thread(r, "cache-refresh");
                t.setDaemon(true);
                return t;
            });
    private static final Set<String> REFRESH_IN_FLIGHT = ConcurrentHashMap.newKeySet();
    public static final String ERROR_TO_CREATE_CACHE = "### ERROR TO CREATE CACHE";
    public static final String EXPIRED_CACHE = "Cache expired: ";
    public static final String NEW_CACHE = " new cache: ";
    public static final String ERROR_WHILE_PARSING_CREATION_DATES_INIT_CREATION_DATE_AND_FINISH_CREATION_DATE = "Error while parsing creation dates: initCreationDate %s and finishCreationDate %s ; %s";
    public static final String ADD_FILTER_CREATION_TIME_GREATER_THAN_OR_EQUAL_TO_S_FOR_CREATION_TIME_LESS_THAN = "Add creation time filter CreationTimeGreaterThanOrEqualTo:  %s forCreationTimeLessThan : %s";
    public static final String ERROR_WHILE_PARSING_CREATION_DATES = "Error while parsing creation dates";
    public static final String ERROR_MAPPING = "### ERROR Mapping : %s";

    public BaseService(Gson gsonMapper, ObjectMapper objectMapper, MemoryCacheService memoryCacheService, JobQuartzService jobQuartzService, WebSocketHandler webSocketHandler) {
        this.gsonMapper = gsonMapper;
        this.objectMapper = objectMapper;
        this.memoryCacheService = memoryCacheService;
        this.jobQuartzService = jobQuartzService;
        this.webSocketHandler = webSocketHandler;
    }

    protected BaseDomain buildError(BaseDomain obj, Exception e, String codeMessage) {
        LogManagement.error(
                String.format("Exception en '%s': %s", codeMessage, e.getMessage()),
                this
        );
        e.printStackTrace();
        if (obj.getErrors()==null || obj.getErrors().isEmpty()) {
            obj.setErrors(new ArrayList<>());
        }
        ErrorApi error = ErrorApi.builder()
                .code("ERROR - " + codeMessage)
                .message(e.getMessage())
                .build();
        obj.getErrors().add(error);
        return obj;
    }

    protected Object getCache(BaseFilterCache filter, long timeCache) {
        try {
            String cacheKey = filter.getCacheKey();
            if (cacheKey == null) {
                return null;
            }

            if (filter.isEnableCache()) {
                if (memoryCacheService.isValid(cacheKey, filter.getTimeCache() != null ? filter.getTimeCache() : timeCache, this)) {
                    return memoryCacheService.get(cacheKey);
                }
            }else{
                return null;
            }
        } catch (Exception e) {
            LogManagement.error(ERROR_TO_CREATE_CACHE, this.getClass().getName());
        }
        return null;
    }

    protected void putCache(BaseFilterCache filter, Object items, long timeCache) {
        String cacheKey = filter.getCacheKey();
        if (cacheKey != null) {
            LogManagement.info(NEW_CACHE + cacheKey, this.getClass().toString());
            memoryCacheService.put(cacheKey, items, timeCache);
        }
    }

    @SuppressWarnings("unchecked")
    protected <T> T getOrStaleThenRefresh(BaseFilterCache filter, long defaultTtlMillis, Supplier<T> producer) {

        final String cacheKey = filter.getCacheKey();
        if (cacheKey == null) {
            return producer.get();
        }

        final long ttl = (filter.getTimeCache() != null) ? filter.getTimeCache() : defaultTtlMillis;

        if (!filter.isEnableCache()) {
            return producer.get();
        }

        Object cached = memoryCacheService.get(cacheKey);
        boolean valid = memoryCacheService.isValid(cacheKey, ttl, this);

        if (cached != null) {
            if (!valid) {
                LogManagement.info(EXPIRED_CACHE + cacheKey, this.getClass().toString());
                triggerRefresh(cacheKey, ttl, producer);
            }
            return (T) cached; // válida o stale
        }

        // No hay caché -> cálculo síncrono (primer llenado)
        T fresh = producer.get();
        memoryCacheService.put(cacheKey, fresh, ttl);
        return fresh;
    }

    private <T> void triggerRefresh(String cacheKey, long ttl, Supplier<T> producer) {
        if (!REFRESH_IN_FLIGHT.add(cacheKey)) return; // ya hay un refresh corriendo
        CompletableFuture.runAsync(() -> {
            try {
                T fresh = producer.get();
                memoryCacheService.put(cacheKey, fresh, ttl);
                LogManagement.info(BG_REFRESH_DONE + cacheKey, this.getClass().toString());
            } catch (Exception ex) {
                LogManagement.error(BG_REFRESH_FAIL + ex.getMessage(), this.getClass().getName());
            } finally {
                REFRESH_IN_FLIGHT.remove(cacheKey);
            }
        }, CACHE_REFRESH_POOL);
    }




    /**
     * Versión mejorada que usa solo BaseFilter
     */
    protected void checkJobQuartz(BaseFilterCache filter) {
        try {
            if (filter.getVarSubscribe() != null) {
                String varSubscribe = filter.getVarSubscribe();
                if (!varSubscribe.isEmpty()) {
                    if (!jobQuartzService.exist(varSubscribe)) {
                        SchedulerEntry entry = buildSchedulerEntry(filter);
                          if(entry!=null) {
                              jobQuartzService.create(varSubscribe, entry);
                              LogManagement.info(NEW_JOB_QUARTZ + varSubscribe, this.getClass().getName());
                           }else {
                               LogManagement.info(ERROR_BUILD_ENTRY + varSubscribe, this.getClass().getName());
                           }
                    }
              }
           }
       } catch (Exception e) {
           LogManagement.error(format(ERROR_UPDATE_MSG,e.getMessage()), this.getClass().getName());
        }
    }


    private static SchedulerEntry buildSchedulerEntry(BaseFilterCache filter) {
        Long time = filter.getTimeQuartz() != null ? filter.getTimeQuartz() : null;
        String uri = filter.getFullUrl();
        String queryString = filter.getQueryString();

        return SchedulerEntry.builder()
                .time(time)
                .uri(uri)
                .queryString(queryString)
                .build();
    }
    protected static LocalDate parseDateFilter(String stringDate) {
        if (stringDate == null || stringDate.isBlank()) return null;
        String s = stringDate.trim();

        // 1) ISO-8601 con Z (UTC) -> LocalDate en zona local
        try {
            Instant instant = Instant.parse(s); // usa DateTimeFormatter.ISO_INSTANT bajo el capó
            return LocalDateTime.ofInstant(instant, SERVER_ZONE).toLocalDate();
        } catch (DateTimeParseException ignored) { /* sigue con DMY */ }

        // 2) d/M/uuuu (fecha local)
        try {
            return LocalDate.parse(s, DMY);
        } catch (DateTimeParseException ignored) { /* cae al throw */ }
        return null;
    }
    protected static LocalDate toLocalDateOrNull(LocalDateTime ldt) {
        return (ldt != null) ? ldt.toLocalDate() : null;
    }

    protected void applyServerPaging(com.datasweep.compatibility.client.Filter rf, Integer top, Integer skip) {
        if (top != null && top > 0)  rf.setPagingFilterRowCount(top);
        if (skip != null && skip >= 0) rf.setPagingFilterStartRow(skip + 1); // DFilter usa base 1
    }

    protected static HttpHeaders getHttpHeadersOdata(HttpServletRequest req) {
        var headers = new HttpHeaders();
        headers.add("OData-Version", "4.0");
        if (ODataPrefer.resolveMaxPageSize(req.getHeader(ApiConstants.ODATA_PREFER)) != null) headers.add("Preference-Applied", "odata.maxpagesize=" + ODataPrefer.resolveMaxPageSize(req.getHeader(ApiConstants.ODATA_PREFER)));
        return headers;
    }

    protected ODataPaging generatePageOdata(String odataQuery, OlingoQueryParser parser) throws UriValidationException, UriParserException {

        String decoded = odataQuery == null ? "" : URLDecoder.decode(odataQuery, StandardCharsets.UTF_8);
        var uri         = parser.parseQuery(decoded == null ? "" : decoded);

        var topOpt   = uri.getTopOption();
        var skipOpt  = uri.getSkipOption();
        var countOpt = uri.getCountOption();

        Integer preferMax = ODataPrefer.resolveMaxPageSize(odataQuery);
        return ODataPaging.from(
                topOpt == null ? null : topOpt.getValue(),
                skipOpt == null ? null : skipOpt.getValue(),
                countOpt == null ? null : countOpt.getValue(),
                preferMax,
                null,
                ApiConstants.ODATA_MAX_TOP
        );
    }

    protected UriInfo generateUriOdata(String odataQuery, OlingoQueryParser parser) throws UriValidationException, UriParserException {
        String decoded = odataQuery == null ? "" : URLDecoder.decode(odataQuery, StandardCharsets.UTF_8);

        return parser.parseQuery(decoded == null ? "" : decoded.trim());
    }

    protected String buildODataQuery(OdataPage odataPage) {
        List<String> parts = new ArrayList<>();

        if (odataPage.getOdataFilter() != null && !odataPage.getOdataFilter().isBlank()) {
            String decodedString;
            if (isBase64(odataPage.getOdataFilter())) {
                byte[] decodedBytes = Base64.getDecoder().decode(odataPage.getOdataFilter());
                decodedString = new String(decodedBytes);
                decodedString = decodedString.trim();
            }else{
                decodedString = odataPage.getOdataFilter();
                decodedString = decodedString.trim();
            }
            parts.add("$filter=" + URLEncoder.encode(decodedString, StandardCharsets.UTF_8));
        }
        if (odataPage.getOrderBy() != null && !odataPage.getOrderBy() .isBlank()) {
            parts.add("$orderby=" + URLEncoder.encode(odataPage.getOrderBy(), StandardCharsets.UTF_8));
        }
        if (odataPage.getTop() != null) {
            parts.add("$top=" + odataPage.getTop());
        }
        if (odataPage.getSkip() != null) {
            parts.add("$skip=" + odataPage.getSkip());
        }
        if (odataPage.getCount() != null) {
            parts.add("$count=" + odataPage.getCount());
        }

        return String.join("&", parts);
    }


    private static boolean isBase64(String value) {
        if (value == null) {
            return false;
        }

        // Quitamos espacios al principio y al final
        String s = value.trim();
        if (s.isEmpty()) {
            return false;
        }

        // Si permites saltos de línea/espacios en medio, descomenta:
        // s = s.replaceAll("\\s", "");

        // Solo caracteres válidos Base64 estándar + hasta 2 '=' al final
        if (!s.matches("^[A-Za-z0-9+/]+={0,2}$")) {
            return false;
        }

        try {
            byte[] decoded = Base64.getDecoder().decode(s);
            // Volvemos a codificar
            String encodedAgain = Base64.getEncoder().encodeToString(decoded);

            // Normalizamos quitando '=' finales en ambas
            String normalizedInput = s.replaceAll("=+$", "");
            String normalizedEncoded = encodedAgain.replaceAll("=+$", "");

            return normalizedInput.equals(normalizedEncoded);
        } catch (IllegalArgumentException ex) {
            // El decoder de Java lanza esto si NO es Base64 válido
            return false;
        }
    }

    public void setAccessPrivilegeOrReturnEmptyData(String accessPrivilegeFilter, com.datasweep.compatibility.client.Filter itemFilter, IFunctionsEx functions) {// 1) NULL => devolver todo (sin filtro)
        // 1) NULL => filter not apply
        if (accessPrivilegeFilter == null) {
            return;
        }
        AccessPrivilegeFilter privilegeFilter = functions.createAccessPrivilegeFilter();

        // 2) String.Empty => only UDA NULL
        if (accessPrivilegeFilter.isEmpty()) {
            itemFilter.forUdaEqualTo(UdaConstant.ACCESS_PRIVILEGE, (String) null);
            return;
        }else {
            // 2) String.Empty => add UDA NULL + acces
            itemFilter.forUdaEqualTo(UdaConstant.ACCESS_PRIVILEGE, (String) null);
            privilegeFilter.forNameEqualTo(accessPrivilegeFilter);
            for (Object item : functions.getFilteredAccessPrivileges(privilegeFilter)) {
                if (item instanceof com.datasweep.compatibility.client.AccessPrivilege accessPrivilege) {
                    itemFilter.forUdaEqualTo(UdaConstant.ACCESS_PRIVILEGE, accessPrivilege);
                }
            }
        }
    }

    public static String buildErrorDataSweepException(Exception ex) {
        String message = null;

        if (ex instanceof PnutsException pnutsException && pnutsException.getThrowable() != null) {
            message = pnutsException.getThrowable().getMessage();
        } else if (ex.getCause() instanceof PnutsException pnutsCause && pnutsCause.getThrowable() != null) {
            message = pnutsCause.getThrowable().getMessage();
        }

        if (message == null || message.isBlank()) {
            message = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
        }

        return message;
    }
}
