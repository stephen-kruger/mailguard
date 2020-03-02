package com.madibasoft.messaging.smtp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.madibasoft.messaging.smtp.Config;
import com.madibasoft.messaging.smtp.Link;
import com.madibasoft.messaging.smtp.ResolvedLink;
import com.madibasoft.messaging.smtp.db.DbFactory;
import com.madibasoft.messaging.smtp.db.DbInterface;
import com.madibasoft.messaging.smtp.ws.link.ExpireService;
import com.madibasoft.messaging.smtp.ws.link.SetService;

public class LogicTest {
	private static final Logger log = LoggerFactory.getLogger(LogicTest.class);
	private static DbInterface db;
	private static SetService rs;
	private static ExpireService es;

	@BeforeAll
	static void setUp() throws Exception {
		log.info("Setup");
		Config.getInstance().setString(Config.MAILGUARD_SMTP_OUT_TYPE, "dummy");
		db = DbFactory.getDatabase();
		rs = new SetService(db);
		es = new ExpireService(db);
	}

	@AfterAll
	static void tearDown() throws Exception {
		db.stop();
	}

	@Test
	public void testReEnableConsistency() throws Exception {
		Link r = db.setLink(new Link("001", "002"));
		Thread.sleep(2000);
		ResolvedLink rl = new ResolvedLink(r);

		assertTrue(db.containsLink(r));
		es.expire("002", "001");
		Link rExpired = db.getLink(r);
		assertFalse(rExpired.isValidLink());

		assertTrue(db.acceptUid(r.getUidA(), r.getUidB()));
		assertTrue(db.acceptUid(r.getUidB(), r.getUidA()));
		assertFalse(db.acceptUid(r.getUidA(), r.getUidA()));
		assertFalse(db.acceptUid(r.getUidB(), r.getUidB()));

		rs.set("002", "001");
		assertTrue(db.acceptUid("002", "001"));
		assertTrue(db.acceptUid("001", "002"));
		Link r5 = db.getLink(rl);
		assertTrue(r5.isValidLink());

		// now check hashes are still the same
		ResolvedLink r2 = new ResolvedLink(db.getLink(rl));
		r2.swop(rl.getClearA());
		assertEquals(rl.getClearA(), r2.getClearA());
		assertEquals(rl.getClearB(), r2.getClearB());
		assertEquals(rl.getProxyB(), r2.getProxyB());
		assertEquals(rl.getProxyA(), r2.getProxyA());

		// and check the other direction
		ResolvedLink r3 = new ResolvedLink(db.getLink(rl));
		r3.swop(rl.getClearA());
		assertEquals(rl.getClearA(), r3.getClearA());
		assertEquals(rl.getClearB(), r3.getClearB());
		assertEquals(rl.getProxyB(), r3.getProxyB());
		assertEquals(rl.getProxyA(), r3.getProxyA());
	}

}
