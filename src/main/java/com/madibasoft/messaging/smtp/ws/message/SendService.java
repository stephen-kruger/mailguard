package com.madibasoft.messaging.smtp.ws.message;

import java.security.InvalidParameterException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.madibasoft.messaging.smtp.Config;
import com.madibasoft.messaging.smtp.Link;
import com.madibasoft.messaging.smtp.MailUtils;
import com.madibasoft.messaging.smtp.MissingParameterException;
import com.madibasoft.messaging.smtp.ResolvedLink;
import com.madibasoft.messaging.smtp.Utils;
import com.madibasoft.messaging.smtp.chat.ChatClient;
import com.madibasoft.messaging.smtp.db.DbInterface;
import com.madibasoft.messaging.smtp.user.UserServiceFactory;
import com.madibasoft.messaging.smtp.ws.AbstractJsonService;
import com.madibasoft.messaging.smtp.ws.link.SetService;

public class SendService extends AbstractJsonService {
	public static final String TO_UID_PARAM = "toUid";
	public static final String FROM_UID_PARAM = "fromUid";
	public static final String SUBJECT_PARAM = "subject";
	private static final Logger log = LoggerFactory.getLogger(SendService.class);
	private DbInterface db;

	public SendService(DbInterface db) {
		this.db = db;
	}

	public JsonObject send(String toUserId, String fromUserId, String subject, String body) throws Exception {
		// check the parameters
		if (toUserId == null)
			throw new MissingParameterException("Missing parameter " + SetService.TO_UID_PARAM);
		if (fromUserId == null)
			throw new MissingParameterException("Missing parameter " + FROM_UID_PARAM);
		if (subject == null)
			throw new MissingParameterException("Missing parameter " + SUBJECT_PARAM);

		// make sure we can't send to ourselves
		if (toUserId.equals(fromUserId)) {
			throw new InvalidParameterException("Sender and recipient id cannot be the same");
		}

		// store this link for future checking
		Link newLink = new Link(toUserId, fromUserId);
		ResolvedLink resolvedLink = new ResolvedLink(db.setLink(newLink));
		resolvedLink.swop(UserServiceFactory.getInstance().lookupEmailByUid(toUserId));
		log.info("Creating message");
		MimeMessage message = MailUtils.createMimeMessage(resolvedLink.getProxyB(), resolvedLink.getClearA(), subject,
				body);
		log.info("Done creating message");
		return sendAll(db, resolvedLink, message);
	}

	/*
	 * This will send a message to link.A from obfuscated link.B
	 */
	public static JsonObject sendAll(DbInterface db, ResolvedLink link, MimeMessage message) {
		message = MailUtils.populateMimeMessage(link.getProxyB(), link.getClearA(), message);
		ExecutorService executor = Executors.newCachedThreadPool();
		Config config = Config.getInstance();
		final MimeMessage processedMessage = message;
		// send the email
		switch (config.getForwardingType()) {
		case email:
		case both:
			Runnable runnable = new Runnable() {
				public void run() {
					try {
						// send the mail
						MailUtils.getInstance().sendMail(processedMessage);
					} catch (Throwable t) {
						Utils.jsonError(log, "Problem sending async", t);
					}
				}
			};
			executor.submit(runnable);
			break;
		case chat:
			break;
		}
		// and now the chat
		ChatClient chatClient = ChatClient.getInstance();
		String chatUid = "skipped";
		switch (config.getForwardingType()) {
		case chat:
		case both:
			try {
				JsonObject response = chatClient.sendMessage(link, message);
				if (response.has("messageID")) {
					chatUid = response.get("messageID").getAsString();
					log.debug("Sent chat message :{}", response);
					db.storeMail(link, message, chatUid);
				}
			} catch (Throwable t) {
				Utils.jsonError(log, "Error sending chat message :{}", t);
			}
			break;
		case email:
			db.storeMail(link, message, chatUid);
			break;
		}
		JsonObject result = result("Message sent");
		result.addProperty("from", link.getUidB());
		result.addProperty("to", link.getUidA());
		result.addProperty("chatUid", chatUid);
		log.debug("{}", result);
		return result;
	}

	@Override
	protected JsonObject invoke(Map<String, String[]> map, String body) throws Exception {
		log.debug(map.toString());
		return send(getString(map, TO_UID_PARAM, null), getString(map, FROM_UID_PARAM, null),
				getString(map, SUBJECT_PARAM, null), body);
	}

}
