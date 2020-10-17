package com.madibasoft.messaging.smtp.ws.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.security.InvalidParameterException;

import javax.mail.Message.RecipientType;
import javax.mail.internet.MimeMessage;

import com.google.gson.JsonObject;
import com.madibasoft.messaging.smtp.Config;
import com.madibasoft.messaging.smtp.Inbox;
import com.madibasoft.messaging.smtp.Link;
import com.madibasoft.messaging.smtp.MailUtils;
import com.madibasoft.messaging.smtp.MissingParameterException;
import com.madibasoft.messaging.smtp.ResolvedLink;
import com.madibasoft.messaging.smtp.db.DbFactory;
import com.madibasoft.messaging.smtp.db.DbInterface;
import com.madibasoft.messaging.smtp.user.UserNotFoundException;
import com.madibasoft.messaging.smtp.user.UserServiceFactory;
import com.madibasoft.messaging.smtp.user.UserServiceInterface;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SendServiceTest {
	private static final Logger log = LoggerFactory.getLogger(SendServiceTest.class);
	private static SendService ss;
	private static DbInterface db;

	@BeforeAll
	static void setUp() throws Exception {
		Config.getInstance().setString(Config.MAILGUARD_SMTP_OUT_TYPE, "dummy");
		db = DbFactory.getDatabase();
		ss = new SendService(db);
		db.start();
		log.info("Completed test setup");
	}

	@AfterAll
	static void tearDown() throws Exception {
		db.stop();
		log.info("Completed test teardown");
	}

	@BeforeEach
	void beforeEach() throws Exception {
		db.clear();
		MailUtils.getInstance().getDeliveredMails().clear();
	}

	@Test
	void testSendBadId() throws Exception {
		String toUid = "999999";
		String fromUid = "888888";
		String subject = "This is the email subject";
		String body = "This is an email body";
		try {
			ss.send(toUid, fromUid, subject, body);
			fail("We expect a UserNotFoundException");
		} catch (UserNotFoundException enf) {
			// good
		}
	}

	@Test
	void testDoubleLink() throws Exception {
		String toUid = "001";
		String fromUid = "002";
		String subject = "This is the email subject";
		String body = "This is an email body";
		try {
			ss.send(toUid, fromUid, subject, body);
			Link r1 = db.getLink(new Link(toUid, fromUid));
			Link r2 = db.getLink(new Link(fromUid, toUid));
			assertEquals(r1.getUidA(), r2.getUidA());
			ss.send(fromUid, toUid, subject, body);
		} catch (Throwable e) {
			log.error("Something bad happened", e);
			fail("Oh no:" + e.getMessage());
		}
	}

	@Test
	void testSendBadParams() throws Exception {
		String toUid = "002200";
		String fromUid = "888888";
		String subject = "This is the email subject";
		String body = "This is an email body";
		try {
			ss.send(null, fromUid, subject, body);
			fail("We expect a MissingParameterException");
		} catch (MissingParameterException enf) {
			// good
		}
		try {
			ss.send(toUid, null, subject, body);
			fail("We expect a MissingParameterException");
		} catch (MissingParameterException enf) {
			// good
		}
		try {
			ss.send(null, fromUid, subject, body);
			fail("We expect a MissingParameterException");
		} catch (MissingParameterException enf) {
			// good
		}
		try {
			ss.send(toUid, fromUid, null, body);
			fail("We expect a MissingParameterException");
		} catch (MissingParameterException enf) {
			// good
		}
		try {
			ss.send(toUid, toUid, subject, body);
			fail("Cannot send to yourself");
		} catch (InvalidParameterException enf) {
			// good
		}
	}

	@Test
	void testStress() throws Exception {
		String toUid;
		String fromUid;
		String subject = "This is the email subject";
		String body = "This is an email body";
		for (int i = 0; i < 10; i++) {
			for (int j = 1; j < 5; j++) {
				toUid = "00" + j;
				fromUid = "00" + (j + 1);
				assertNotNull(ss.send(toUid, fromUid, subject, body));
			}
		}
	}

	@Test
	void testSingle() throws Exception {
		String toUid;
		String fromUid;
		String subject = "This is the email subject";
		String body = "This is an email body";
		toUid = "001";
		fromUid = "002";
		assertNotNull(ss.send(toUid, fromUid, subject, body));
	}

	@Test
	void testSendForward() throws Exception {
		String toUid = "001";
		String fromUid = "002";
		String subject = "This is the email subject";
		String body = "This is an email body";
		db.clear();
		try {
			// set up the link
			ss.send(toUid, fromUid, subject, body);
			ResolvedLink r1 = new ResolvedLink(new Link(toUid, fromUid));
			ResolvedLink r2 = new ResolvedLink(new Link(fromUid, toUid));

			Inbox inbox = new Inbox(db);
			assertTrue(inbox.acceptUid(r1.getUidA(), r1.getUidB()));
			assertTrue(inbox.acceptUid(r1.getUidB(), r1.getUidA()));

			assertTrue(inbox.acceptUid(r2.getUidA(), r2.getUidB()));
			assertTrue(inbox.acceptUid(r2.getUidB(), r2.getUidA()));
		} catch (Throwable e) {
			log.error("Something bad happened", e);
			fail("Oh no:" + e.getMessage());
		}
	}

	@Test
	void testSendToSelf() throws Exception {
		String toUid = "001";
		String fromUid = "001";
		String subject = "This is the email subject";
		String body = "This is an email body";
		db.clear();
		try {
			ss.send(toUid, fromUid, subject, body);
			fail("Expected a failure");
		} catch (Throwable e) {
			log.info("Caught bad params");
		}
	}

	@Test
	void testSendRecipient() throws Exception {
		UserServiceInterface us = UserServiceFactory.getInstance();
		final String toUid = "001";
		final String fromUid = "002";
		log.info("Sending mail to {} {}", toUid, us.lookupEmailByUid(toUid));
		log.info("Sending mail from {} {}", fromUid, us.lookupEmailByUid(fromUid));

		String subject = "This is the email subject";
		String body = "This is an email body";

		assertEquals(0, MailUtils.getInstance().getDeliveredMails().size());
		JsonObject r = ss.send(toUid, fromUid, subject, body);
		assertEquals(toUid, r.get("to").getAsString(), "Incorrect To detected");
		assertEquals(fromUid, r.get("from").getAsString(), "Incorrect From detected");
		Thread.sleep(2000);
		assertEquals(1, MailUtils.getInstance().getDeliveredMails().size());
		MimeMessage message = MailUtils.getInstance().getDeliveredMails().get(0);
		String toUserId = us.lookupUidByEmail(message.getRecipients(RecipientType.TO)[0].toString());
		assertEquals(toUid, toUserId, "Incorrect recipient uid");
		String fromUserId = us.lookupUidByProxy(message.getFrom()[0].toString());
		assertEquals(fromUid, fromUserId, "Incorrect sender uid");
	}

}
