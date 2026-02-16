package com.adasoft.tomcat.integration.api;

import com.adasoft.pharmasuite.apips.ApiApplication;
import com.adasoft.tomcat.integration.common.ITTestCommon;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.stream.Stream;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        classes = ApiApplication.class
)
@ActiveProfiles("it")
@Execution(ExecutionMode.CONCURRENT)
public class ApiControllerITTest extends ITTestCommon {
    private static final String CASES_PATTERN = "classpath*:/it-cases/api-cases/*/*.json";

    @TestFactory
    Stream<DynamicTest> cases_from_files() {
        return loadAndRunCases(CASES_PATTERN);
    }
}
