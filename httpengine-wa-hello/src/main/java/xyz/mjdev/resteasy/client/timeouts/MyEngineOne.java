package xyz.mjdev.resteasy.client.timeouts;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.Configurable;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HttpContext;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient4Engine;
import org.jboss.resteasy.client.jaxrs.i18n.Messages;
import org.jboss.resteasy.client.jaxrs.internal.ClientInvocation;

import javax.ws.rs.ProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class MyEngineOne extends ApacheHttpClient4Engine {
	public MyEngineOne() {
		super();
	}

	public MyEngineOne(HttpHost defaultProxy) {
		super(defaultProxy);
	}

	public MyEngineOne(HttpClient httpClient) {
		super(httpClient);
	}

	public MyEngineOne(HttpClient httpClient, boolean closeHttpClient) {
		super(httpClient, closeHttpClient);
	}

	public MyEngineOne(HttpClient httpClient, HttpContext httpContext) {
		super(httpClient, httpContext);
	}

	@Override
	protected HttpClient createDefaultHttpClient() {
		final HttpClientBuilder builder = HttpClientBuilder.create();
		RequestConfig.Builder requestBuilder = RequestConfig.custom();
		if (defaultProxy != null) {
			requestBuilder.setProxy(defaultProxy);
		}
		builder.disableContentCompression();
		builder.setDefaultRequestConfig(requestBuilder.build());
		return builder.build();
	}

	@Override
	public HttpHost getDefaultProxy() {
		Configurable clientConfiguration = (Configurable) httpClient;
		return clientConfiguration.getConfig().getProxy();
	}

	@Override
	protected void setRedirectRequired(final ClientInvocation request, final HttpRequestBase httpMethod) {
		RequestConfig.Builder requestBuilder = RequestConfig.copy(getCurrentConfiguration(request, httpMethod));
		requestBuilder.setRedirectsEnabled(true);
		httpMethod.setConfig(requestBuilder.build());
	}

	@Override
	protected void setRedirectNotRequired(final ClientInvocation request, final HttpRequestBase httpMethod) {
		RequestConfig.Builder requestBuilder = RequestConfig.copy(getCurrentConfiguration(request, httpMethod));
		requestBuilder.setRedirectsEnabled(false);
		httpMethod.setConfig(requestBuilder.build());
	}

	private RequestConfig getCurrentConfiguration(final ClientInvocation request,
			final HttpRequestBase httpMethod) {
		RequestConfig baseConfig;
		if (httpMethod != null && httpMethod.getConfig() != null) {
			baseConfig = httpMethod.getConfig();
		}
		else {
			MyEngineOne engine =
					((MyEngineOne) request.getClient().httpEngine());
			baseConfig = ((Configurable) engine.getHttpClient()).getConfig();
			if (baseConfig == null) {
				Configurable clientConfiguration = (Configurable) httpClient;
				baseConfig = clientConfiguration.getConfig();
			}
		}
		return baseConfig;
	}

	@Override
	protected void loadHttpMethod(final ClientInvocation request, HttpRequestBase httpMethod) throws Exception {
		if (isRedirectRequired(request, httpMethod)) {
			setRedirectRequired(request, httpMethod);
		}
		else {
			setRedirectNotRequired(request, httpMethod);
		}

		if (request.getEntity() != null) {
			if (httpMethod instanceof HttpGet)
				throw new ProcessingException(Messages.MESSAGES.getRequestCannotHaveBody());

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			request.getDelegatingOutputStream().setDelegate(baos);
			try {
				HttpEntity entity = buildEntity(request);
				HttpPost post = (HttpPost) httpMethod;
				commitHeaders(request, httpMethod);
				post.setEntity(entity);
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		else {
			commitHeaders(request, httpMethod);
		}
	}

	@Override
	protected boolean isRedirectRequired(final ClientInvocation request, final HttpRequestBase httpMethod) {
		if (httpMethod instanceof HttpGet && false) {
			return true;
		}
		return false;
	}
}
