package io.aino.tools.resend;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.nio.file.NoSuchFileException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ResendApplication.class,
		webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {"spring.test=true"})
public class ResendApplicationTests {
    ResponseEntity response;

    @Autowired
    private ResendApplication resendApplication;
    // Runtime configuration containing directory  locations and  database credentials.
    @Autowired
    private ResendConfigurationProperties resendConfigurationProperties;

	@Rule
	public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort().dynamicHttpsPort());
    @Before
    public void setUp() {
        // Mock Rest-api
        response = null;
        wireMockRule.stubFor(post(urlEqualTo("/rest/v2.0/transaction"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", APPLICATION_JSON_VALUE).withBody("{\"batchId\":\"40707137-e5a3-4890-9e18-283d6b36569a\"}")));
//                        .withBodyFile("/organizationTree.json")));

        wireMockRule.stubFor(post(urlEqualTo("/rest/v2.0/transactionfail"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.FORBIDDEN.value()))
        );
    }

    @After
    public void tearDown() {
        wireMockRule.resetAll();
        wireMockRule.stop();
    }


    /**
     * All sends successfully done. No exceptions thrown
     * @throws IOException
     */
    @Test
    public void successfullResend() throws IOException {
        int port = wireMockRule.port();
        resendConfigurationProperties.seturl("http://localhost:"+port+"/rest/v2.0/transaction");
        resendApplication.executeResend("build/resources/test/aino.log");
    }

    /**
     * Should throw NoSuchFileException because of faulty logfile name.
     */
    @Test(expected = NoSuchFileException.class)
    public void missingLogFile() throws IOException {
        int port = wireMockRule.port();
        resendConfigurationProperties.seturl("http://localhost:"+port+"/rest/v2.0/transaction");
        resendApplication.executeResend("src/test/resources/aino");
    }

    /**
     * When REST-api return error code like 401, it throws IOException
     * @throws IOException
     */
    @Test (expected = IOException.class)
    public void failedAccess() throws IOException {
        int port = wireMockRule.port();
        // Change URL for mock returning 401 Access denied error
        resendConfigurationProperties.seturl("http://localhost:"+port+"/rest/v2.0/transactionfail");
        resendApplication.executeResend("src/test/resources/aino.log");
    }
}
