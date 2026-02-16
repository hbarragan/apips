package com.adasoft.pharmasuite.apips.core.constant;

public class ApiConstants {

    public static final String  API_HOME = "/api";
    public static final String  SEPARATOR = "/";
    public static final String  PATH_PAGED = "/paged";

    // MEASURE
    public static final String  DEFAULT_QUANTITY_UOM = "kg";
    public static final String  DEFAULT_POTENCY_UOM = "%";

    // ORDER
    public static final String TAG_ORDER = "order";
    public static final String DESCRIPTION_ORDER = "Operations related to order management";
    public static final String API_ORDER = API_HOME+SEPARATOR+ TAG_ORDER;
    //  - methods
    public static final String API_PROCESS_ORDER = "/process-order";
    public static final String API_PROCESS_ORDER_PAGED = "/process-order-paged";

    // RECIPE
    public static final String TAG_RECIPE = "recipe";
    public static final String DESCRIPTION_RECIPE = "Operations related to recipe management";
    public static final String API_RECIPE = API_HOME+SEPARATOR+ TAG_RECIPE;

    // EXCEPTION
    public static final String TAG_EXCEPTION = "exception";
    public static final String TAG_EXCEPTIONS = "exceptions";
    public static final String DESCRIPTION_EXCEPTION = "Operations related to exception management";
    public static final String API_EXCEPTION = API_HOME+SEPARATOR+ TAG_EXCEPTION;

    // BATCH
    public static final String  TAG_BATCH = "batch";
    public static final String  TAG_SUBLOT = "sublot";
    public static final String  TAG_TRANSACTION_HISTORY = "transaction-history";
    public static final String  DESCRIPTION_BATCH = "Operations related to batch management";
    public static final String  API_BATCH = API_HOME+SEPARATOR+TAG_BATCH;
    public static final String  API_SUBLOT = SEPARATOR+TAG_SUBLOT;
    public static final String  API_TRANSACTION_HISTORY = SEPARATOR+TAG_TRANSACTION_HISTORY;
    // WORKFLOW
    public static final String  TAG_WORKFLOW = "workflow";
    public static final String  DESCRIPTION_WORKFLOW = "Operations related to workflow management";
    public static final String  API_WORKFLOW = API_HOME+SEPARATOR+TAG_WORKFLOW;

    // DASHBOARD
    public static final String  TAG_DASHBOARD = "dashboard";
    public static final String  TAG_SUMMARY = "summary";
    public static final String  DESCRIPTION_DASHBOARD = "Operations related to dashboard management";
    public static final String  API_DASHBOARD = API_HOME+SEPARATOR+TAG_DASHBOARD;
    public static final String  API_DASHBOARD_EXCPETIONS_SUMMARY = SEPARATOR+TAG_EXCEPTIONS+SEPARATOR+TAG_SUMMARY;

    // SCHEDULER
    public static final String  TAG_SCHEDULER = "scheduler-job";
    public static final String  DESCRIPTION_SCHEDULER = "Operations related to scheduler management";
    public static final String  API_SCHEDULER = API_HOME+SEPARATOR+TAG_SCHEDULER;
    public static final String  API_KEYS = "/keys";
    public static final String  API_DETAILS = "/details";
    public static final String  API_ALL = "/all";
    public static final String  API_TRIGGER = "/trigger";
    public static final String  API_DELETE = "/delete";
    public static final String  API_DELETE_ALL = "/delete-all";


    // HEALTH
    public static final String  TAG_HEALTH = "health";
    public static final String  DESCRIPTION_HEALTH = "Health application";
    public static final String  API_HEALTH = TAG_HEALTH;

    //ODATA
    public static final String  ODATA_SKIP = "$skip";
    public static final String  ODATA_FILTER = "$filter";
    public static final String  ODATA_TOP = "$top";
    public static final String  ODATA_SKIP_TOKEN = "$skiptoken";
    public static final String  ODATA_COUNT = "$count";
    public static final String  ODATA_ORDER_BY = "$orderby";
    public static final String  ODATA_PREFER = "Prefer";
    public static final int    ODATA_DEFAULT_TOP =25;
    public static final int    ODATA_MAX_TOP =999999999;
}
