package com.adasoft.tomcat.integration.ws;

import com.adasoft.pharmasuite.apips.ApiApplication;
import com.adasoft.tomcat.integration.common.ITTestCommon;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        classes = ApiApplication.class
)
@ActiveProfiles("it")
@Execution(ExecutionMode.CONCURRENT)
public class WebSocketITTest extends ITTestCommon {

    private static final String CASES_PATTERN = "classpath*:/it-cases/ws/*.json";

//    @TestFactory
//    Stream<DynamicTest> ws_cases_from_files() {
//        var repository = new ClasspathWsCaseRepository(objectMapper, CASES_PATTERN);
//        var rest = getRestClient();
//        var runner = new WsCaseRunner(objectMapper, rest);
//
//        return repository.load().map(tc -> DynamicTest.dynamicTest(
//                tc.getName(),
//                () -> runner.run(tc, httpPort)
//        ));
//    }
}
