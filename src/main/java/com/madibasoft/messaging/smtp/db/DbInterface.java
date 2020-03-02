package com.madibasoft.messaging.smtp.db;

import java.sql.Connection;
import java.util.List;

import javax.mail.internet.MimeMessage;

import com.google.gson.JsonObject;
import com.madibasoft.messaging.smtp.Link;
import com.madibasoft.messaging.smtp.ResolvedLink;
import com.madibasoft.messaging.smtp.user.UserNotFoundException;

public interface DbInterface {

	String LINK_TABLE = "LINK_TABLE";
	String MAIL_TABLE = "MAIL_TABLE";
	String A_UID = "A_UID";
	String B_UID = "B_UID";
	String TO_UID = "TO_UID";
	String FROM_UID = "FROM_UID";
	String SUBJECT = "SUBJECT";
	String BODY = "BODY";
	String EXPIRY = "EXPIRY";
	String CREATED = "CREATED";
	String MODIFIED = "MODIFIED";
	String CHAT_UID = "CHATUID";

	void start() throws Exception;

	void stop();

	Connection getConnection() throws Exception;

	void clear() throws Exception;

	void deleteLink(Link r) throws Exception;

	boolean containsLink(Link r) throws Exception;

	boolean containsByUid(String uidA, String uidB);

	Link setLink(Link link) throws UserNotFoundException;

	/*
	 * Get the links based on the uid
	 */
	Link getLink(Link link);

	/*
	 * List all links for this userid
	 */
	List<Link> listLinks(String userId, int offset, int count);

	List<JsonObject> listMails(int offset, int count);

	/*
	 * Store this mail into our MAIL_TABLE for future reference
	 */
	void storeMail(ResolvedLink r, MimeMessage mimeMessage, String chatUid);

	boolean acceptUid(String uidA, String uidB);

	Object getLinkByUid(String string, String string2);

}