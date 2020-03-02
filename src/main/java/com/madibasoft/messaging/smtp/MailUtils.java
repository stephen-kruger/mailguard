package com.madibasoft.messaging.smtp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.ContentType;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;

import org.apache.commons.configuration.event.ConfigurationEvent;
import org.apache.commons.configuration.event.ConfigurationListener;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.mail.util.MimeMessageParser;
import org.masukomi.aspirin.Aspirin;
import org.masukomi.aspirin.listener.AspirinListener;
import org.masukomi.aspirin.listener.ResultState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.i18n.phonenumbers.PhoneNumberMatch;
import com.google.i18n.phonenumbers.PhoneNumberUtil;

import spark.utils.IOUtils;

public class MailUtils implements AspirinListener, ConfigurationListener {
	private static final Logger log = LoggerFactory.getLogger(MailUtils.class);
	private static final String TEXT_HTML = "text/html";
	private static final String TEXT_PLAIN = "text/plain";
	private static MailUtils mailUtilsInstance;
	private static PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
	private static List<MimeMessage> deliveredMails;

	private Config config;

	private MailUtils() {
		if (config == null) {
			config = Config.getInstance();
			config.addConfigurationListener(this);
		}
		phoneUtil = PhoneNumberUtil.getInstance();
	}

	/*
	 * Generic mail send method which send email using the configured type (direct,
	 * smtp or dummy)
	 */
	public void sendMail(MimeMessage mimeMessage) throws Exception {
		log.debug("Sending mail using type {}", config.getString(Config.MAILGUARD_SMTP_OUT_TYPE));
		if (mimeMessage == null)
			throw new Exception("Invalid message");
		switch (config.getSmtpOutType()) {
		case dummy:
			sendMailDummy(mimeMessage);
			break;
		case direct:
			sendMailAspirin(mimeMessage);
			break;
		case smtp:
			sendMailSMTP(mimeMessage);
			break;
		}
	}

	public static MailUtils getInstance() {
		if (mailUtilsInstance == null) {
			mailUtilsInstance = new MailUtils();
		}
		return mailUtilsInstance;
	}

	public void sendMailDummy(MimeMessage mimeMessage) throws MessagingException, IOException {
		// no-op
		for (Address toAddress : mimeMessage.getRecipients(RecipientType.TO)) {
			delivered(new Date().toString(), toAddress.toString(), ResultState.SENT, "ok");
		}
		getDeliveredMails().add(mimeMessage);
	}

	public List<MimeMessage> getDeliveredMails() {
		if (deliveredMails == null) {
			deliveredMails = new ArrayList<MimeMessage>();
		}
		return deliveredMails;
	}

	private void sendMailAspirin(MimeMessage mimeMessage) throws MessagingException, IOException, Exception {
		log.debug("Sending mail to {} from {}", mimeMessage.getRecipients(RecipientType.TO), mimeMessage.getFrom());
		Aspirin.add(mimeMessage, config.getLong(Config.MAILGUARD_SMTP_OUT_DIRECT_EXPIRY));
		Aspirin.addListener(this);
	}

	private void sendMailSMTP(Message message) throws Exception {
		// Get the Session object.
		Session session;
		final Properties props = System.getProperties();
		props.put("mail.transport.protocol", "smtp");
		props.put("mail.smtp.starttls.enable", "true");

		props.put("mail.smtp.host", config.getProperty(Config.MAILGUARD_SMTP_OUT_HOST));
		props.put("mail.smtp.port", config.getInt(Config.MAILGUARD_SMTP_OUT_PORT));
		props.put("mail.smtp.username", config.getProperty(Config.MAILGUARD_SMTP_OUT_USERNAME));
		props.put("mail.smtp.password", config.getProperty(Config.MAILGUARD_SMTP_OUT_PASSWORD));

		if ((props.containsKey("mail.smtp.username")
				&& (props.getProperty("mail.smtp.username").trim().length() > 0))) {
			log.debug("Using smtp auth with {} via {}", props.getProperty("mail.smtp.username"),
					props.getProperty("mail.smtp.host"));
			props.put("mail.smtp.auth", "true");
			session = Session.getInstance(props, new javax.mail.Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(props.getProperty("mail.smtp.username"),
							props.getProperty("mail.smtp.password"));
				}
			});
		} else {
			props.put("mail.smtp.auth", "false");
			session = Session.getInstance(props);
		}
		session.setDebug(false);
		try {
			// Send message
			Transport transport = session.getTransport();
			transport.connect(props.getProperty(Config.MAILGUARD_SMTP_OUT_HOST),
					props.getProperty(Config.MAILGUARD_SMTP_OUT_USERNAME),
					props.getProperty(Config.MAILGUARD_SMTP_OUT_PASSWORD));
			transport.sendMessage(message, message.getAllRecipients());
			for (Address toAddress : message.getRecipients(RecipientType.TO)) {
				log.debug("Sent message successfully to {}", toAddress);
				delivered(new Date().toString(), toAddress.toString(), ResultState.SENT, "ok");
			}
		} catch (MessagingException e) {
			Utils.jsonError(log, "Problem sending message", e);
			for (Address toAddress : message.getRecipients(RecipientType.TO)) {
				delivered(new Date().toString(), toAddress.toString(), ResultState.FAILED, e.getMessage());
			}
			throw e;
		}
	}

	public static Session getSession() {
		Properties props = new Properties();
		// do this to speed up JUnit tests when no network is present
		props.setProperty("mail.host", Config.getInstance().getString(Config.MAILGUARD_PUBLIC_HOST));
		return Session.getDefaultInstance(props);
	}

	public static MimeMessage createMimeMessage(String from, String to, String subject, String body) throws Exception {
		MimeMessage message = new MimeMessage(getSession());
		message.setSubject(subject);
		message.setText(body);
		message.saveChanges();
		return populateMimeMessage(from, to, message);
	}

	public static MimeMessage populateMimeMessage(String from, String to, MimeMessage mimeMessage) {
		try {
			mimeMessage.setRecipients(RecipientType.TO, new Address[0]);
		} catch (MessagingException e) {
			Utils.jsonError(log, "Problem clearing TO of message", e);
		}
		try {
			mimeMessage.setRecipients(RecipientType.CC, new Address[0]);
		} catch (MessagingException e) {
			Utils.jsonError(log, "Problem clearing CC of message", e);
		}
		try {
			mimeMessage.setRecipients(RecipientType.BCC, new Address[0]);
		} catch (MessagingException e) {
			Utils.jsonError(log, "Problem clearing BCC if message", e);
		}

		if (from != null) {
			try {
				mimeMessage.setFrom(new InternetAddress(from));
			} catch (Throwable e) {
				Utils.jsonError(log, "Problem populating From of message", e);
			}
		}
		if (to != null) {
			try {
				mimeMessage.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
			} catch (MessagingException e) {
				Utils.jsonError(log, "Problem populating To of message", e);
			}
		}
		try {
			mimeMessage.saveChanges();
		} catch (MessagingException e) {
			Utils.jsonError(log, "Problem populating message", e);
		}
		return mimeMessage;
	}

	public static MimeMessage populateMimeRejectionMessage(String from, String to, MimeMessage mimeMessage)
			throws MessagingException, IOException, Exception {
		mimeMessage = populateMimeMessage(from, to, mimeMessage);
		mimeMessage.setSubject(
				Config.getInstance().getString(Config.MAILGUARD_REJECTION_SUBJECT) + mimeMessage.getSubject());
		mimeMessage.setText(Config.getInstance().getString(Config.MAILGUARD_REJECTION_BODY));
		return mimeMessage;
	}

	public static MimeMessage loadEmail(byte[] bytes) throws MessagingException, IOException {
		return loadEmail(new ByteArrayInputStream(bytes));
	}

	public static MimeMessage loadEmail(InputStream inputStream) throws MessagingException, IOException {
		MimeMessage message = new MimeMessage(getSession(), inputStream);
		message.saveChanges();
		inputStream.close();
		return message;
	}

	public static InputStream saveEmail(MimeMessage message) throws IOException, MessagingException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		message.writeTo(baos);
		return new ByteArrayInputStream(baos.toByteArray());
	}

	public static String decodeRFC2407(String s) {
		try {
			s = IOUtils.toString(MimeUtility.decode(new ByteArrayInputStream(s.getBytes()), "quoted-printable"));
			s = MimeUtility.decodeText(s);
		} catch (Throwable e) {
			log.warn("Problem decoding " + s);
		}
		return s;

	}

	/*
	 * Remove any fluff and names etc from the email
	 */
	public static String cleanEmail(String email) {
		try {
			email = decodeRFC2407(email);
			email = StringEscapeUtils.unescapeJava(email);
			// hack the Cicso crap off here
			// see https://en.wikipedia.org/wiki/Bounce_Address_Tag_Validation
			if (email.startsWith("prvs")) {
				log.warn("Trying to recover from mangled email :" + email);
				email = email.substring(email.lastIndexOf('=') + 1);
			}
			return new InternetAddress(email.trim(), true).getAddress();
		} catch (Throwable e) {
			log.error("Problem parsing email {}: {}", email, e.getMessage());
			return email;
		}
	}

	@Override
	public void delivered(String mailId, String recipient, ResultState state, String resultContent) {
		log.info("Mail state changed to {} for recipient {}", state.name(), MailUtils.anonymiseLog(recipient));
	}

	private static String removeDigits(String s) {
		return s.replaceAll("0", "*").replaceAll("1", "*").replaceAll("2", "*").replaceAll("3", "*")
				.replaceAll("4", "*").replaceAll("5", "*").replaceAll("6", "*").replaceAll("7", "*")
				.replaceAll("8", "*").replaceAll("9", "*");
	}

	/*
	 * Remove an email address from piiString
	 */
	protected static String removePhoneNumbers(String piiString) {
		String[] countries = { "US", "SG", "VN", "ID", "MY", "CN", "PH", "JP", "KO", "ID" };
		for (String country : countries) {
			for (PhoneNumberMatch m : phoneUtil.findNumbers(piiString, country)) {
				piiString = piiString.replace(m.rawString(), removeDigits(m.rawString()));
			}
		}
		return piiString;
	}

	public static boolean isValidDomain(String recipient) {
		// if it's addressed to one of our acceptable domains, proceed
		boolean valid = false;
		for (String domain : Config.getInstance().getStringList(Config.MAILGUARD_SMTP_IN_ACCEPT)) {
			// first check we allow the recipient domain
			if (recipient.toLowerCase().endsWith(domain)) {
				log.debug("Valid domain {}", domain);
				valid = true;
			}
		}
		if (!valid) {
			log.warn("Rejecting mail from unknown domain : {}", recipient);
		}
		return valid;
	}

	protected static String replaceEmails(String oldEmail, String newEmail, final String str) {
		String newStr = str;
		do {
			newStr = newStr.replace(oldEmail, newEmail);
		} while (newStr.indexOf(oldEmail) >= 0);

		return newStr;
	}

	protected static String replaceEmails(ResolvedLink r, String str) {
		String newStr = replaceEmails(r.getClearA(), r.getProxyA(), str);
		newStr = replaceEmails(r.getClearB(), r.getProxyB(), newStr);
		return newStr;
	}

	public static String replacePIIFromString(ResolvedLink r, String body) {
		body = MailUtils.decodeRFC2407(body);
		body = replaceEmails(r, body);
		body = removePhoneNumbers(body);
		return body;
	}

	/*
	 * Make sure none of the clear emails are found in the message, by replacing
	 * them with proxied versions
	 */
	public static MimeMessage removePiiFromMimeMessage(ResolvedLink r, MimeMessage message)
			throws MessagingException, IOException {
		if (message.isMimeType(TEXT_PLAIN)) {
			message.setContent(replacePIIFromString(r, message.getContent().toString()), message.getContentType());
		} else if (message.isMimeType("multipart/*")) {
			MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
			message.setContent(replacePIIFromMimeMultipart(r, mimeMultipart), message.getContentType());
		}
		message.saveChanges();
		return message;
	}

	public static MimeMultipart replacePIIFromMimeMultipart(ResolvedLink r, MimeMultipart mimeMultipart)
			throws MessagingException, IOException {
		int count = mimeMultipart.getCount();
		for (int i = 0; i < count; i++) {
			BodyPart bodyPart = mimeMultipart.getBodyPart(i);
			if (bodyPart.getContent() instanceof MimeMultipart) {
				bodyPart.setContent(replacePIIFromMimeMultipart(r, (MimeMultipart) bodyPart.getContent()),
						bodyPart.getContentType());
			} else if ((bodyPart.isMimeType("text/plain")) || (bodyPart.isMimeType(TEXT_HTML))) {
				bodyPart.setContent(replacePIIFromString(r, bodyPart.getContent().toString()),
						bodyPart.getContentType());
			} else {
				log.warn("Ignoring content type for body part!!!!!!!!!!! {}", bodyPart.getContentType());
			}
		}
		return mimeMultipart;
	}

	public static String getHtmlFromMimeContent(MimeMessage message) throws Exception {
		MimeMessageParser parser = new MimeMessageParser(message);
		parser.parse();

		String htmlContent = parser.getHtmlContent();
		return htmlContent;
	}

	public static String getTextFromMimeMessage(MimeMessage message) throws IOException, MessagingException {
		if (message.getContent() instanceof Multipart)
			return getTextFromMimeMultipart((Multipart) message.getContent());
		return message.getContent().toString();
	}

	public static String getTextFromMimeMultipart(Multipart mimeMultipart) throws IOException, MessagingException {

		int count = mimeMultipart.getCount();
		if (count == 0) {
			log.error("Multipart with no body parts not supported.");
			throw new MessagingException("Multipart with no body parts not supported.");
		}
		boolean multipartAlt = new ContentType(mimeMultipart.getContentType()).match("multipart/alternative");
		if (multipartAlt) {
			// alternatives appear in an order of increasing
			// faithfulness to the original content. Customize as req'd.
			return getTextFromBodyPart(mimeMultipart.getBodyPart(count - 1));
		} else {
			String result = "";
			for (int i = 0; i < count; i++) {
				BodyPart bodyPart = mimeMultipart.getBodyPart(i);
				result += getTextFromBodyPart(bodyPart);
			}
			return result;
		}
	}

	public static String getTextFromBodyPart(BodyPart bodyPart) throws IOException, MessagingException {

		String result = "";
		if (bodyPart.isMimeType(TEXT_PLAIN)) {
			result = (String) bodyPart.getContent();
		} else if (bodyPart.isMimeType(TEXT_HTML)) {
			String html = (String) bodyPart.getContent();
			result = html;
		} else if (bodyPart.getContent() instanceof MimeMultipart) {
			result = getTextFromMimeMultipart((MimeMultipart) bodyPart.getContent());
		} else {
			log.warn("Unknown body part type encountered :" + bodyPart.getContentType());
		}
		return result;
	}

	@Override
	public void configurationChanged(ConfigurationEvent event) {
		if (event.getPropertyName().equals(Config.MAILGUARD_SMTP_OUT_TYPE)) {
			Aspirin.removeListener(this);

			switch (config.getSmtpOutType()) {
			case dummy:
				break;
			case direct:
				Aspirin.addListener(this);
				break;
			case smtp:
				break;
			}
		}

	}

	public static String anonymiseLog(String email) {
		StringBuffer clean = new StringBuffer(email.substring(0, email.indexOf("@")));
		int count = email.length() - clean.length();
		clean.append('@');
		for (int i = 0; i < count; i++) {
			clean.append('*');
		}
		return clean.toString();
	}
}
