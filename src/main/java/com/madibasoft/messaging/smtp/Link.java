package com.madibasoft.messaging.smtp;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

public class Link {

	private static final Logger log = LoggerFactory.getLogger(Link.class);
	private Date expiry = new Date(0), created = new Date(0), modified = new Date(0);

	private String uidA = null, uidB = null;

	public Link() {
		setExpiry(new Date(new Date().getTime() + Config.getInstance().getLong(Config.MAILGUARD_LINK_VALIDITY)));
	}

	public Link(String uidA, String uidB) {
		this(uidA, uidB, new Date(new Date().getTime() + Config.getInstance().getLong(Config.MAILGUARD_LINK_VALIDITY)));
	}

	public Link(String uidA, String uidB, Date expiry) {
		this(uidA, uidB, expiry, new Date(), new Date());
	}

	public Link(String uidA, String uidB, Date expiry, Date created, Date modified) {
		this();
		setUidA(uidA);
		setUidB(uidB);
		setExpiry(expiry);
		setCreated(created);
		setModified(modified);
	}

	public String getUidA() {
		return uidA;
	}

	public void setUidA(String uidA) {
		this.uidA = uidA;
	}

	public String getUidB() {
		return uidB;
	}

	public void setUidB(String uidB) {
		this.uidB = uidB;
	}

	public Date getExpiry() {
		return expiry;
	}

	public void setExpiry(Date expiry) {
		this.expiry = expiry;
	}

	public Date getCreated() {
		return created;
	}

	public void setCreated(Date created) {
		this.created = created;
	}

	public Date getModified() {
		return modified;
	}

	public void setModified(Date modified) {
		this.modified = modified;
	}

	public boolean isValidLink() {
		if (getExpiry().after(new Date(new java.util.Date().getTime()))) {
			log.debug("Expires at {}, current time is {}", getExpiry(), new Date());
			return true;
		} else {
			log.debug("Expired at {}, current time is {}", getExpiry(), new Date());
			return false;
		}
	}

	public String toString() {
		return getAsJson().toString();
	}

	public JsonObject getAsJson() {
		JsonObject jo = new JsonObject();
		jo.addProperty("uidA", this.getUidA());
		jo.addProperty("uidB", this.getUidB());
		jo.addProperty("expires", getExpiry().toString());
		jo.addProperty("modified", getModified().toString());
		jo.addProperty("created", getCreated().toString());
		return jo;
	}

}
