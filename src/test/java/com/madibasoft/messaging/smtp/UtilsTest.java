package com.madibasoft.messaging.smtp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class UtilsTest {
	private static final Logger log = LoggerFactory.getLogger(UtilsTest.class);

	@BeforeEach
	void setUp() throws Exception {
	}

	@AfterEach
	void tearDown() throws Exception {
	}

	@Test
	void testEncodeDecode() throws Exception {
		String input = "xxx";
		String key = "1234567890123456";
		log.info("Input: " + input);
		String output = Utils.encrypt2(input, key);
		log.info("Encrypted: " + output);
		String decoded = Utils.decrypt2(output, key);
		log.info("Decrypted: " + decoded);
		assertEquals(input, decoded);
	}

	@Test
	void testJsonStackTrace() throws Exception {
		Exception test = new Exception("This is the exception message");
		Utils.jsonError(log, "JUnit test", test);
	}
}
