package com.madibasoft.messaging.smtp.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.madibasoft.messaging.smtp.Link;
import com.madibasoft.messaging.smtp.ResolvedLink;
import com.madibasoft.messaging.smtp.user.UserNotFoundException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DbTest {
	private static final Logger log = LoggerFactory.getLogger(DbTest.class);
	private static DbInterface db;

	private static String uidA = "001";
	private static String uidB = "002";

	@BeforeAll
	static void setUp() {
		try {
			db = DbFactory.getDatabase();
			db.start();
			db.clear();

		} catch (Throwable t) {
			t.printStackTrace();
		}
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
	public void createAndDeleteTest() throws Exception {

		assertFalse(db.containsByUid(uidA, uidB));

		Link r = db.setLink(new Link(uidA, uidB));
		assertTrue(db.containsByUid(uidA, uidB));
//		assertNotNull(userService.getProxyAByEmail(clearA, clearB));
//		assertNotSame(userService.getProxyAByEmail(clearA, clearB), userService.getProxyBByEmail(clearA, clearB));
		assertTrue(r.isValidLink());
		log.info("getValidity {}", r.getExpiry());
		log.info("isValid {}", r.isValidLink());
//		log.info("getToProxy {}", userService.getProxyAByEmail(clearA, clearB));
//		log.info("getFromProxy {}", userService.getProxyBByEmail(clearA, clearB));

		db.deleteLink(r);
		assertFalse(db.containsByUid(uidA, uidB));
//		try {
//			db.getMaskedLink(clearA, clearB);
//			fail("Should not be a valid link");
//		} catch (Throwable t) {
		// got expected exception
//		}
//		assertFalse(r2.isValidLink());
	}

	@Test
	public void linkLogicTest() throws Exception {
		// first set up a valid link
		Link r = db.setLink(new Link(uidA, uidB));
		log.debug("Created link {}", r.toString());

		// now check that toAddr can send mail to fromProxy
		log.info("1. Checking that {} can send mail to {}", uidA, uidB);
		assertTrue(db.acceptUid(uidA, uidB), "Could not accept between " + uidA + " and " + uidB);
		// now check fromAddr can contact toProxy
		log.info("2. Checking that {} can send mail to {}", uidB, uidA);
		assertTrue(db.acceptUid(uidB, uidA), "Could not accept between " + uidB + " and " + uidA);
	}

	@Test
	void testResolveAndAccept() throws Exception {
		ResolvedLink r = new ResolvedLink(db.setLink(new Link(uidA, uidB)));
		assertTrue(r.isValidLink());

		String obscuredA = r.getProxyA();
		String obscuredB = r.getProxyB();

		assertTrue(db.acceptUid(uidA, uidB));
		assertTrue(db.acceptUid(uidB, uidA));
		assertFalse(db.acceptUid(obscuredA, uidB));
		assertFalse(db.acceptUid(obscuredB, uidA));
		db.deleteLink(r);
		assertFalse(db.acceptUid(uidA, uidB));
		assertFalse(db.acceptUid(uidB, uidA));
		assertFalse(db.acceptUid(uidB, uidB));
		assertFalse(db.acceptUid(uidA, uidA));
	}

	@Test
	/*
	 * Make sure duplicates overwrite the existing modified, and do not generate new
	 * link hashes
	 */
	void testUpdate() throws InterruptedException, UserNotFoundException {
		assertFalse(db.containsByUid(uidA, uidB));
		assertFalse(db.containsByUid(uidB, uidA));
		Link r1 = db.setLink(new Link(uidA, uidB));
		assertTrue(db.containsByUid(uidA, uidB));
		assertTrue(db.containsByUid(uidB, uidA));

		assertNotNull(r1.getModified());
		Thread.sleep(500);
		Link r2 = db.setLink(new Link(uidA, uidB));
		assertNotNull(r2.getModified());

		assertEquals(r1.getUidA(), r2.getUidA());
		assertNotEquals(r1.getModified(), r2.getModified());
	}

	@Test
	public void testDoubleStart() throws Exception {
		try {
			db.start();
			fail("Should not allow double start");
		} catch (RuntimeException rte) {
			// expected
		}
	}

	@Test
	void testBadLink() {
		assertNull(db.getLinkByUid("531245", "734556"));
	}

	@Test
	void testSetClearTest() throws Exception {
		Link r1 = db.setLink(new Link(uidA, uidB));
		assertTrue(db.containsByUid(uidA, uidB));
		db.deleteLink(r1);
		assertFalse(db.containsByUid(uidA, uidB));
	}
}
