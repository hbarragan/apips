package com.adasoft.pharmasuite.apips.api.exception.controller;

import com.adasoft.pharmasuite.apips.api.exception.domain.ExceptionFilter;
import com.adasoft.pharmasuite.apips.api.exception.domain.ExceptionPS;
import com.adasoft.pharmasuite.apips.api.exception.service.ExceptionService;
import com.adasoft.pharmasuite.apips.core.constant.ApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

//@CrossOrigin(origins = "*")
//@RestController
//@RequestMapping(value = ApiConstants.API_EXCEPTION, produces = MediaType.APPLICATION_JSON_VALUE)
//@Tag(name = ApiConstants.TAG_EXCEPTION, description = ApiConstants.DESCRIPTION_EXCEPTION)
public class ExceptionController {

    public static final String SUMMARY_GET_ALL_EXCEPTION = "Get filtered exception";
    public static final String DESCRIPTION_GET_ALL_EXCEPTION = "Return a list of exception filtered.";
    public static final String OPERATION_ID_GET_ALL_EXCEPTION = "getAllException";
    private final ExceptionService exceptionService;

    public ExceptionController(ExceptionService service) {
        this.exceptionService = service;
    }

    //@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    //@Operation(
    //        summary = SUMMARY_GET_ALL_EXCEPTION,
    //        description = DESCRIPTION_GET_ALL_EXCEPTION,
    //        operationId = OPERATION_ID_GET_ALL_EXCEPTION,
    //        tags = { ApiConstants.TAG_EXCEPTION}
    //)
    public ResponseEntity<List<ExceptionPS>> getAllException(@ParameterObject ExceptionFilter filter, HttpServletRequest request) {
        filter.setRequestInfo(request);
        return exceptionService.getAllException(filter);
    }

}
