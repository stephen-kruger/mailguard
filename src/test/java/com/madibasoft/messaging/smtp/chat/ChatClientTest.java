package com.madibasoft.messaging.smtp.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import javax.mail.internet.MimeMessage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.madibasoft.messaging.smtp.Config;
import com.madibasoft.messaging.smtp.Inbox;
import com.madibasoft.messaging.smtp.Link;
import com.madibasoft.messaging.smtp.MailUtils;
import com.madibasoft.messaging.smtp.ResolvedLink;
import com.madibasoft.messaging.smtp.db.DbFactory;

public class ChatClientTest {
	private static final Logger log = LoggerFactory.getLogger(ChatClientTest.class);

	@BeforeEach
	void setUp() throws Exception {
	}

	@AfterEach
	void tearDown() throws Exception {
	}

	@Test
	public void testChatDirection() throws Exception {
		log.info("Testing chat direction");
		MimeMessage message = MailUtils.loadEmail(getClass().getResource("/mail.txt").openStream());

		Config.getInstance().setString(Config.MAILGUARD_SMTP_OUT_TYPE, "dummy");
		Inbox inbox = new Inbox(DbFactory.getDatabase());
		ResolvedLink link = new ResolvedLink(inbox.getDb().setLink(new Link("001", "002")));
		message = MailUtils.populateMimeMessage(link.getProxyA(), link.getClearB(), message);
		assertEquals(MailUtils.getToUid(link, message), link.getUidB());
		assertNotEquals(MailUtils.getToUid(link, message), link.getUidA());
	}
}
