package com.madibasoft.messaging.smtp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subethamail.smtp.RejectException;

import com.madibasoft.messaging.smtp.Config;
import com.madibasoft.messaging.smtp.Inbox;
import com.madibasoft.messaging.smtp.Link;
import com.madibasoft.messaging.smtp.MailUtils;
import com.madibasoft.messaging.smtp.ResolvedLink;
import com.madibasoft.messaging.smtp.db.DbFactory;
import com.madibasoft.messaging.smtp.user.UserNotFoundException;
import com.madibasoft.messaging.smtp.user.UserServiceFactory;
import com.madibasoft.messaging.smtp.user.UserServiceInterface;

import spark.utils.IOUtils;

public class InboxTest {
	private static final Logger log = LoggerFactory.getLogger(InboxTest.class);
	private static Inbox inbox;

	@BeforeAll
	static void setUp() throws Exception {
		inbox = new Inbox(DbFactory.getDatabase());
		inbox.getDb().start();
		Config.getInstance().setProperty(Config.MAILGUARD_SMTP_OUT_TYPE, "dummy");
	}

	@AfterAll
	static void tearDown() throws Exception {
		inbox.getDb().stop();
	}

	@Test
	void testRecipients() {
		assertEquals("steve@test.com", inbox.getRecipients("steve@test.com, steve@test.com , bob@test.com").get(0));
		assertEquals("steve@test.com", inbox.getRecipients("steve@test.com").get(0));
	}

	@Test
	void testInbox() throws UserNotFoundException, RejectException {
		Config.getInstance().setProperty(Config.MAILGUARD_SMTP_OUT_TYPE, "dummy");
		UserServiceInterface userService = UserServiceFactory.getInstance();
		String uidA = "001";
		String uidB = "002";

		Link r = inbox.getDb().setLink(new Link(uidA, uidB));
		ResolvedLink rlink = new ResolvedLink(r);

		String proxyA = userService.lookupProxyByUid(uidA);
		assertEquals(rlink.getProxyA(), proxyA);
		assertTrue(inbox.acceptUid(uidB, uidA));
		assertTrue(inbox.acceptUid(uidA, uidB));
		// we should never receive mails from the obscured addresses
		assertFalse(inbox.acceptUid(uidA, uidA));
		assertFalse(inbox.acceptUid(uidB, uidB));

		MimeMessage mail;
		try {
			mail = MailUtils.loadEmail(getClass().getResource("/mail.txt").openStream());
			inbox.messageArrived(null, rlink.getClearA(), rlink.getProxyB(),
					IOUtils.toByteArray(mail.getInputStream()));
			inbox.messageArrived(null, rlink.getClearB(), rlink.getProxyA(),
					IOUtils.toByteArray(mail.getInputStream()));
		} catch (MessagingException | IOException e) {
			log.error("Problem!S", e);
		}
	}

	@Test
	void testSendHugeMail() throws UserNotFoundException, MessagingException {
		Config.getInstance().setProperty(Config.MAILGUARD_SMTP_OUT_TYPE, "dummy");
		String uidA = "001";
		String uidB = "002";

		Link r = inbox.getDb().setLink(new Link(uidA, uidB));
		ResolvedLink rlink = new ResolvedLink(r);

		MimeMessage mail;
		try {
			mail = MailUtils.loadEmail(getClass().getResource("/mail.txt").openStream());
			String filename = "/mail.txt";
			DataSource source;
			Multipart multipart = new MimeMultipart();

			for (int i = 0; i < 50000; i++) {
				source = new FileDataSource(createTempFile());
				MimeBodyPart messageBodyPart = new MimeBodyPart();
				messageBodyPart.setDataHandler(new DataHandler(source));
				messageBodyPart.setFileName(filename);
				multipart.addBodyPart(messageBodyPart);
			}
			mail.setContent(multipart);
			mail.saveChanges();
			inbox.messageArrived(null, rlink.getClearA(), rlink.getProxyB(),
					IOUtils.toByteArray(MailUtils.saveEmail(mail)));
			fail("Should reject the huge message");
		} catch (RejectException | IOException e) {
			log.info("Good:", e);
		}
	}

	// Creates a temporary file that will be deleted on JVM exit.
	private static String createTempFile() throws IOException {
		// Since Java 1.7 Files and Path API simplify operations on files
		Path path = Files.createTempFile("sample-file", ".txt");
		File file = path.toFile();
		// writing sample data
		Files.write(path, "Temporary content...".getBytes(StandardCharsets.UTF_8));
		// This tells JVM to delete the file on JVM exit.
		// Useful for temporary files in tests.
		file.deleteOnExit();
		return file.getAbsolutePath();
	}

	@Test
	void testBadDomain() throws UserNotFoundException {
		assertFalse(inbox.acceptEmail("bob@gmail.com", "bob@gmail.com", "001", "002"));
	}

	@Test
	void testProxyAddressRefusal() throws UserNotFoundException {
		Config.getInstance().setProperty(Config.MAILGUARD_SMTP_OUT_TYPE, "dummy");
		String toUid = "001";
		String fromUid = "002";
//		String clearA = new UserServiceImpl().lookupEmailByUid(toUid);
//		String clearB = new UserServiceImpl().lookupEmailByUid(fromUid);
		Link r = inbox.getDb().setLink(new Link(toUid, fromUid));
		ResolvedLink rlink = new ResolvedLink(r);

		// we should never receive mails from the obscured addresses
		assertFalse(inbox.acceptEmail(rlink.getProxyB(), rlink.getClearA(), rlink.getUidB(), rlink.getUidA()));
		assertFalse(inbox.acceptEmail(rlink.getProxyA(), rlink.getClearB(), rlink.getUidA(), rlink.getUidB()));
	}

	@Test
	void testAcceptByUid() throws Exception {
		String uidA = "001";
		String uidB = "002";
		ResolvedLink r = new ResolvedLink(inbox.getDb().setLink(new Link(uidA, uidB)));
		assertTrue(r.isValidLink());

		String obscuredA = r.getProxyA();
		String obscuredB = r.getProxyB();

		assertTrue(inbox.acceptUid(uidA, uidB));
		assertTrue(inbox.acceptEmail(r.getClearA(), r.getProxyB(), r.getUidA(), r.getUidB()));
		assertTrue(inbox.acceptUid(uidB, uidA));
		assertFalse(inbox.acceptUid(obscuredA, uidB));
		assertFalse(inbox.acceptUid(obscuredB, uidA));
		inbox.getDb().deleteLink(r);
		assertFalse(inbox.acceptEmail(r.getClearA(), r.getProxyB(), r.getUidA(), r.getUidB()));
		assertFalse(inbox.acceptUid(uidA, uidB));
		assertFalse(inbox.acceptUid(uidB, uidA));
		assertFalse(inbox.acceptUid(uidB, uidB));
		assertFalse(inbox.acceptUid(uidA, uidA));
	}

}
