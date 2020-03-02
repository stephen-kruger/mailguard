package com.madibasoft.messaging.smtp.ws.message;

import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.madibasoft.messaging.smtp.db.DbInterface;
import com.madibasoft.messaging.smtp.ws.AbstractJsonService;

public class ListService extends AbstractJsonService {
	public static final String OFFSET_QUERY = "offset";
	public static final String COUNT_QUERY = "count";
	private DbInterface db;

	public ListService(DbInterface db) {
		this.db = db;
	}

	public JsonObject list(int offset, int count) throws Exception {
		JsonArray result = new JsonArray();
		for (JsonObject r : db.listMails(offset, count)) {
			result.add(r);
		}
		JsonObject r = new JsonObject();
		r.add("result", result);
		return r;
	}

	@Override
	protected JsonObject invoke(Map<String, String[]> map, String body) throws Exception {
		return list(getInt(map, OFFSET_QUERY, 0), getInt(map, COUNT_QUERY, 10));
	}

}
