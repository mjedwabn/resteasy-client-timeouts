package xyz.mjdev.resteasy.client.timeouts;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient43Engine;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@Startup
public class Hello {
	private static final Logger LOGGER = Logger.getLogger(Hello.class.getSimpleName());

	private static final String URI = "http://www.fakeresponse.com/api/?api_key=b3898a2e-5afb-481c-a50d-a80b86643c2c&sleep=5";
	private static final int TIMEOUT = 1000;

	@PostConstruct
	void hello() {
		RequestConfig requestConfig = RequestConfig.custom()
				.setSocketTimeout(TIMEOUT)
				.setConnectTimeout(TIMEOUT)
				.setConnectionRequestTimeout(TIMEOUT)
				.build();
		CloseableHttpClient httpClient = HttpClients.custom()
				.setDefaultRequestConfig(requestConfig)
				.build();
		Client client = new ResteasyClientBuilder()
				.httpEngine(new ApacheHttpClient43Engine(httpClient))
				.build();

		final Instant start = Instant.now();
		LOGGER.log(Level.INFO, "Executing http request...");
		try {
			final Response response = client.target(URI)
					.request().get();
			LOGGER.log(Level.INFO, "Response code: {0}", response.getStatus());
		}
		catch (ProcessingException e) {
			LOGGER.log(Level.SEVERE, "Request failed cause of: {0}",
					Optional.ofNullable(e.getCause()).orElse(e).getMessage());
		}
		finally {
			LOGGER.log(Level.INFO, "Request duration: {0}",
					Duration.between(start, Instant.now()));
		}
	}
}
