package com.madibasoft.messaging.smtp.ws.link;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.security.InvalidParameterException;

import com.madibasoft.messaging.smtp.Link;
import com.madibasoft.messaging.smtp.MissingParameterException;
import com.madibasoft.messaging.smtp.db.DbFactory;
import com.madibasoft.messaging.smtp.db.DbInterface;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExpireServiceTest {
	private static final Logger log = LoggerFactory.getLogger(ExpireServiceTest.class);
	private static ExpireService es;
	private static SetService ss;
	private static DbInterface db;

	@BeforeAll
	static void setUp() throws Exception {
		db = DbFactory.getDatabase();
		es = new ExpireService(db);
		ss = new SetService(db);
		db.start();
	}

	@AfterAll
	static void tearDown() throws Exception {
		db.stop();
	}

	@BeforeEach
	public void setUpTest() throws Exception {
		db.clear();
	}

	@AfterEach
	public void tearDownTest() throws Exception {
		db.clear();
	}

	@Test
	void testExpireNullId() throws Exception {
		log.info("Test expired link");
		String toUid = null;
		String fromUid = null;
		try {
			es.expire(toUid, "001");
			fail("We expect a MissingParameterException");
		} catch (MissingParameterException enf) {
			// good
		}
		try {
			es.expire("001", fromUid);
			fail("We expect a MissingParameterException");
		} catch (MissingParameterException enf) {
			// good
		}
	}

	@Test
	void testExpireBadId() throws Exception {
		String toUid = "999999";
		String fromUid = "888888";
		try {
			es.expire(toUid, fromUid);
			fail("We expect a UserNotFoundException");
		} catch (InvalidParameterException enf) {
			// good
		}
		try {
			es.expire(toUid, fromUid);
			fail("We expect a UserNotFoundException");
		} catch (InvalidParameterException enf) {
			// good
		}
	}

	@Test
	void testSameId() throws Exception {
		String toUid = "001";
		String fromUid = "001";
		try {
			es.expire(toUid, fromUid);
			fail("We expect a UserNotFoundException");
		} catch (InvalidParameterException enf) {
			// good
		}
		try {
			es.expire(fromUid, toUid);
			fail("We expect a UserNotFoundException");
		} catch (InvalidParameterException enf) {
			// good
		}
	}

	@Test
	void testExpireBadLink() throws Exception {
		String toUid = "001";
		String fromUid = "002";
		try {
			es.expire(toUid, fromUid);
			fail("We expect a UserNotFoundException");
		} catch (InvalidParameterException enf) {
			// good
		}
		try {
			es.expire(toUid, fromUid);
			fail("We expect a UserNotFoundException");
		} catch (InvalidParameterException enf) {
			// good
		}
		db.setLink(new Link(toUid, fromUid));
		// should not have n exception here
		es.expire(toUid, fromUid);
	}

	@Test
	void testExpireLink() throws Exception {
		String toUid = "001";
		String fromUid = "002";
		Link link = new Link(toUid, fromUid);
		link = db.getLink(link);
		try {
			assertNull(link);
			ss.set(toUid, fromUid);
			link = db.getLink(new Link(toUid, fromUid));
			assertTrue(link.isValidLink());
			es.expire(toUid, fromUid);
			link = db.getLink(new Link(toUid, fromUid));
			assertFalse(link.isValidLink());
		} catch (InvalidParameterException enf) {
			fail("Unexpected failure");
		}
	}

}
