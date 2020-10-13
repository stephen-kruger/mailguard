package com.madibasoft.messaging.smtp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.madibasoft.messaging.smtp.db.DbFactory;
import com.madibasoft.messaging.smtp.user.UserNotFoundException;
import com.madibasoft.messaging.smtp.user.UserServiceImpl;
import com.madibasoft.messaging.smtp.user.UserServiceInterface;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinkTest {
	private static Inbox inbox;
	private static final Logger log = LoggerFactory.getLogger(LinkTest.class);
	private UserServiceInterface us = new UserServiceImpl();

	@BeforeAll
	static void setUp() throws Exception {
		try {
			inbox = new Inbox(DbFactory.getDatabase());
			inbox.getDb().clear();
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	@AfterAll
	static void tearDown() throws Exception {
		inbox.getDb().stop();
	}

	@Test
	public void linkLogicTest() throws Exception {
		if (inbox == null) {
			fail("Test setup was not run - use newer versions of Gradle or Maven for JUnit5");
		}
		String uidA = "001";
		String uidB = "002";
		String clearA = us.lookupEmailByUid(uidA);
		String clearB = us.lookupEmailByUid(uidB);
		String proxyA = us.lookupProxyByUid(uidA);
		String proxyB = us.lookupProxyByUid(uidB);

		// first set up a valid link
		Link r = inbox.getDb().setLink(new Link(uidA, uidB));
		log.info("Created link {}", r.toString());

		// now check that toAddr can send mail to fromHash
		log.info("1. Checking that {} can send mail to {}", clearA, proxyB);
		assertTrue(inbox.getDb().acceptUid(uidA, uidB), "No link found");
		// now check fromAddr can contact toHash
		log.info("2. Checking that {} can send mail to {}", clearB, proxyA);
		assertTrue(inbox.getDb().acceptUid(uidB, uidA));

		// make sure we do not forward direct mails
		log.info("2. Checking that {} can not send mail to {}", clearA, clearB);
		assertFalse(inbox.acceptEmail(clearA, clearB, uidA, uidB));
		assertFalse(inbox.acceptEmail(clearB, clearA, uidB, uidA));

		// we should never receive mails from the obscured addresses
		assertFalse(inbox.acceptEmail(proxyA, clearA, uidA, uidA));
		assertFalse(inbox.acceptEmail(proxyB, clearB, uidB, uidB));
	}

	@Test
	void testRelation() throws UserNotFoundException {
		Link t = inbox.getDb().setLink(new Link("001", "002"));
		assertNotNull(t.getUidA());
		assertNotNull(t.getUidB());
		assertNotNull(t.getExpiry());
		assertNotNull(t.getCreated());
		assertNotNull(t.getModified());
		assertNotNull(t.toString());
		log.info(t.toString());
	}

	@Test
	void testDomains() {

		assertFalse(MailUtils.isValidDomain("sender1@mail.com"));
		for (String domain : Config.getInstance().getStringList(Config.MAILGUARD_SMTP_IN_ACCEPT)) {
			assertTrue(MailUtils.isValidDomain("sender1@" + domain));
		}
	}

	@Test
	void testLinkCreation() throws UserNotFoundException {

		String toUid = "001";
		String fromUid = "002";

		Link r1 = inbox.getDb().setLink(new Link(toUid, fromUid));
		ResolvedLink rl1 = new ResolvedLink(r1);
		Link r2 = inbox.getDb().getLink(new Link(toUid, fromUid));
		ResolvedLink rl2 = new ResolvedLink(r2);
		Link r3 = inbox.getDb().getLink(new Link(fromUid, toUid));
		ResolvedLink rl3 = new ResolvedLink(r3);

		log.info("{}", r1);
		log.info("{}", r2);
		log.info("{}", r3);

		assertEquals(rl1.getClearA(), rl2.getClearA());
		assertEquals(rl1.getClearB(), rl2.getClearB());
		assertEquals(rl1.getProxyA(), rl2.getProxyA());
		assertEquals(rl1.getProxyB(), rl2.getProxyB());

		assertEquals(rl1.getClearA(), rl3.getClearA());
		assertEquals(rl1.getClearB(), rl3.getClearB());
		assertEquals(rl1.getProxyA(), rl3.getProxyA());
		assertEquals(rl1.getProxyB(), rl3.getProxyB());
	}

	@Test
	void testDirectionalRelation() throws UserNotFoundException {
		Link t = inbox.getDb().setLink(new Link("001", "002"));
		log.info("t={}", t);
		ResolvedLink rt = new ResolvedLink(t);
		log.info("rt={}", rt);

		Link test = inbox.getDb().getLink(new Link(rt.getUidA(), rt.getUidB()));
		log.info("test={}", test);
		ResolvedLink rtest = new ResolvedLink(test);
		assertEquals(rt.getClearA(), rtest.getClearA());
		assertEquals(rt.getClearB(), rtest.getClearB());
		assertEquals(rt.getProxyB(), rtest.getProxyB());
		assertEquals(rt.getProxyA(), rtest.getProxyA());

		Link test2 = inbox.getDb().getLink(new Link(rt.getUidB(), rt.getUidA()));
		ResolvedLink rtest2 = new ResolvedLink(test2);
		assertTrue((rt.getClearA().equals(rtest2.getClearA())) || (rt.getClearA().equals(rtest2.getClearB())));
		assertTrue((rt.getClearB().equals(rtest2.getClearB())) || (rt.getClearB().equals(rtest2.getClearA())));
		assertTrue((rt.getProxyB().equals(rtest2.getProxyB())) || (rt.getProxyB().equals(rtest2.getProxyA())));
		assertTrue((rt.getProxyA().equals(rtest2.getProxyA())) || (rt.getProxyA().equals(rtest2.getProxyB())));
	}

}
