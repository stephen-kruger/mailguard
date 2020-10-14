package com.madibasoft.messaging.smtp.chat;

import java.io.IOException;
import java.util.UUID;

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
import com.madibasoft.messaging.smtp.ResolvedLink;
import com.madibasoft.messaging.smtp.Utils;

import spark.utils.IOUtils;

public class ChatClient {
	private static final Logger log = LoggerFactory.getLogger(ChatClient.class);
	private Config config = Config.getInstance();
	private Gson gson = new GsonBuilder().setPrettyPrinting().create();
	private Timeout timeout = Timeout.ofMilliseconds(2000);
	private static ChatClient chatClient;

	private ChatClient() {

	}

	public JsonObject sendMessage(final ResolvedLink rlink, MimeMessage mimeMessage)
			throws MessagingException, IOException {
		log.debug("Sent message {}", rlink);
		JsonObject response = new JsonObject();
		return response;
	}
	
	public JsonObject invoke(JsonObject body) throws UnsupportedOperationException, IOException {
		String endpoint = "https://somemessagingendpoint/";
		log.debug("Sending chat {} via {}", gson.toJson(body), endpoint);
		try {
			if (Config.getInstance().getSmtpOutType().equals(Config.SmtpOutType.dummy)) {
				// fake the call to send the Chat message
				JsonObject jo = new JsonObject();
				jo.addProperty("messageID", UUID.randomUUID().toString());
				return jo;
			} else {
				// send the Chat message
				HttpPost request = new HttpPost(config.getString(endpoint));
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

	public static ChatClient getInstance() {
		if (chatClient == null) {
			chatClient = new ChatClient();
		}
		return chatClient;
	}
}
