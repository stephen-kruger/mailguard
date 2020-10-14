package com.madibasoft.messaging.smtp;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subethamail.smtp.MessageContext;
import org.subethamail.smtp.RejectException;
import org.subethamail.smtp.helper.BasicMessageListener;

import com.madibasoft.messaging.smtp.db.DbInterface;
import com.madibasoft.messaging.smtp.user.UserNotFoundException;
import com.madibasoft.messaging.smtp.user.UserServiceFactory;
import com.madibasoft.messaging.smtp.user.UserServiceInterface;
import com.madibasoft.messaging.smtp.ws.message.SendService;

public class Inbox implements BasicMessageListener {
	private static final Logger log = LoggerFactory.getLogger(Inbox.class);
	private DbInterface db;
	private Config config = Config.getInstance();

	public Inbox() {
		super();
	}

	public Inbox(DbInterface db) {
		super();
		this.db = db;
	}

	public void setDb(DbInterface db) {
		this.db = db;
	}

	public DbInterface getDb() {
		return db;
	}

	public List<String> getRecipients(String recipient) {
		List<String> recipients = new ArrayList<String>();
		if (recipient.indexOf(',') >= 0) {
			StringTokenizer tok = new StringTokenizer(recipient, ",");
			String r;
			while (tok.hasMoreTokens()) {
				r = MailUtils.cleanEmail(tok.nextToken());
				// ensure we remove duplicates
				if ((r.length() > 1) && (!recipients.contains(r))) {
					recipients.add(r);
				} else {
					log.info("Skipping duplicate recipient {}", r);
				}
			}
		} else {
			recipients.add(recipient);
		}
		return recipients;
	}

	public boolean acceptUid(String fromUid, String recipientUid) {
		try {
			return db.acceptUid(fromUid, recipientUid);
		} catch (Throwable t) {
			Utils.jsonError(log, "Something bad happened", t);
			return false;
		}
	}

	public boolean acceptEmail(String fromEmail, String recipientEmail, String fromUid, String recipientUid) {
		// check for null
		if ((fromEmail == null) || (recipientEmail == null)) {
			log.error("Null sender {} or recipient {}", fromEmail, recipientEmail);
			return false;
		}

		// make sure we cannot send to ourselves
		if (fromEmail.equals(recipientEmail)) {
			log.warn("Detected email send to itself");
			return false;
		}

		// if it's addressed to one of our acceptable proxy domains, proceed
		if (!MailUtils.isValidDomain(recipientEmail)) {
			return false;
		}

		return acceptUid(fromUid, recipientUid);
	}

	/*
	 * We need to proxy from to a obscured address, and decode recipient to a clear
	 * address.
	 */
	@Override
	public void messageArrived(MessageContext context, String clearA, String proxyB, byte[] data)
			throws RejectException {
		log.info("Received message from={} to={} bytes=" + data.length, MailUtils.anonymiseLog(clearA), proxyB);
		if (data.length > config.getInt(Config.MAILGUARD_SMTP_IN_MAX_MAIL_SIZE, 100000)) {
			throw new RejectException("Too much data (" + data.length + " bytes) maximum allowed is "
					+ config.getInt(Config.MAILGUARD_SMTP_IN_MAX_MAIL_SIZE, 100000));
		}
		UserServiceInterface us = UserServiceFactory.getInstance();
		clearA = MailUtils.cleanEmail(clearA);
		proxyB = MailUtils.cleanEmail(proxyB);
		String fromUid, recipientUid;
		try {
			fromUid = us.lookupUidByEmail(clearA);
			recipientUid = us.lookupUidByProxy(proxyB);
		} catch (UserNotFoundException enfe) {
			log.warn("Unable to accept message from={} to={} due to error :" + enfe.getMessage(),
					MailUtils.anonymiseLog(clearA), proxyB);
			throw new RejectException("Unable to accept message from=" + clearA + " to=" + proxyB + " due to error :"
					+ enfe.getMessage());
		}
		if (!acceptEmail(clearA, proxyB, fromUid, recipientUid)) {
			log.warn("Rejecting unsolicited mail from={} to={}", MailUtils.anonymiseLog(clearA), proxyB);
			throw new RejectException("Rejecting unsolicited mail from " + clearA + " to=" + proxyB);
		} else {
			log.info("Accepting message from={} to={}", MailUtils.anonymiseLog(clearA), proxyB);
		}
		try {

			List<String> obscuredRecipientList = getRecipients(proxyB);
			MimeMessage mimeMessage = MailUtils.loadEmail(data);

			for (String obscuredRecipient : obscuredRecipientList) {
				recipientUid = us.lookupUidByProxy(obscuredRecipient);
				Link link = new Link(recipientUid, fromUid);
				ResolvedLink rlink = new ResolvedLink(link);
				if (rlink.isValidLink()) {
					mimeMessage = MailUtils.removePiiFromMimeMessage(rlink, mimeMessage);
					log.info("{}", SendService.sendAll(db, rlink, mimeMessage));
				} else {
					// send a rejection message to the sender
					log.warn("Expired link {}, sending rejection mail to sender", link);
					mimeMessage = MailUtils.populateMimeRejectionMessage(rlink.getProxyB(), rlink.getClearA(),
							mimeMessage);
					// send the rejection message to each recipient
					MailUtils.getInstance().sendMail(mimeMessage);
				}

			}
		} catch (Throwable t) {
			log.error("Something bad happened while processing message from=" + clearA + " to=" + proxyB, t);
			t.printStackTrace();
		}

	}

}
