package com.adasoft.pharmasuite.apips.api.exception.service;

import com.adasoft.pharmasuite.apips.api.exception.domain.ExceptionFilter;
import com.adasoft.pharmasuite.apips.api.exception.domain.ExceptionPS;

import org.springframework.http.ResponseEntity;

import java.util.List;

public interface ExceptionService {
     ResponseEntity<List<ExceptionPS>> getAllException(ExceptionFilter filter);

}
