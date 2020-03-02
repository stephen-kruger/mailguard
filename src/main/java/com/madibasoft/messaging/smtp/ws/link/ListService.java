package com.madibasoft.messaging.smtp.ws.link;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.madibasoft.messaging.smtp.Link;
import com.madibasoft.messaging.smtp.db.DbInterface;
import com.madibasoft.messaging.smtp.ws.AbstractJsonService;

public class ListService extends AbstractJsonService {
	public static final String USERID_QUERY = "uid";
	public static final String OFFSET_QUERY = "offset";
	public static final String COUNT_QUERY = "count";
	private static final Logger log = LoggerFactory.getLogger(ListService.class);
	private DbInterface db;

	public ListService(DbInterface db) {
		this.db = db;
	}

	public JsonObject list(String userId, int offset, int count) throws Exception {
		JsonArray result = new JsonArray();
		for (Link r : db.listLinks(userId, offset, count)) {
			result.add(r.getAsJson());
		}
		JsonObject r = new JsonObject();
		r.add("result", result);
		return r;
	}

	@Override
	public JsonObject invoke(Map<String, String[]> map, String body) throws Exception {
		log.debug(map.toString());
		return list(getString(map, USERID_QUERY, null), getInt(map, OFFSET_QUERY, 0), getInt(map, COUNT_QUERY, 10));
	}

}
