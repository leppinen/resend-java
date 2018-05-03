package io.aino.tools.resend;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.stream.Stream;

@SpringBootApplication
public class ResendApplication implements CommandLineRunner {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(ResendApplication.class);

	boolean ainomessage = false;
	private String currentTransaction = null;

	// Runtime configuration containing directory  locations and  database credentials.
	@Autowired
	private ResendConfigurationProperties resendConfigurationProperties;
	// When  running integration tests, this flag is set true which skips initialization and enables individual
	// method testing
	@Value("${spring.test}")
	private boolean isTest;

	public static void main(String[] args){
		SpringApplication.run(ResendApplication.class, args);
	}

	/**
	 * Starts individual service run and check parameters.
	 *
	 */
	@Override
	public void run(String... args) throws IOException {
		if (args.length > 0) {
			logger.info("Logfile given as parameter:"+args[0]);
			if(args[0]!= null && !args[0].isEmpty())
				resendConfigurationProperties.setInputFile(args[0]);
		}
		if(resendConfigurationProperties.getInputFile()== null || resendConfigurationProperties.getInputFile().isEmpty()){
			logger.error("No Aino logfile given");
			System.exit(-1);
		}

		// Return if test
		if (!isTest){
			executeResend(resendConfigurationProperties.getInputFile());
		}

	}
		/**
         * Starts individual service run.
         * @param fileName for Aino log containing items for resend
         */
	public void executeResend(String fileName) throws IOException{

	    int[] statuses = new int[1];
		logger.info("Starting AinoIO export run for:"+fileName);

		try (Stream<String> stream = Files.lines(Paths.get(fileName))) {
			stream.forEach( line  -> {
				// Check if  line is part of the  Aino Error message
				if (line.equals("AINO_ERROR_START")) {
					ainomessage = true;
					currentTransaction = "";
				} else if (line.equals("AINO_ERROR_END")) {
					ainomessage = false;
					int status = sendTransactionToAino(currentTransaction);
					if(status != 200 && status != 202 ) {
                        logger.error("Failed to send transaction to Aino:" + status);
                        // Effectively final. So statuses is like final, but it's value varies
                        // Here it is not 0 when communication error happens
                        statuses[0]=status;
                    }
					currentTransaction = null; // Flush buffer
				} else if (ainomessage) {
					if(currentTransaction != null )
						currentTransaction = currentTransaction.concat(line);
					else {
						currentTransaction = line;
					}
				}
			});
		} catch (IOException ex) {
			logger.error( ex.toString());
			throw ex;
		}
		if( statuses[0]!= 0)
		    throw new IOException("Failed to communicate with Aino:"+statuses[0]);
	}

	public int sendTransactionToAino(String requestJson) {
		int rv = -1;
		RestTemplate restTemplate = new RestTemplate();

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setAccept(Collections.singletonList(MediaType.ALL));
		headers.add("Accept-Charset","UTF-8");
		headers.add("Authorization","apikey "+resendConfigurationProperties.getApikey());

		if(logger.isDebugEnabled()) {
			logger.debug("Sending:" +requestJson + " Headers=" + headers);
		}

		try {
			HttpEntity<String> entity = new HttpEntity<String>(requestJson, headers);
			ResponseEntity<String> response = restTemplate.exchange(resendConfigurationProperties.getUrl(), HttpMethod.POST, entity, String.class);
			if(logger.isDebugEnabled())
				logger.debug("Result - status ("+ response.getStatusCode() + ") has body: " + response.hasBody());
			rv = response.getStatusCodeValue();
			if(response.hasBody()) {
				String jsonResp = response.getBody();
				if(logger.isDebugEnabled()) {
					logger.info("Incoming=" + jsonResp);
				}
			}
		}
		catch(HttpClientErrorException exp){
			logger.error(" Resend Aino transaction:"+exp.getMessage());
			logger.error("Reason:"+exp.getResponseBodyAsString());
			logger.error("\nAINO_ERROR_START\n"+requestJson+"\nAINO_ERROR_END");
			rv = exp.getRawStatusCode();

		}
		catch (ResourceAccessException rexp){
			logger.error("Error, Resend Aino transaction: "+rexp.getMessage());
		}
		return rv;
	}
}
