package com.madibasoft.messaging.smtp.ws.link;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Date;
import java.util.HashMap;

import javax.mail.internet.MimeMessage;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.madibasoft.messaging.smtp.Config;
import com.madibasoft.messaging.smtp.Inbox;
import com.madibasoft.messaging.smtp.Link;
import com.madibasoft.messaging.smtp.MailUtils;
import com.madibasoft.messaging.smtp.ResolvedLink;
import com.madibasoft.messaging.smtp.db.DbFactory;
import com.madibasoft.messaging.smtp.db.DbInterface;
import com.madibasoft.messaging.smtp.user.UserServiceFactory;

import spark.utils.IOUtils;

public class ListServiceTest {
	private static final Logger log = LoggerFactory.getLogger(ListServiceTest.class);
	private static ListService rs;
	private static DbInterface db;

	@BeforeAll
	static void setUp() throws Exception {
		Config.getInstance().setString(Config.MAILGUARD_SMTP_OUT_TYPE, "dummy");
		db = DbFactory.getDatabase();
		db.start();
		db.clear();
		rs = new ListService(db);
	}

	@AfterAll
	static void tearDown() throws Exception {
		db.stop();
	}

	@Test
	void testListNull() throws Exception {
		String toUid = "001";
		String fromUid = "002";
		db.clear();
		try {
			db.setLink(new Link(toUid, fromUid));
		} catch (Throwable e) {
			log.error("Something bad happened", e);
			fail("Oh no");
		}
		JsonArray results = rs.invoke(new HashMap<String, String[]>(), "").getAsJsonArray("result");
		assertEquals(1, results.size());
	}

	@Test
	void testListWithParam() throws Exception {
		String toUid = "001";
		String fromUid = "002";
		try {
			db.setLink(new Link(toUid, fromUid));
		} catch (Throwable e) {
			log.error("Something bad happened", e);
			fail("Oh no");
		}
		JsonArray results = rs.list(toUid, 0, 10).getAsJsonArray("result");
		assertEquals(1, results.size());
		results = rs.list(fromUid, 0, 10).getAsJsonArray("result");
		assertEquals(1, results.size());
		log.info(results.toString());
	}

	@Test
	void testListWithBadParam() throws Exception {
		String toUid = "001";
		String fromUid = "002";

		JsonArray results = rs.list(toUid, 0, 10).getAsJsonArray("result");
		assertEquals(0, results.size());
		results = rs.list(fromUid, 0, 10).getAsJsonArray("result");
		assertEquals(0, results.size());
		log.info(results.toString());
	}

	@Test
	void testStress() throws Exception {
//		UserServiceFactory.setInstance("com.madibasoft.messaging.smtp.user.RandomUserService");
		String toUid = "000";
		String fromUid = "110";
		int max = 10;
		MimeMessage mail = MailUtils.loadEmail(getClass().getResource("/mail.txt").openStream());
		Inbox inbox = new Inbox(DbFactory.getDatabase());
		Date start = new Date();
		for (int i = 1; i < max; i++) {
//			String toEmail = UserServiceFactory.getInstance().lookupEmailByUid(toUid + i);
//			String fromEmail = UserServiceFactory.getInstance().lookupEmailByUid(fromUid + i);
			try {
				db.setLink(new Link(toUid, fromUid));
			} catch (Throwable e) {
				log.error("Something bad happened", e);
				fail("Oh no");
			}
			if ((i % 100) == 0) {
				Link r = db.getLink(new Link(toUid, fromUid));
				ResolvedLink rl = new ResolvedLink(r);
				assertTrue(db.containsLink(r));
				assertTrue(db.acceptUid(rl.getUidA(), rl.getUidB()));
				assertTrue(db.acceptUid(rl.getUidB(), rl.getUidA()));
				Date end = new Date();
				// send an email from A to B
				MailUtils.populateMimeMessage(rl.getProxyA(), rl.getClearB(), mail);
				assertTrue(inbox.acceptUid(rl.getUidA(), rl.getUidB()));
				inbox.messageArrived(null, rl.getClearA(), rl.getProxyB(),
						IOUtils.toByteArray(MailUtils.saveEmail(mail)));
				// send an email from B to A
				MailUtils.populateMimeMessage(rl.getProxyB(), rl.getClearA(), mail);
				assertTrue(inbox.acceptUid(rl.getUidB(), rl.getUidA()));
				inbox.messageArrived(null, rl.getClearB(), rl.getProxyA(),
						IOUtils.toByteArray(MailUtils.saveEmail(mail)));
				log.info("{} links took {}ms/link", i, (end.getTime() - start.getTime()) / 1000);
				start = new Date();
			}
		}
		UserServiceFactory.setInstance(Config.getInstance().getString(Config.MAILGUARD_USERSERVICE));
		db.clear();
	}

}
