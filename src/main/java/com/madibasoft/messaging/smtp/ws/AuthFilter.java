package com.madibasoft.messaging.smtp.ws;

import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.madibasoft.messaging.smtp.Config;

import spark.Filter;
import spark.Request;
import spark.Response;

public class AuthFilter implements Filter {
	private static final Logger log = LoggerFactory.getLogger(AuthFilter.class);
	private Config config = Config.getInstance();

	@Override
	public void handle(Request request, Response response) throws Exception {
		log.debug("Checking auth on request {}", request.pathInfo());
		if ((request.headers(Config.MAILGUARD_HTTP_SECRET) != null) && (request.headers(Config.MAILGUARD_HTTP_SECRET)
				.equals(config.getString(Config.MAILGUARD_HTTP_SECRET)))) {
			log.debug("Authentication accepted for {} ({})", request.pathInfo(), request.userAgent());
			// all good
		} else {
			log.warn("No authentication detected for {} from {}", request.pathInfo(), request.ip());
			spark.Spark.halt(HttpStatus.UNAUTHORIZED_401, "Not authorised");
		}
	}

}
