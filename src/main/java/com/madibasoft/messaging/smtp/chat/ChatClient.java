package com.madibasoft.messaging.smtp.chat;

import java.io.IOException;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.madibasoft.messaging.smtp.Config;
import com.madibasoft.messaging.smtp.MailUtils;
import com.madibasoft.messaging.smtp.ResolvedLink;
import com.madibasoft.messaging.smtp.Utils;

import spark.utils.IOUtils;

public class ChatClient {
	private static final Logger log = LoggerFactory.getLogger(ChatClient.class);
	private Config config = Config.getInstance();
	private Gson gson = new GsonBuilder().setPrettyPrinting().create();
	private Timeout timeout = Timeout.ofMilliseconds(2000);
	private static final String templateId = "aff79f14-bbc4-4ed0-ae45-90e9e3b7df0f";
	// private static final String templateId =
	// "5eac8b23-dd7b-4920-9685-49f467a2fbe4";
	private static ChatClient chatClient;

	private ChatClient() {

	}

	public enum Category {
		transactional, account, offers, announcements, research
	};

	public enum MessageCategory {
		Partner
	}

	public JsonObject invoke(JsonObject body) throws UnsupportedOperationException, IOException {
		log.debug("Sending chat {} via {}", gson.toJson(body), config.getString(Config.MAILGUARD_MESSAGING_API));
		try {
			if (Config.getInstance().getSmtpOutType().equals(Config.SmtpOutType.dummy)) {
				// fake the call to send the Chat message
				JsonObject jo = new JsonObject();
				jo.addProperty("messageID", "2b48707d-46ca-4b66-bd44-9589e365d603");
				return jo;
			} else {
				// send the Chat message
				HttpPost request = new HttpPost(config.getString(Config.MAILGUARD_MESSAGING_API));
				request.setHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
				request.setEntity(new StringEntity(gson.toJson(body)));

				RequestConfig config = RequestConfig.custom().setConnectTimeout(timeout)
						.setConnectionRequestTimeout(timeout).build();

				CloseableHttpResponse response = HttpClientBuilder.create().setDefaultRequestConfig(config).build()
						.execute(request);
				HttpEntity entity = response.getEntity();

				String responseStr = IOUtils.toString(entity.getContent());
				return gson.fromJson(responseStr, JsonObject.class);
			}
		} catch (Throwable t) {
			Utils.jsonError(log, "Error sending chat message :{}", t);
			JsonObject jo = new JsonObject();
			jo.addProperty("error", t.getMessage());
			jo.addProperty("exception", t.getClass().getName());
			return jo;
		}
	}

	public JsonObject createSendBody(String userId, MimeMessage mimeMessage, Category category,
			MessageCategory messageCategory) throws MessagingException, IOException {
		// String content = MailUtils.getTextFromMimeMessage(mimeMessage);
		String content;
		try {
			content = MailUtils.getHtmlFromMimeContent(mimeMessage);
			if ((content == null) || (content.length() == 0)) {
				content = MailUtils.getTextFromMimeMessage(mimeMessage);
			}
			return createSendBody(userId, mimeMessage.getSubject(), content, category, messageCategory);
		} catch (Exception e) {
			return createSendBody(userId, mimeMessage.getSubject(), e.getMessage(), category, messageCategory);
		}
	}

	public JsonObject createSendBody(String userId, String title, String content, Category category,
			MessageCategory messageCategory) {
		JsonObject jo = new JsonObject();
		jo.addProperty("recipientId", userId);
		jo.addProperty("recipientType", "passenger");
		jo.add("template", createTemplate(title, content, messageCategory));
		jo.addProperty("category", category.name());
		return jo;
	}

	private JsonObject createTemplate(String title, String content, MessageCategory messageCategory) {
		JsonObject template = new JsonObject();
		template.addProperty("id", templateId);
		template.addProperty("language", "en");
		JsonObject params = new JsonObject();
		params.addProperty("title", title);
		params.addProperty("message", content);

		template.add("params", params);
		return template;
	}

	public JsonObject sendMessage(final ResolvedLink rlink, MimeMessage mimeMessage)
			throws MessagingException, IOException {
		JsonObject rtcMessage = createSendBody(//
				getToUid(rlink, mimeMessage), // userId
				mimeMessage, // the mime message
				Category.transactional, //
				MessageCategory.Partner);
		log.debug("Sent rtc message {}", rtcMessage);
		JsonObject response = invoke(rtcMessage);
		return response;
	}

	public String getToUid(ResolvedLink rlink, MimeMessage mimeMessage) throws MessagingException {
		try {
			for (Address fromAddress : mimeMessage.getFrom()) {
				if (rlink.getProxyA().equals(fromAddress.toString())) {
					return rlink.getUidB();
				} else if (rlink.getProxyB().equals(fromAddress.toString())) {
					return rlink.getUidA();
				}
			}
		} catch (Throwable t) {
			Utils.jsonError(log, "Could not find uid", t);
			return "";
		}
		throw new MessagingException("Unrecognised from address");
	}

	public static ChatClient getInstance() {
		if (chatClient == null) {
			chatClient = new ChatClient();
		}
		return chatClient;
	}
}
