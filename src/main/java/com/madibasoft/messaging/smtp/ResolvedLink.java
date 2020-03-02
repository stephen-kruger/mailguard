package com.madibasoft.messaging.smtp;

import com.google.gson.JsonObject;
import com.madibasoft.messaging.smtp.user.UserNotFoundException;
import com.madibasoft.messaging.smtp.user.UserServiceFactory;
import com.madibasoft.messaging.smtp.user.UserServiceInterface;

/**
 * The Class ResolvedLink.
 */
public class ResolvedLink extends Link {

	private String clearA, clearB, proxyA, proxyB = null;

	/**
	 * Instantiates a new resolved link.
	 *
	 * @param link the link with uid values that will populate this ResolvedLink
	 * @throws UserNotFoundException the user not found exception
	 */
	public ResolvedLink(Link link) throws UserNotFoundException {
		super(link.getUidA(), link.getUidB(), link.getExpiry(), link.getCreated(), link.getModified());
		UserServiceInterface userService = UserServiceFactory.getInstance();
		setClearA(userService.lookupEmailByUid(getUidA()));
		setClearB(userService.lookupEmailByUid(getUidB()));
		setProxyA(userService.lookupProxyByUid(getUidA()));
		setProxyB(userService.lookupProxyByUid(getUidB()));
	}

	/**
	 * Gets the clear email A.
	 *
	 * @return the clear A
	 */
	public String getClearA() {
		return clearA;
	}

	/**
	 * Sets the clear email A.
	 *
	 * @param clearA the new clear A
	 */
	public void setClearA(String clearA) {
		this.clearA = clearA;
	}

	/**
	 * Gets the clear email B.
	 *
	 * @return the clear B
	 */
	public String getClearB() {
		return clearB;
	}

	/**
	 * Sets the clear email B.
	 *
	 * @param clearB the new clear B
	 */
	public void setClearB(String clearB) {
		this.clearB = clearB;
	}

	/**
	 * Gets the proxy A.
	 *
	 * @return the proxy A
	 */
	public String getProxyA() {
		return proxyA;
	}

	/**
	 * Sets the proxy A.
	 *
	 * @param proxyA the new proxy A
	 */
	public void setProxyA(String proxyA) {
		this.proxyA = proxyA;
	}

	/**
	 * Gets the proxy B.
	 *
	 * @return the proxy B
	 */
	public String getProxyB() {
		return proxyB;
	}

	/**
	 * Sets the proxy B.
	 *
	 * @param proxyB the new proxy B
	 */
	public void setProxyB(String proxyB) {
		this.proxyB = proxyB;
	}

	/**
	 * Swop to ensure clearEmail always matches clearA, swapping A and B if needed
	 *
	 * @param clearEmail the clear email
	 */
	public void swop(final String clearEmail) {
		if (!getClearA().equals(clearEmail)) {
			String temp = getClearA();
			setClearA(getClearB());
			setClearB(temp);
			temp = getProxyA();
			setProxyA(getProxyB());
			setProxyB(temp);
			temp = getUidA();
			setUidA(getUidB());
			setUidB(temp);
		}
		// else we are already in the right order
	}

	/**
	 * Gets the structure as json.
	 *
	 * @return the json
	 */
	public JsonObject getAsJson() {
		JsonObject jo = super.getAsJson();
		jo.addProperty("clearA", this.getClearA());
		jo.addProperty("proxyA", this.getProxyA());
		jo.addProperty("clearB", this.getClearB());
		jo.addProperty("proxyB", this.getProxyB());
		return jo;
	}

}
