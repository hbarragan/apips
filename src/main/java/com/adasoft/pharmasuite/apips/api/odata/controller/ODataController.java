package com.adasoft.pharmasuite.apips.api.odata.controller;

import com.adasoft.pharmasuite.apips.api.batch.domain.Batch;
import com.adasoft.pharmasuite.apips.api.batch.domain.TransactionHistory;
import com.adasoft.pharmasuite.apips.api.batch.service.BatchService;
import com.adasoft.pharmasuite.apips.api.common.domain.PageResponseOdata;
import com.adasoft.pharmasuite.apips.api.common.domain.odata.OdataPage;
import com.adasoft.pharmasuite.apips.api.odata.provider.ApplicationEdmProvider;
import com.adasoft.pharmasuite.apips.api.order.domain.ProcessOrder;
import com.adasoft.pharmasuite.apips.api.order.service.ProcessOrdersService;
import com.adasoft.pharmasuite.apips.api.recipe.domain.Recipe;
import com.adasoft.pharmasuite.apips.api.recipe.service.RecipeService;
import com.adasoft.pharmasuite.apips.api.workflow.domain.Workflow;
import com.adasoft.pharmasuite.apips.api.workflow.service.WorkflowService;
import com.adasoft.pharmasuite.apips.core.constant.ApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.adasoft.pharmasuite.apips.api.common.controller.CommonController.getOdataPage;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/odata")
@Tag(name = "OData", description = "OData compatible service")
public class ODataController {


    private final RecipeService recipeService;
    private final BatchService batchService;
    private final WorkflowService workflowService;
    private final ProcessOrdersService processOrdersService;
    private final ApplicationEdmProvider applicationEdmProvider;
    private static final String ODATA_SEGMENT = "odata";

    public ODataController(
            RecipeService recipeService,
            BatchService batchService,
            WorkflowService workflowService,
            ProcessOrdersService processOrdersService,
            ApplicationEdmProvider applicationEdmProvider
    ) {
        this.recipeService = recipeService;
        this.batchService = batchService;
        this.workflowService = workflowService;
        this.processOrdersService = processOrdersService;
        this.applicationEdmProvider = applicationEdmProvider;
    }

    // ==================== SERVICE DOCUMENT ====================

    @GetMapping
    public ResponseEntity<byte[]> getServiceDocument(HttpServletRequest request, @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept) {
        String serviceRoot = getBaseUrl(request) + "/";

        OData odata = OData.newInstance();
        ServiceMetadata md = odata.createServiceMetadata(applicationEdmProvider, List.of());

        ContentType ct = negotiate(accept);
        byte[] body;

        try {
            ODataSerializer serializer = odata.createSerializer(ct);
            try (InputStream in = serializer.serviceDocument(md, serviceRoot).getContent()) {
                body = in.readAllBytes();
            }
        } catch (IOException | SerializerException e) {
            throw new IllegalStateException("Error serializing OData service document", e);
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(ct.toContentTypeString()))
                .body(body);
    }

    // ==================== $metadata ====================

    @GetMapping(value = "/$metadata")
    @Operation(summary = "OData $metadata", description = "Returns the OData metadata document (EDMX)")
    public ResponseEntity<String> getMetadata(HttpServletRequest request) {
        OData odata = OData.newInstance();
        String xml;
        ServiceMetadata md = odata.createServiceMetadata(applicationEdmProvider, List.of());
        try{
            ODataSerializer serializer = odata.createSerializer(ContentType.APPLICATION_XML);
            InputStream in = serializer.metadataDocument(md).getContent();
            xml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException | SerializerException e) {
            throw new IllegalStateException("Error serializing OData $metadata", e);
        }
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(xml);
    }

    // ==================== ENTITY SETS ====================

    @GetMapping(value = "/Recipes")
    @Operation(summary = "Get Recipes", description = "Returns recipes with OData query support")
    public ResponseEntity<PageResponseOdata<Recipe>> getRecipes(
            HttpServletRequest request,
            @RequestParam(name = ApiConstants.ODATA_FILTER, required = false) String filter,
            @RequestParam(name = ApiConstants.ODATA_ORDER_BY, required = false) String orderBy,
            @RequestParam(name = ApiConstants.ODATA_TOP, required = false) Integer top,
            @RequestParam(name = ApiConstants.ODATA_SKIP, required = false) Integer skip,
            @RequestParam(name = ApiConstants.ODATA_COUNT, required = false) Boolean count
    ) {
        OdataPage odataPage = getOdataPage(filter, orderBy, top, skip, count, request);
        ResponseEntity<PageResponseOdata<Recipe>> serviceResponse = recipeService.getFilteredOData(odataPage);
        PageResponseOdata<Recipe> body = serviceResponse.getBody();
        String baseUrl = getBaseUrl(request);
        String ctx = baseUrl + "/$metadata#Recipes";
        return getPageResponseOdataResponseEntity(count, body, ctx);
    }



    @GetMapping(value = "/Batches")
    @Operation(summary = "Get Batches", description = "Returns batches with OData query support")
    public ResponseEntity<PageResponseOdata<Batch>> getBatches(
            HttpServletRequest request,
            @RequestParam(name = ApiConstants.ODATA_FILTER, required = false) String filter,
            @RequestParam(name = ApiConstants.ODATA_ORDER_BY, required = false) String orderBy,
            @RequestParam(name = ApiConstants.ODATA_TOP, required = false) Integer top,
            @RequestParam(name = ApiConstants.ODATA_SKIP, required = false) Integer skip,
            @RequestParam(name = ApiConstants.ODATA_COUNT, required = false) Boolean count
    ) {
        OdataPage odataPage = getOdataPage(filter, orderBy, top, skip, count, request);
        ResponseEntity<PageResponseOdata<Batch>> serviceResponse = batchService.getFilteredBatchOData(odataPage);
        PageResponseOdata<Batch> body = serviceResponse.getBody();

        String baseUrl = getBaseUrl(request);
        String ctx = baseUrl + "/$metadata#Batches";
        return getPageResponseOdataResponseEntity(count, body, ctx);
    }

    @GetMapping(value = "/Workflows")
    @Operation(summary = "Get Workflows", description = "Returns workflows with OData query support")
    public ResponseEntity<PageResponseOdata<Workflow>> getWorkflows(
            HttpServletRequest request,
            @RequestParam(name = ApiConstants.ODATA_FILTER, required = false) String filter,
            @RequestParam(name = ApiConstants.ODATA_ORDER_BY, required = false) String orderBy,
            @RequestParam(name = ApiConstants.ODATA_TOP, required = false) Integer top,
            @RequestParam(name = ApiConstants.ODATA_SKIP, required = false) Integer skip,
            @RequestParam(name = ApiConstants.ODATA_COUNT, required = false) Boolean count
    ) {
        OdataPage odataPage = getOdataPage(filter, orderBy, top, skip, count, request);
        ResponseEntity<PageResponseOdata<Workflow>> serviceResponse = workflowService.getFilteredOData(odataPage);
        PageResponseOdata<Workflow> body = serviceResponse.getBody();

        String baseUrl = getBaseUrl(request);
        String ctx = baseUrl + "/$metadata#Workflows";
        return getPageResponseOdataResponseEntity(count, body, ctx);
    }

    @GetMapping(value = "/ProcessOrders")
    @Operation(summary = "Get Process Orders", description = "Returns process orders with OData query support")
    public ResponseEntity<PageResponseOdata<ProcessOrder>> getProcessOrders(
            HttpServletRequest request,
            @RequestParam(name = ApiConstants.ODATA_FILTER, required = false) String filter,
            @RequestParam(name = ApiConstants.ODATA_ORDER_BY, required = false) String orderBy,
            @RequestParam(name = ApiConstants.ODATA_TOP, required = false) Integer top,
            @RequestParam(name = ApiConstants.ODATA_SKIP, required = false) Integer skip,
            @RequestParam(name = ApiConstants.ODATA_COUNT, required = false) Boolean count
    ) {
        OdataPage odataPage = getOdataPage(filter, orderBy, top, skip, count, request);
        ResponseEntity<PageResponseOdata<ProcessOrder>> serviceResponse = processOrdersService.getFilteredOData(odataPage);
        PageResponseOdata<ProcessOrder> body = serviceResponse.getBody();

        String baseUrl = getBaseUrl(request);
        String ctx = baseUrl + "/$metadata#ProcessOrders";
        return getPageResponseOdataResponseEntity(count, body, ctx);
    }

    @GetMapping(value = "/TransactionHistories")
    @Operation(summary = "Get Transaction History", description = "Returns transaction history with OData query support")
    public ResponseEntity<PageResponseOdata<TransactionHistory>> getTransactionHistory(
            HttpServletRequest request,
            @RequestParam(name = ApiConstants.ODATA_FILTER, required = false) String filter,
            @RequestParam(name = ApiConstants.ODATA_ORDER_BY, required = false) String orderBy,
            @RequestParam(name = ApiConstants.ODATA_TOP, required = false) Integer top,
            @RequestParam(name = ApiConstants.ODATA_SKIP, required = false) Integer skip,
            @RequestParam(name = ApiConstants.ODATA_COUNT, required = false) Boolean count
    ) {
        OdataPage odataPage = getOdataPage(filter, orderBy, top, skip, count, request);
        ResponseEntity<PageResponseOdata<TransactionHistory>> serviceResponse =
                batchService.getFilteredTransactionHistoryOData(odataPage);
        PageResponseOdata<TransactionHistory> body = serviceResponse.getBody();

        String baseUrl = getBaseUrl(request);
        String ctx = baseUrl + "/$metadata#TransactionHistories";
        return getPageResponseOdataResponseEntity(count, body, ctx);
    }

    // ==================== HELPERS ====================

    private String getBaseUrl(HttpServletRequest request) {
        return ServletUriComponentsBuilder
                .fromContextPath(request)
                .pathSegment(ODATA_SEGMENT)
                .build()
                .toUriString();
    }

    private static <T> ResponseEntity<PageResponseOdata<T>> getPageResponseOdataResponseEntity(Boolean count, PageResponseOdata<T> body, String ctx) {
        Long odataCount = Boolean.TRUE.equals(count) ? (body != null && body.count() != null ? body.count() : 0L) : null;


        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/json;odata.metadata=minimal"))
                .body(new PageResponseOdata<>(ctx, odataCount, body != null ? body.value() : List.of()));
    }

    private static ContentType negotiate(String accept) {
        if (accept == null || accept.isBlank()) return ContentType.APPLICATION_JSON;

        // Prioridad real: si el cliente acepta JSON, usa JSON
        if (accept.contains("application/json")) return ContentType.APPLICATION_JSON;

        // Si no, XML (si lo acepta)
        if (accept.contains("application/xml") || accept.contains("xml")) return ContentType.APPLICATION_XML;

        return ContentType.APPLICATION_JSON;
    }
}
