package com.madibasoft.messaging.smtp.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
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
	@Disabled
	void testInvoke() throws MessagingException, IOException {
		ChatClient cc = ChatClient.getInstance();
		JsonObject result;
		Config.getInstance().setString(Config.MAILGUARD_SMTP_OUT_TYPE, Config.SmtpOutType.direct.name());
		result = cc.invoke(cc.createSendBody("91581385", "This is the template test title",
				"<h1>Mime Content with Embedded Image</h1>\n"
						+ "<img src=\"data:image/png;base64, iVBORw0KGgoAAAANSUhEUgAAAAUA\n"
						+ "    AAAFCAYAAACNbyblAAAAHElEQVQI12P4//8/w38GIAXDIBKE0DHxgljNBAAO\n"
						+ "        9TXL0Y4OHwAAAABJRU5ErkJggg==\" alt=\"Red dot\" />",
				ChatClient.Category.transactional, ChatClient.MessageCategory.Partner));
		assertNotNull(result);
		log.info(result.toString());
	}

	@Test
	@Disabled
	void testInvokeHtml() throws MessagingException, IOException {
		MimeMessage message = MailUtils.loadEmail(getClass().getResource("/mail.txt").openStream());
		ChatClient cc = ChatClient.getInstance();
		JsonObject sendObject = cc.createSendBody("001", message, ChatClient.Category.transactional,
				ChatClient.MessageCategory.Partner);
		JsonObject result = cc.invoke(sendObject);
		assertNotNull(result);
		log.info(result.toString());
	}

	@Test
	public void testChatDirection() throws Exception {
		ChatClient cc = ChatClient.getInstance();
		MimeMessage message = MailUtils.loadEmail(getClass().getResource("/mail.txt").openStream());

		Config.getInstance().setString(Config.MAILGUARD_SMTP_OUT_TYPE, "dummy");
		Inbox inbox = new Inbox(DbFactory.getDatabase());
		ResolvedLink link = new ResolvedLink(inbox.getDb().setLink(new Link("001", "002")));
		message = MailUtils.populateMimeMessage(link.getProxyA(), link.getClearB(), message);
		assertEquals(cc.getToUid(link, message), link.getUidB());
		assertNotEquals(cc.getToUid(link, message), link.getUidA());
	}
}
