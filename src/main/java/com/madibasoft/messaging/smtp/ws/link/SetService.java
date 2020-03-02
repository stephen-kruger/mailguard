package com.madibasoft.messaging.smtp.ws.link;

import java.security.InvalidParameterException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.madibasoft.messaging.smtp.Link;
import com.madibasoft.messaging.smtp.MissingParameterException;
import com.madibasoft.messaging.smtp.db.DbInterface;
import com.madibasoft.messaging.smtp.user.UserServiceFactory;
import com.madibasoft.messaging.smtp.ws.AbstractJsonService;
import com.madibasoft.messaging.smtp.ws.message.SendService;

public class SetService extends AbstractJsonService {
	public static final String TO_UID_PARAM = "toUid";
	public static final String FROM_UID_PARAM = "fromUid";
	public static final String SUBJECT_QUERY = "subject";
	private static final Logger log = LoggerFactory.getLogger(SetService.class);
	private DbInterface db;

	public SetService(DbInterface db) {
		this.db = db;
	}

	public JsonObject set(String toUserId, String fromUserId) throws Exception {
		// check the parameters
		if (toUserId == null)
			throw new MissingParameterException("Missing parameter " + SendService.TO_UID_PARAM);
		if (fromUserId == null)
			throw new MissingParameterException("Missing parameter " + FROM_UID_PARAM);

		// make sure we can't send to ourselves
		if (toUserId.equals(fromUserId)) {
			throw new InvalidParameterException("Sender and recipient id cannot be the same");
		}

//		// ensure they are valid uid's
		UserServiceFactory.getInstance().isValidUid(toUserId);
		UserServiceFactory.getInstance().isValidUid(fromUserId);

		// store this link for future checking
		Link r = db.setLink(new Link(toUserId, fromUserId));

		return r.getAsJson();
	}

	@Override
	protected JsonObject invoke(Map<String, String[]> map, String body) throws Exception {
		log.debug(map.toString());
		return set(getString(map, TO_UID_PARAM, null), getString(map, FROM_UID_PARAM, null));

	}

}
