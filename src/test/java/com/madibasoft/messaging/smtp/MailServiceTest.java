package com.madibasoft.messaging.smtp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.mail.internet.MimeMessage;

import com.madibasoft.messaging.smtp.db.DbFactory;
import com.madibasoft.messaging.smtp.db.DbInterface;
import com.madibasoft.messaging.smtp.ws.message.ListService;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spark.utils.IOUtils;

public class MailServiceTest {
	private static final Logger log = LoggerFactory.getLogger(MailServiceTest.class);
	private static ListService ms;
	private static DbInterface db;
	private static Inbox inbox;

	@BeforeAll
	static void setUp() throws Exception {
		log.info("Clearing db");
		db = DbFactory.getDatabase();
		Config.getInstance().setString(Config.MAILGUARD_SMTP_OUT_TYPE, "dummy");
		inbox = new Inbox(db);
		ms = new ListService(db);
		db.start();
	}

	@AfterAll
	static void tearDown() throws Exception {
		db.stop();
	}

	@BeforeEach
	void beforeEach() throws Exception {
		assertEquals(Config.SmtpOutType.dummy, Config.getInstance().getSmtpOutType());
		db.clear();
	}

	@Test
	public void testList() throws Exception {
		log.info("{}", ms.list(0, 10));
		assertEquals(0, ms.list(0, 10).get("result").getAsJsonArray().size());

		// first set up a valid link
		ResolvedLink r = new ResolvedLink(inbox.getDb().setLink(new Link("001", "002")));

		MimeMessage mail = MailUtils.loadEmail(getClass().getResource("/mail.txt").openStream());

		inbox.messageArrived(null, r.getClearA(), r.getProxyB(), IOUtils.toByteArray(MailUtils.saveEmail(mail)));

		Thread.sleep(2000);
		assertEquals(1, ms.list(0, 10).size());
		// log.info("{}", ms.list(0, 10));
	}

}
