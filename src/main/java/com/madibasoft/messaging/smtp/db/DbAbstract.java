package com.madibasoft.messaging.smtp.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.mail.internet.MimeMessage;

import org.apache.commons.dbutils.DbUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.madibasoft.messaging.smtp.Config;
import com.madibasoft.messaging.smtp.Link;
import com.madibasoft.messaging.smtp.MailUtils;
import com.madibasoft.messaging.smtp.ResolvedLink;
import com.madibasoft.messaging.smtp.Utils;

abstract class DbAbstract implements DbInterface {
	private static final Logger log = LoggerFactory.getLogger(DbAbstract.class);

	private void dropTables() {
		Connection connection = null;
		Statement ps = null;
		try {
			connection = getConnection();
			connection.setAutoCommit(false);
			ps = connection.createStatement();
			ps.executeUpdate("DROP TABLE " + LINK_TABLE);
			ps.executeUpdate("DROP TABLE " + MAIL_TABLE);
			connection.commit();
		} catch (Throwable t) {
			log.warn("Cannot drop INBOX_TABLE :" + t.getMessage());
		} finally {
			DbUtils.closeQuietly(ps);
			DbUtils.closeQuietly(connection);
		}
	}

	@Override
	public void clear() throws Exception {
		dropTables();
		setupTables();
	}

	protected void setupTables() {
		Connection connection = null;
		Statement ps = null;
		try {
			connection = getConnection();
			ps = connection.createStatement();
			connection.setAutoCommit(false);
			try {
				String sqlStr = "CREATE TABLE " + LINK_TABLE + " (" + A_UID + " VARCHAR(256), " + B_UID
						+ " VARCHAR(256), " + EXPIRY + " TIMESTAMP, " + CREATED + " TIMESTAMP, " + MODIFIED
						+ " TIMESTAMP" + ")";
				ps.executeUpdate(sqlStr);
				String[] indexes = new String[] { A_UID, B_UID };
				createIndexes(connection, LINK_TABLE, indexes);
			} catch (Throwable t) {
				log.debug("Problem setting up link table", t);
			}

			try {
				String sqlStr = "CREATE TABLE " + MAIL_TABLE + " (" + TO_UID + " VARCHAR(256), " + FROM_UID
						+ " VARCHAR(256), " + SUBJECT + " VARCHAR(256), " + BODY + " TEXT, " + CREATED + " TIMESTAMP, "
						+ CHAT_UID + " VARCHAR(36)" + ")";
				ps.executeUpdate(sqlStr);
				String[] indexes = new String[] { TO_UID, FROM_UID, CREATED };
				createIndexes(connection, MAIL_TABLE, indexes);
			} catch (Throwable t) {
				log.debug("Problem setting up mail table", t);
			}
			connection.commit();
		} catch (Throwable t) {
			log.warn("Cannot setup tables :" + t.getMessage());
		} finally {
			DbUtils.closeQuietly(ps);
			DbUtils.closeQuietly(connection);
		}
	}

	private void createIndexes(Connection connection, String table, String[] indexes) throws Exception {
		Statement s = connection.createStatement();
		for (int i = 0; i < indexes.length; i++) {
			try {
				s.executeUpdate("CREATE INDEX " + indexes[i] + "_INDEX ON " + table + "(" + indexes[i] + ")");
			} catch (Throwable t) {
				log.debug("Problem creating asc index {} ({})", indexes[i], t.getMessage());
			}
		}
		DbUtils.closeQuietly(s);
	}

	public boolean containsByUid(String uidA, String uidB) {
		boolean contains = false;
		log.debug("Checking for {}  {}", uidA, uidB);
		Connection connection = null;
		PreparedStatement ps = null;
		ResultSet result = null;
		try {
			connection = getConnection();
			ps = connection.prepareStatement("SELECT * FROM " + LINK_TABLE + " WHERE (" + A_UID + "=? AND " + B_UID
					+ "=?)" + " OR(" + A_UID + "=? AND " + B_UID + "=?) LIMIT 1");
			ps.setString(1, uidA);
			ps.setString(2, uidB);
			ps.setString(3, uidB);
			ps.setString(4, uidA);
			ps.execute();
			result = ps.getResultSet();
			if (result.next()) {
				contains = true;
			} else {
				log.debug("No relationship found for {}  {}", uidA, uidB);
			}
			result.close();
		} catch (Throwable t) {
			log.error("Problem checking for {}  {}", uidA, uidB);
		} finally {
			DbUtils.closeQuietly(result);
			DbUtils.closeQuietly(ps);
			DbUtils.closeQuietly(connection);
		}
		return contains;
	}

	public void deleteLink(Link r) throws Exception {
		Connection connection = null;
		PreparedStatement ps = null;
		try {
			connection = getConnection();
			connection.setAutoCommit(false);
			ps = connection.prepareStatement("DELETE FROM " + LINK_TABLE + " WHERE (" + A_UID + "=? AND " + B_UID
					+ "=?)" + " OR(" + A_UID + "=? AND " + B_UID + "=?)");
			ps.setString(1, r.getUidA());
			ps.setString(2, r.getUidB());
			ps.setString(3, r.getUidB());
			ps.setString(4, r.getUidA());
			ps.execute();
			connection.commit();
			log.debug("Removed link {}", r);
		} finally {
			DbUtils.closeQuietly(ps);
			DbUtils.closeQuietly(connection);
		}
	}

	public boolean containsLink(Link r) {
		return containsByUid(r.getUidA(), r.getUidB());
	}

	/*
	 * Update or add a link
	 */
	public Link setLink(Link link) {
		if (containsLink(link)) {
			Link r = getLink(link);
			r.setExpiry(link.getExpiry());
			log.info("Updating link");
			return updateLink(r);
		} else {
			log.info("Adding link");
			return addLink(link);
		}
	}

	/*
	 * Add a new row into the database
	 */
	private Link addLink(Link link) {
		Connection connection = null;
		PreparedStatement ps = null;
		try {
			connection = getConnection();
			connection.setAutoCommit(false);
			ps = connection.prepareStatement("INSERT INTO " + LINK_TABLE + " VALUES (?,?,?,?,?)");
			ps.setString(1, link.getUidA());
			ps.setString(2, link.getUidB());
			ps.setTimestamp(3, new Timestamp(link.getExpiry().getTime()));
			ps.setTimestamp(4, new Timestamp(link.getCreated().getTime()));
			ps.setTimestamp(5, new Timestamp(link.getModified().getTime()));
			ps.execute();
			connection.commit();
			connection.close();
			log.debug("Added link {}", link);
			return link;
		} catch (Throwable t) {
			Utils.jsonError(log, "An exception was thrown whilst adding the link", t);
			return null;
		} finally {
			DbUtils.closeQuietly(ps);
			DbUtils.closeQuietly(connection);
		}
	}

	/*
	 * Update an existing row in the database
	 */
	private Link updateLink(Link link) {
		Connection connection = null;
		PreparedStatement ps = null;
		try {
			connection = getConnection();
			connection.setAutoCommit(false);

			link.setModified(new java.util.Date());
			ps = connection.prepareStatement("UPDATE " + LINK_TABLE + " SET " + EXPIRY + "=? WHERE (" + A_UID
					+ "=? AND " + B_UID + "=?) OR (" + A_UID + "=? AND " + B_UID + "=?);");
			ps.setTimestamp(1, new Timestamp(link.getExpiry().getTime()));
			ps.setString(2, link.getUidA());
			ps.setString(3, link.getUidB());
			ps.setString(4, link.getUidB());
			ps.setString(5, link.getUidA());
			ps.execute();
			connection.commit();
			log.debug("Updated link {}", link);
			return link;
		} catch (Throwable t) {
			Utils.jsonError(log, "An exception was thrown whilst updating the link", t);
			return null;
		} finally {
			DbUtils.closeQuietly(ps);
			DbUtils.closeQuietly(connection);
		}
	}

	/*
	 * List all links for this userid
	 */
	public List<Link> listLinks(String userId, int offset, int count) {
		List<Link> r = new ArrayList<Link>();
		Connection connection = null;
		PreparedStatement ps = null;
		ResultSet result = null;
		try {
			connection = getConnection();
			if ((userId == null) || (userId.length() == 0)) {
				ps = connection.prepareStatement("SELECT * FROM " + LINK_TABLE + " LIMIT " + offset + "," + count);
			} else {
				ps = connection.prepareStatement("SELECT * FROM " + LINK_TABLE + " WHERE (" + A_UID + "=? OR " + B_UID
						+ "=?) LIMIT " + offset + "," + count);
				ps.setString(1, userId);
				ps.setString(2, userId);
			}
			ps.execute();
			result = ps.getResultSet();
			while (result.next()) {
				String uid1 = result.getString(A_UID);
				String uid2 = result.getString(B_UID);
				Link rel = new Link(uid1, //
						uid2, //
						result.getTimestamp(EXPIRY), //
						result.getTimestamp(CREATED), //
						result.getTimestamp(MODIFIED));
				r.add(rel);

			}
			result.close();
			ps.close();
			connection.close();
		} catch (Throwable t) {
			log.warn(t.getMessage());
		} finally {
			DbUtils.closeQuietly(result);
			DbUtils.closeQuietly(ps);
			DbUtils.closeQuietly(connection);
		}

		return r;
	}

	@SuppressWarnings("deprecation")
	public List<JsonObject> listMails(int offset, int count) {
		List<JsonObject> r = new ArrayList<JsonObject>();
		Connection connection = null;
		PreparedStatement ps = null;
		ResultSet result = null;
		try {
			connection = getConnection();
			ps = connection.prepareStatement("SELECT * FROM " + MAIL_TABLE + " LIMIT " + offset + "," + count);
			ps.execute();
			result = ps.getResultSet();
			while (result.next()) {
				JsonObject jo = new JsonObject();
				jo.addProperty(TO_UID, result.getString(TO_UID));
				jo.addProperty(FROM_UID, result.getString(FROM_UID));
				jo.addProperty(SUBJECT, result.getString(SUBJECT));
				jo.addProperty(BODY, result.getString(BODY));
				jo.addProperty(CREATED, result.getTimestamp(CREATED).toGMTString());
				jo.addProperty(CHAT_UID, result.getString(CHAT_UID));
				r.add(jo);
			}
		} catch (Throwable t) {
			log.warn(t.getMessage());
		} finally {
			DbUtils.closeQuietly(result);
			DbUtils.closeQuietly(ps);
			DbUtils.closeQuietly(connection);
		}
		return r;
	}

	/*
	 * Store this mail into our MAIL_TABLE for future reference
	 */
	public void storeMail(final ResolvedLink r, MimeMessage mimeMessage, String chatUid) {
		Connection connection = null;
		PreparedStatement ps = null;
		try {
			String subject = mimeMessage.getSubject();
			String body = MailUtils.getTextFromMimeMessage(mimeMessage);

			log.debug("Storing mail for {}", r);
			connection = getConnection();
			connection.setAutoCommit(false);
			ps = connection.prepareStatement("INSERT INTO " + MAIL_TABLE + " VALUES (?,?,?,?,?,?)");
			ps.setString(1, r.getUidA());
			ps.setString(2, r.getUidB());
			ps.setString(3, subject);
			ps.setString(4, body);
			ps.setTimestamp(5, new Timestamp(new Date().getTime()));
			ps.setString(6, chatUid);
			ps.execute();
			connection.commit();
			connection.close();
		} catch (Throwable t) {
			log.warn("Something bad happened", t);
		} finally {
			DbUtils.closeQuietly(ps);
			DbUtils.closeQuietly(connection);
		}
	}

	/*
	 * Get the link based on the uid
	 */
	public Link getLink(Link link) {
		return getLinkByUid(link.getUidA(), link.getUidB());
	}

//	private void debugDb() throws Exception {
//		log.info("DB dump");
//		Connection connection = getConnection();
//		PreparedStatement ps = connection.prepareStatement("SELECT * FROM " + LINK_TABLE + ";");
//		ResultSet result = ps.getResultSet();
//		while (result.next()) {
//			log.info(result.getString(A_UID) + //
//					result.getString(B_UID) + //
//					result.getTimestamp(EXPIRY) + //
//					result.getTimestamp(CREATED) + //
//					result.getTimestamp(MODIFIED));
//		}
//	}

	/*
	 * Get the link based on the uid
	 */
	public Link getLinkByUid(String uidA, String uidB) {
		Link r = null;
		Connection connection = null;
		PreparedStatement ps = null;
		ResultSet result = null;
		try {

			;
			connection = getConnection();
			ps = connection.prepareStatement("SELECT * FROM " + LINK_TABLE + " WHERE (" + A_UID + "=? AND " + B_UID
					+ "=?) OR (" + A_UID + "=? AND " + B_UID + "=?)");
			ps.setString(1, uidA);
			ps.setString(2, uidB);
			ps.setString(3, uidB);
			ps.setString(4, uidA);
			ps.execute();
			result = ps.getResultSet();
			if (result.next()) {
				r = new Link( //
						result.getString(A_UID), //
						result.getString(B_UID), //
						result.getTimestamp(EXPIRY), //
						result.getTimestamp(CREATED), //
						result.getTimestamp(MODIFIED));
				return r;
			} else {
				log.info("No link found");
			}

		} catch (Throwable t) {
			log.warn(t.getMessage());
		} finally {
			DbUtils.closeQuietly(result);
			DbUtils.closeQuietly(ps);
			DbUtils.closeQuietly(connection);
		}
		return r;
	}

	@Override
	public boolean acceptUid(String uidA, String uidB) {
		if (Config.getInstance().getBoolean(Config.MAILGUARD_SMTP_IN_AUTO_ACCEPT)) {
			try {
				setLink(new Link(uidA, uidB));
				return true;
			} catch (Throwable e) {
				return false;
			}
		}
		return containsByUid(uidA, uidB);
	}

}
