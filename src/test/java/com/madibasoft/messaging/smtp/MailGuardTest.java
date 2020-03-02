package com.madibasoft.messaging.smtp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.HttpResponse;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spark.utils.IOUtils;

class MailGuardTest {
	private static final Logger log = LoggerFactory.getLogger(MessageHandlerFactoryImpl.class);
	private static MailGuard smtpServer;

	@BeforeAll
	static void setUp() throws Exception {
		try {
			smtpServer = new MailGuard(new MessageHandlerFactoryImpl(new Inbox(), 10000));
			smtpServer.start();
			smtpServer.getDb().clear();
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	@AfterAll
	static void tearDown() throws Exception {
		smtpServer.stop();
	}

	@Test
	void test404() throws Exception {
		HttpUriRequest request = new HttpGet("http://" + Config.getInstance().getString(Config.MAILGUARD_HTTP_HOST)
				+ ":" + Config.getInstance().getInt(Config.MAILGUARD_HTTP_PORT) + "/xxx?name=something");
		request.setHeader(Config.MAILGUARD_HTTP_SECRET, Config.getInstance().getString(Config.MAILGUARD_HTTP_SECRET));
		log.info("Connecting to {}", request.getUri().toString());
		HttpResponse response = HttpClientBuilder.create().build().execute(request);
		assertEquals(HttpStatus.NOT_FOUND_404, response.getCode());
	}

	@Test
	void testBadParam() throws Exception {
		HttpUriRequest request = new HttpPost("http://" + Config.getInstance().getString(Config.MAILGUARD_HTTP_HOST)
				+ ":" + Config.getInstance().getInt(Config.MAILGUARD_HTTP_PORT) + "/message/send?name=something");
		request.setHeader(Config.MAILGUARD_HTTP_SECRET, Config.getInstance().getString(Config.MAILGUARD_HTTP_SECRET));
		log.info("Connecting to {}", request.getUri().toString());
		HttpResponse response = HttpClientBuilder.create().build().execute(request);
		assertEquals(HttpStatus.BAD_REQUEST_400, response.getCode());
	}

	@Test
	void testBadUser() throws Exception {
		HttpUriRequest request = new HttpPost("http://" + Config.getInstance().getString(Config.MAILGUARD_HTTP_HOST)
				+ ":" + Config.getInstance().getInt(Config.MAILGUARD_HTTP_PORT)
				+ "/message/send?to=something&from=xxxx&subject=xxx");
		request.setHeader(Config.MAILGUARD_HTTP_SECRET, Config.getInstance().getString(Config.MAILGUARD_HTTP_SECRET));

		log.info("Connecting to {}", request.getUri().toString());
		HttpResponse response = HttpClientBuilder.create().build().execute(request);
		assertEquals(HttpStatus.BAD_REQUEST_400, response.getCode());
	}

	@Test
	void testList() throws Exception {
		HttpUriRequest request = new HttpGet("http://" + Config.getInstance().getString(Config.MAILGUARD_HTTP_HOST)
				+ ":" + Config.getInstance().getInt(Config.MAILGUARD_HTTP_PORT) + "/link/list");
		request.setHeader(Config.MAILGUARD_HTTP_SECRET, Config.getInstance().getString(Config.MAILGUARD_HTTP_SECRET));

		log.info("Connecting to {}", request.getUri().toString());
		CloseableHttpResponse response = HttpClientBuilder.create().build().execute(request);
		assertEquals(HttpStatus.OK_200, response.getCode());
		log.info("{}", IOUtils.toString(response.getEntity().getContent()));
	}

	@Test
	void testMailList() throws Exception {
		HttpUriRequest request = new HttpGet("http://" + Config.getInstance().getString(Config.MAILGUARD_HTTP_HOST)
				+ ":" + Config.getInstance().getInt(Config.MAILGUARD_HTTP_PORT) + "/message/list");
		request.setHeader(Config.MAILGUARD_HTTP_SECRET, Config.getInstance().getString(Config.MAILGUARD_HTTP_SECRET));

		log.info("Connecting to {}", request.getUri().toString());
		CloseableHttpResponse response = HttpClientBuilder.create().build().execute(request);
		assertEquals(HttpStatus.OK_200, response.getCode());
		log.info("{}", IOUtils.toString(response.getEntity().getContent()));
	}

	@Test
	void testExpireNonExistent() throws Exception {
		HttpUriRequest request = new HttpDelete("http://" + Config.getInstance().getString(Config.MAILGUARD_HTTP_HOST)
				+ ":" + Config.getInstance().getInt(Config.MAILGUARD_HTTP_PORT) + "/link/expire?to=001&from=002");
		request.setHeader(Config.MAILGUARD_HTTP_SECRET, Config.getInstance().getString(Config.MAILGUARD_HTTP_SECRET));

		log.info("Connecting to {}", request.getUri().toString());
		CloseableHttpResponse response = HttpClientBuilder.create().build().execute(request);
		assertEquals(HttpStatus.BAD_REQUEST_400, response.getCode());
		log.info("{}", IOUtils.toString(response.getEntity().getContent()));
	}

	@Test
	void testNoAuth() throws Exception {
		HttpUriRequest request = new HttpGet("http://" + Config.getInstance().getString(Config.MAILGUARD_HTTP_HOST)
				+ ":" + Config.getInstance().getInt(Config.MAILGUARD_HTTP_PORT) + "/link/list");
		log.info("Connecting to {}", request.getUri().toString());
		CloseableHttpResponse response = HttpClientBuilder.create().build().execute(request);
		assertEquals(HttpStatus.UNAUTHORIZED_401, response.getCode());
		log.info("{}", IOUtils.toString(response.getEntity().getContent()));
	}

	@Test
	void testbadAuth() throws Exception {
		HttpUriRequest request = new HttpGet("http://" + Config.getInstance().getString(Config.MAILGUARD_HTTP_HOST)
				+ ":" + Config.getInstance().getInt(Config.MAILGUARD_HTTP_PORT) + "/list");
		request.setHeader(Config.MAILGUARD_HTTP_SECRET, "badsecret");

		log.info("Connecting to {}", request.getUri().toString());
		CloseableHttpResponse response = HttpClientBuilder.create().build().execute(request);
		assertEquals(HttpStatus.UNAUTHORIZED_401, response.getCode());
		log.info("{}", IOUtils.toString(response.getEntity().getContent()));
	}

}
