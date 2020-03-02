package com.madibasoft.messaging.smtp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.madibasoft.messaging.smtp.Config;
import com.madibasoft.messaging.smtp.Config.ForwardingType;
import com.madibasoft.messaging.smtp.Config.SmtpOutType;

class ConfigTest {
	private static final Logger log = LoggerFactory.getLogger(ConfigTest.class);
	private Config config;

	@BeforeEach
	void setUp() throws Exception {
		config = Config.getInstance();
		config.setString(Config.MAILGUARD_SMTP_OUT_TYPE, "dummy");
	}

	@AfterEach
	void tearDown() throws Exception {
	}

	@Test
	public void testConfig() {
		List<String> list = new ArrayList<String>();
		list.add("a");
		list.add("b");
		list.add("c");
		config.setStringList("key", list);
		assertEquals(list.size(), config.getStringList("key").size());
		config.setStringList("key", new ArrayList<String>());
		assertEquals(0, config.getStringList("key").size());
	}

	@Test
	public void testLists() {
		assertTrue(config.getStringList(Config.MAILGUARD_SMTP_IN_ACCEPT).size() > 0);
		log.info("List is {}", config.getStringList(Config.MAILGUARD_SMTP_IN_ACCEPT).toString());
		log.info("Host is {}", config.getString(Config.MAILGUARD_HTTP_HOST));
		assertEquals(3, Config.toList("a,b,c").size());
		assertTrue(config.getStringList(Config.MAILGUARD_SMTP_IN_ACCEPT).toString()
				.contains(config.getString(Config.MAILGUARD_HTTP_HOST)));
		assertNotNull(Config.toString(config.getStringList(Config.MAILGUARD_SMTP_IN_ACCEPT)));
		assertNotNull(config.toString());
	}

	@Test
	public void testSet() {
		config.setString("xxx", "yyy");
		assertEquals("yyy", config.getString("xxx"));
	}

	@Test
	public void testEmptyList() {
		config.setString("xxx", "yyy");
		assertEquals("", Config.toString(new ArrayList<String>()));
	}

	@Test
	public void testSmtpOutType() {
		assertEquals(SmtpOutType.dummy, config.getSmtpOutType());
	}

	@Test
	public void testForwardingType() {
		assertEquals(ForwardingType.both, config.getForwardingType());
	}

}
