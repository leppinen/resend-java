package io.aino.tools.resend;


import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;


/**
 * Runtime configuration for SAP-Safea integration.
 * <p>
 * All these parameters are configured in  application.yml-file. See src/resources/xxx.yml. Configuration consist of
 * application specific  data, logging configuration and database connection setup. Here is a sample of runtime
 * configuration.
 * </p>
 * <pre>
 * {@code
 * aino:
 *     #Incoming  logfile
 *     inputFile: build/test/resources/aino.log
 *     #customer specific apikey
 *     apikey: xxxxxyyyyyzzzzz
 *     ainoURL: https://data.aino.io/rest/v2.0/transaction
 *
 * logging:
 *     level:
 *         root: INFO
 *         io:
 *             aino:
 *                 tools:
 *                     Resend: DEBUG
 *
 *     path: logs
 *
 *
 * }</pre>
 */

@Validated
@Component
@ConfigurationProperties(prefix = "aino")
@SpringBootConfiguration
public class ResendConfigurationProperties {

    private String inputFile;

    /**
     * Get configured input file where failed Aino log entries are stored
     * @return input file as a String
     */
    public String getInputFile() {
        return this.inputFile;
    }

    /**
     * Set input file
     *
     * @param fileName as a string for log containing failed Aino messages
     */
    public void setInputFile(String fileName) {
        this.inputFile = fileName;
    }

    private String apikey;

    /**
     * Get configured Aino apikey ie. authorization credentials
     * @return apikey as a String
     */
    public String getApikey() {
        return this.apikey;
    }

    /**
     * Set customer APIkey for Aino  authentication
     *
     * @param key Apikey as a string.
     */
    public void setApikey(String key) {
        this.apikey =key;
    }

    private String url;

    /**
     * Get configured Aino URL
     * @return URL as a String
     */
    public String getUrl() {
        return this.url;
    }

    /**
     * Set Aino URL
     *
     * @param url Aino USL as a string.
     */
    public void seturl(String url) {
        this.url = url;
    }

}