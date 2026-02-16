package com.adasoft.pharmasuite.apips.websocket.job;

import com.adasoft.pharmasuite.apips.core.utils.LogManagement;
import com.adasoft.pharmasuite.apips.websocket.domain.SubscriptionRegistry;
import org.quartz.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static java.lang.String.format;

public class GenericQuartzJob implements Job {

    private final HttpClient httpClient = HttpClient.newBuilder().build();

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap data = context.getJobDetail().getJobDataMap();
        String uri = data.getString("uri");
        String queryString = data.getString("queryString");
        String varName = data.getString("varName");
        String filterJson = data.getString("filter"); // opcional

        try {
            LogManagement.info("INIT JOB: " + varName, this.getClass().getName());

//            String queryStringWithoutCache = getStringByQuery(queryString);
//            String fullUrl = uri + "?" + queryStringWithoutCache;
            String fullUrl = uri + "?" + queryString;

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(fullUrl))
                    .header("Content-Type", "application/json");

            HttpRequest request = (filterJson != null && !filterJson.isEmpty())
                    ? requestBuilder.POST(HttpRequest.BodyPublishers.ofString(filterJson)).build()
                    : requestBuilder.GET().build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();


            if (SubscriptionRegistry.hasSubscribers(varName)) {
                LogManagement.info("Send msg: " + varName, this.getClass().getName());
                SubscriptionRegistry.broadcast(varName, responseBody);
            } else {
                LogManagement.info("No subscribers for: " + varName, this.getClass().getName());
            }

        } catch (Exception e) {
            LogManagement.error(format("### ERROR JOB: %s", e.getMessage()), this.getClass().getName());
            throw new JobExecutionException(e);
        }
    }

    private static String getStringByQuery(String queryString) {
        if (queryString == null || queryString.isEmpty()) {
            return "enableCache=false";
        } else if (queryString.contains("enableCache")) {
            return queryString.replaceAll("(?<=^|&)enableCache=[^&]*", "enableCache=false");
        } else {
            return queryString + "&enableCache=false";
        }
    }
}
