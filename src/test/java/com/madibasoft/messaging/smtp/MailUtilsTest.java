package com.madibasoft.messaging.smtp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.Date;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.masukomi.aspirin.Aspirin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spark.utils.IOUtils;

class MailUtilsTest {
	private static final Logger log = LoggerFactory.getLogger(MailUtilsTest.class);

	@BeforeEach
	void setUp() throws Exception {
	}

	@AfterEach
	void tearDown() throws Exception {
		Aspirin.shutdown();
	}

	@Test
	void testLoadEmail() throws MessagingException, IOException {
		MimeMessage mail = MailUtils.loadEmail(getClass().getResource("/mail.txt").openStream());
		assertEquals("This is the subject", mail.getSubject());
		assertEquals(1, mail.getFrom().length);
		assertEquals("from@test.com", mail.getFrom()[0].toString());
		log.info("Message loaded ok");
	}

	@Test
	void testRealEmail() throws Exception {
		MimeMessage mail = MailUtils.loadEmail(getClass().getResource("/mail.txt").openStream());
		mail = MailUtils.populateMimeMessage("john.hancock@madibasoft.com", "john.hancock@madibasoft.com", mail);
		// AspirinInternal.addListener(this);
		MailUtils.getInstance().sendMail(mail);
	}

	@Test
	void testCleanEmail() throws Exception {
		String brokenEmail = "@invalidaddress.com";
		try {
			assertEquals(brokenEmail, MailUtils.cleanEmail(brokenEmail));
			fail("This should be invalid");
		} catch (Throwable t) {
			log.info("Expected exception was thrown");
		}
		// for some reason our Cisco systems mangle outgoing emails like this
		String ciscoMail = "prvs=17026f8f2=john.hancock@madibasoft.com";
		String cleaned = MailUtils.cleanEmail(ciscoMail);
		assertEquals("john.hancock@madibasoft.com", cleaned);
	}

	@Test
	void testInvalidDomains() throws Exception {
		log.info("Sending test mail");
		try {
			MimeMessage message = MailUtils.createMimeMessage("from@test.com", "john.hancock@somewhereelse.com",
					"This is the subject", "This is the body");
			MailUtils.getInstance().sendMail(message);
			fail("We should reject mails");
		} catch (Throwable t) {
			// good, we rejected the mail
		}
	}

	@Test
	public void testSendMail() {
		Config config = Config.getInstance();
		try {
			config.setString(Config.MAILGUARD_SMTP_OUT_TYPE, "dummy");
			assertEquals("dummy", config.getString(Config.MAILGUARD_SMTP_OUT_TYPE));
			MailUtils.getInstance().sendMail(null);
			fail("We should not get here");
		} catch (Exception e) {
			log.info("Expected fail");
		}
		try {
			config.setString(Config.MAILGUARD_SMTP_OUT_TYPE, "direct");
			assertEquals("direct", config.getString(Config.MAILGUARD_SMTP_OUT_TYPE));
			MailUtils.getInstance().sendMail(null);
			fail("We should not get here");
		} catch (Exception e) {
			log.info("Expected fail");
		}
		try {
			config.setString(Config.MAILGUARD_SMTP_OUT_TYPE, "smtp");
			assertEquals("smtp", config.getString(Config.MAILGUARD_SMTP_OUT_TYPE));
			MailUtils.getInstance().sendMail(null);
			fail("We should not get here");
		} catch (Exception e) {
			log.info("Expected fail");
		}
	}

	@Test
	public void testLoadUnload() throws MessagingException, IOException {
		MimeMessage a = MailUtils.loadEmail(getClass().getResource("/mail.txt").openStream());
		MimeMessage b = MailUtils.loadEmail(MailUtils.saveEmail(a));
		assertEquals(a.getSubject(), b.getSubject());
		assertEquals(a.getContent(), b.getContent());
		assertEquals(a.getFrom()[0], b.getFrom()[0]);

	}

	@Test
	public void testPhoneRemoval() {
		String text[] = { "this is my mobile +6597767899", "this is my mobile +65 9776 7899",
				"this is my mobile 9776 7899" };
		for (String str : text) {
			assertTrue(MailUtils.removePhoneNumbers(str).indexOf("7899") < 0);
		}
	}

	@Test
	public void testFalsePhoneRemoval() {
		String text[] = { "this is my postal code 123456", "this is my postal code 6001" };
		for (String str : text) {
			assertTrue(MailUtils.removePhoneNumbers(str).indexOf("7899") < 0);
		}
	}

	@Test
	public void testEmailRemoval() throws MessagingException, IOException {
		String original = "this is my email <a href=\"mailto:test1@email.com\" target=\"_blank\">test1@email.com</a>";
		String fixed = MailUtils.replaceEmails("test1@email.com", "hashed1@hashed.com", original);
		assertTrue(fixed.indexOf("test1@email.com") < 0);
		assertTrue(fixed.indexOf("hashed1@hashed.com") > 0);
		log.info("{}", fixed);
	}

	@Test
	public void testHtmlEmailRemoval() throws Exception {
		Config.getInstance().setString(Config.MAILGUARD_SMTP_OUT_TYPE, "dummy");
		MimeMessage message = MailUtils.loadEmail(getClass().getResource("/mail.txt").openStream());
		ResolvedLink r = new ResolvedLink(new Link("001", "002", new Date(), new Date(), new Date()));

		message = MailUtils.populateMimeMessage(r.getProxyB(), r.getClearA(), message);
		message = MailUtils.removePiiFromMimeMessage(r, message);
		String asString = MailUtils.getTextFromMimeMessage(message);
		assertEquals(-1, asString.indexOf(r.getClearA()));
		assertEquals(-1, asString.indexOf(r.getClearB()));
	}

	@Test
	public void testTextEmailRemoval() throws Exception {
		String message = IOUtils.toString(getClass().getResource("/mail.txt").openStream());
		ResolvedLink r = new ResolvedLink(new Link("001", "002", new Date(), new Date(), new Date()));
		message = MailUtils.replaceEmails(r, message);
		assertTrue(message.indexOf(r.getClearA()) < 0);
		assertTrue(message.indexOf(r.getClearB()) < 0);
	}

	@Test
	public void testRejectionMessage() throws Exception {
		MimeMessage message = MailUtils.loadEmail(getClass().getResource("/mail.txt").openStream());
		message.setSubject("");
		MailUtils.populateMimeRejectionMessage("test@test.com", "test@test.com", message);
		assertEquals(Config.getInstance().getString(Config.MAILGUARD_REJECTION_SUBJECT), message.getSubject());
	}

	@Test
	public void testPIITextRemoval() throws Exception {
		MimeMessage message = MailUtils.loadEmail(getClass().getResource("/mail.txt").openStream());
		ResolvedLink r = new ResolvedLink(new Link("001", "002", new Date(), new Date(), new Date()));
		message = MailUtils.populateMimeMessage(r.getProxyB(), r.getClearA(), message);
		message = MailUtils.removePiiFromMimeMessage(r, message);
		String asString = MailUtils.getTextFromMimeMessage(message);
		assertEquals(-1, asString.indexOf(r.getClearA()));
		assertEquals(-1, asString.indexOf(r.getClearB()));
	}

	@Test
	public void testAnonymiseEmail() {
		String email = "steve@here.com";
		String cleaned = MailUtils.anonymiseLog(email);
		log.info("Cleaned:{}", cleaned);
	}
}
