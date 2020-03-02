package com.madibasoft.messaging.smtp.ws.link;

import static org.junit.jupiter.api.Assertions.fail;

import java.security.InvalidParameterException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.madibasoft.messaging.smtp.MissingParameterException;
import com.madibasoft.messaging.smtp.db.DbFactory;
import com.madibasoft.messaging.smtp.db.DbInterface;
import com.madibasoft.messaging.smtp.user.UserNotFoundException;
import com.madibasoft.messaging.smtp.ws.link.SetService;

public class SetServiceTest {
	private static final Logger log = LoggerFactory.getLogger(SetServiceTest.class);
	private static SetService rs;
	private static DbInterface db;

	@BeforeAll
	static void setUp() throws Exception {
		db = DbFactory.getDatabase();
		rs = new SetService(db);
		db.start();
		db.clear();

	}

	@AfterAll
	static void tearDown() throws Exception {
		db.stop();
	}

	@Test
	void testExpireNullId() throws Exception {
		log.info("Test set link");
		String toUid = null;
		String fromUid = null;
		try {
			rs.set(toUid, "001");
			fail("We expect a UserNotFoundException");
		} catch (MissingParameterException enf) {
			// good
		}
		try {
			rs.set("001", fromUid);
			fail("We expect a UserNotFoundException");
		} catch (MissingParameterException enf) {
			// good
		}
	}

	@Test
	void testExpireBadId() throws Exception {
		String toUid = "999999";
		String fromUid = "888888";
		try {
			rs.set("001", fromUid);
			fail("We expect a UserNotFoundException");
		} catch (UserNotFoundException enf) {
			// good
		}
		try {
			rs.set(toUid, "001");
			fail("We expect a UserNotFoundException");
		} catch (UserNotFoundException enf) {
			// good
		}
	}

	@Test
	void testSameId() throws Exception {
		String toUid = "001";
		String fromUid = "001";
		try {
			rs.set(toUid, fromUid);
			fail("We expect an InvalidParameterException");
		} catch (InvalidParameterException enf) {
			// good
		}
	}

}
