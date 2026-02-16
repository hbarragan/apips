package com.adasoft.pharmasuite.apips.api.dashboard.service.impl;

import com.adasoft.pharmasuite.apips.api.common.service.BaseService;
import com.adasoft.pharmasuite.apips.api.dashboard.domain.AveragePeriod;
import com.adasoft.pharmasuite.apips.api.dashboard.domain.Dashboard;
import com.adasoft.pharmasuite.apips.api.dashboard.domain.DashboardFilter;
import com.adasoft.pharmasuite.apips.api.dashboard.domain.DashboardStatesProperties;
import com.adasoft.pharmasuite.apips.api.dashboard.domain.Exceptions;
import com.adasoft.pharmasuite.apips.api.dashboard.service.DashBoardService;
import com.adasoft.pharmasuite.apips.cache.service.MemoryCacheService;
import com.adasoft.pharmasuite.apips.core.bbdd.CustomSqlExecutor;
import com.adasoft.pharmasuite.apips.core.bbdd.SqlQueryBuilder;
import com.adasoft.pharmasuite.apips.core.utils.LogManagement;
import com.adasoft.pharmasuite.apips.websocket.config.WebSocketHandler;
import com.adasoft.pharmasuite.apips.websocket.service.JobQuartzService;
import com.datasweep.compatibility.client.ControlRecipe;
import com.datasweep.compatibility.client.ControlRecipeFilter;
import com.datasweep.compatibility.client.DatasweepException;
import com.datasweep.compatibility.client.ProcessOrderItem;
import com.datasweep.compatibility.client.ProcessOrderItemFilter;
import com.datasweep.compatibility.ui.Time;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.rockwell.mes.commons.base.ifc.services.IFunctionsEx;
import com.rockwell.mes.commons.deviation.ifc.IExceptionRecordingService;
import com.rockwell.mes.commons.deviation.ifc.exceptionrecording.IMESExceptionRecord;
import com.rockwell.mes.commons.deviation.ifc.exceptionrecording.IMESExceptionRecordFilter;
import com.rockwell.mes.commons.deviation.impl.ExceptionRecordingService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StopWatch;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Vector;

public class DashBoardServiceImpl extends BaseService implements DashBoardService {

    private final IFunctionsEx functions;
    private final IExceptionRecordingService recodingExceptionService;
    private final DashboardStatesProperties statesProps;

    public DashBoardServiceImpl(IFunctionsEx functions, Gson gsonMapper, ObjectMapper objectMapper, MemoryCacheService memoryCacheService, JobQuartzService jobQuartzService, ExceptionRecordingService recodingExceptionService, DashboardStatesProperties statesProps, WebSocketHandler webSocketHandler) {
            super(gsonMapper, objectMapper, memoryCacheService, jobQuartzService, webSocketHandler);

        this.functions = functions;
        this.recodingExceptionService = recodingExceptionService;
        this.statesProps = statesProps;
    }

    @Override
    public ResponseEntity<Dashboard> getAllExceptionsSummary(DashboardFilter filter) {
        try {
            Dashboard item = getOrStaleThenRefresh(
                    filter, timeCache,
                    () -> {
                        try {
                            return buildDashboard();
                        } catch (DatasweepException e) {
                            LogManagement.error("Error buildDashboard"+e.getMessage(),this);
                            throw new RuntimeException(e);
                        }
                    }
            );
            checkJobQuartz(filter);
            return ResponseEntity.ok(item);
        } catch (Exception e) {
            return ResponseEntity.ok((Dashboard) buildError(Dashboard.builder().build(), e, "getAllExceptionsSummary"));
        }
    }



    private Dashboard buildDashboard() throws DatasweepException {
        Dashboard dashboardReturn = Dashboard.builder().exceptions(new Exceptions()).build();
        dashboardReturn.getExceptions().setOpenExceptionRiskNone(getCountExceptionRiskOpened(IMESExceptionRecord.RiskClass.none.longValue()));
        dashboardReturn.getExceptions().setOpenExceptionRiskLow(getCountExceptionRiskOpened(IMESExceptionRecord.RiskClass.low.longValue()));
        dashboardReturn.getExceptions().setOpenExceptionRiskMedium(getCountExceptionRiskOpened(IMESExceptionRecord.RiskClass.medium.longValue()));
        dashboardReturn.getExceptions().setOpenExceptionRiskHigh(getCountExceptionRiskOpened(IMESExceptionRecord.RiskClass.high.longValue()));
        dashboardReturn.getExceptions().setAverageTimeOpenException(calculateAverageOpenDuration());
        dashboardReturn.setTimeServer(OffsetDateTime.now(ZoneOffset.UTC));
        return dashboardReturn;
    }

    private long executeCount(List<Long> controlRecipes,List<Long> statusCodes, List<Long> riskCodes){
        CustomSqlExecutor ex = new CustomSqlExecutor();
        String sqlQuery = getQuery(controlRecipes,statusCodes,riskCodes);
        return ex.getCount(sqlQuery);
    }

    private  String getQuery(List<Long> controlRecipes,List<Long> statusCodes, List<Long> riskCodes) {
        ClassPathResource res = new ClassPathResource("sql/geAllExceptionsByFilter.sql");
        try{
            String sqlString= StreamUtils.copyToString(res.getInputStream(),StandardCharsets.UTF_8);
            SqlQueryBuilder builder =  new SqlQueryBuilder(sqlString)
                    .with("controlRecipes", controlRecipes)
                    .with("statusCodes",     statusCodes)
                    .with("riskCodes",       riskCodes);

            return builder.build();
        } catch (Exception e) {
            LogManagement.error(" getQuery :"+e.getMessage(),this);
            return "";
        }
    }

    private long getExceptionsCountByProcessOrdersStatus(String[] listState) {
        List<Long> listControlRecipe= new ArrayList<>();
        StopWatch sw = new StopWatch("getExceptionsCountByProcessOrdersStatus(listState:"+ Arrays.toString(listState) +")");
        sw.start();
        ProcessOrderItemFilter processOrderFilter = functions.createProcessOrderItemFilter();
        processOrderFilter.forCurrentStateIn(listState);
        Vector vector = functions.getFilteredProcessOrderItems(processOrderFilter);
        for (Object item : vector) {
            if (item instanceof ProcessOrderItem po) {
                ControlRecipeFilter controlRecipeFilter=functions.createControlRecipeFilter();
                controlRecipeFilter.forProcessOrderItemKeyEqualTo(po.getKey());
                Vector vectorControlRecipes=functions.getFilteredControlRecipes(controlRecipeFilter);
                for (Object itemControlRecipe : vectorControlRecipes) {
                    if (itemControlRecipe instanceof ControlRecipe cr) {
                        listControlRecipe.add(cr.getKey());
                    }
                }
            }
        }
        sw.stop();
        LogManagement.info(sw.prettyPrint(),this);
        return executeCount(listControlRecipe,null, null);
    }

    private int getCountExceptionRiskOpened(long riskCode) throws DatasweepException {
        StopWatch sw = new StopWatch("getCountExceptionRiskOpened(riskCode"+riskCode+")");
        sw.start();
        IMESExceptionRecordFilter filterRisk = recodingExceptionService.createExceptionRecordFilter();
        filterRisk.forRiskClassEqualTo(riskCode);
        filterRisk.forStatusEqualTo(IMESExceptionRecord.Status.open.longValue());
        List<IMESExceptionRecord> listExceptions = filterRisk.getFilteredObjects();
        sw.stop();
        LogManagement.info(sw.prettyPrint(),this);
        return listExceptions.size();
    }

    private String calculateAverageOpenDuration() throws DatasweepException {
        long sumTime = 0L;
        long averageTime = 0L;
        StopWatch sw = new StopWatch("calculateAverageOpenDuration");
        sw.start();
        IMESExceptionRecordFilter filterRisk = recodingExceptionService.createExceptionRecordFilter();
        filterRisk.forStatusEqualTo(IMESExceptionRecord.Status.open.longValue());
        List<IMESExceptionRecord> listExceptions = filterRisk.getFilteredObjects();
        if(listExceptions!=null) {
            for (IMESExceptionRecord exceptionRecord : listExceptions) {
                Time time = exceptionRecord.getCreationTime();
                if (time != null) {
                    Calendar now = Calendar.getInstance();
                    long interval=now.getTimeInMillis() - time.getCalendar().getTimeInMillis();
                    sumTime = sumTime + interval;
                }
            }
            averageTime = sumTime / listExceptions.size();
        }
        AveragePeriod averagePeriod=mapToAveragePeriod(averageTime);
        sw.stop();
        LogManagement.info(sw.prettyPrint(),this);
        return averagePeriod.toString();

    }

    private AveragePeriod mapToAveragePeriod(long averageTime){
        Instant baseInstant = Instant.EPOCH;
        ZonedDateTime baseZdt = baseInstant.atZone(ZoneOffset.UTC);
        ZonedDateTime resultZdt = baseZdt.plus(averageTime, ChronoUnit.MILLIS);
        Period periodo = Period.between(baseZdt.toLocalDate(), resultZdt.toLocalDate());
        ZonedDateTime intermediate = baseZdt.plus(periodo);
        Duration resto = Duration.between(intermediate, resultZdt);
        int hours   = (int) resto.toHours();
        int minutes = (int) resto.minusHours(hours).toMinutes();
        int seconds = (int) resto.minusHours(hours).minusMinutes(minutes).getSeconds();

        return new AveragePeriod(periodo.getYears(), periodo.getMonths(), periodo.getDays(), hours, minutes, seconds);

    }
}
