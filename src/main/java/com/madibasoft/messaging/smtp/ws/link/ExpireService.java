package com.madibasoft.messaging.smtp.ws.link;

import java.security.InvalidParameterException;
import java.util.Date;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.madibasoft.messaging.smtp.Link;
import com.madibasoft.messaging.smtp.MissingParameterException;
import com.madibasoft.messaging.smtp.db.DbInterface;
import com.madibasoft.messaging.smtp.ws.AbstractJsonService;

public class ExpireService extends AbstractJsonService {
	private static final Logger log = LoggerFactory.getLogger(ExpireService.class);
	public static final String TO_UID_PARAM = "toUid";
	public static final String FROM_UID_PARAM = "fromUid";
	private DbInterface db;

	public ExpireService(DbInterface db) {
		this.db = db;
	}

	public JsonObject expire(String toUserId, String fromUserId) throws Exception {
		// check the parameters
		if (toUserId == null)
			throw new MissingParameterException("Missing parameter " + ExpireService.TO_UID_PARAM);
		if (fromUserId == null)
			throw new MissingParameterException("Missing parameter " + FROM_UID_PARAM);
		if (toUserId.equals(fromUserId)) {
			throw new InvalidParameterException("To and From uid cannot be the same");
		}

		Link link = new Link(toUserId, fromUserId, new Date(0));
		if (db.containsLink(link)) {
			link = db.setLink(link);
			log.debug("Updated this link with expired status {}", link);
		} else {
			throw new InvalidParameterException("No link exists");
		}

		return link.getAsJson();

	}

	@Override
	protected JsonObject invoke(Map<String, String[]> map, String body) throws Exception {
		return expire(getString(map, TO_UID_PARAM, null), getString(map, FROM_UID_PARAM, null));
	}

}
