package com.madibasoft.messaging.smtp.ws;

import java.util.Map;

import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.madibasoft.messaging.smtp.Utils;

import spark.Request;
import spark.Response;
import spark.Route;

public abstract class AbstractJsonService implements Route {
	private static final Logger log = LoggerFactory.getLogger(AbstractJsonService.class);

	protected abstract JsonObject invoke(Map<String, String[]> map, String body) throws Exception;

	public String getString(Map<String, String[]> map, String key, String defaultValue) {
		try {
			if (map.containsKey(key)) {
				return map.get(key)[0];
			}
		} catch (Throwable t) {
			Utils.jsonError(log, "Problem reading ws parameter", t);
		}
		return defaultValue;
	}

	public int getInt(Map<String, String[]> map, String key, int defaultValue) {
		try {
			if (map.containsKey(key)) {
				return Integer.parseInt(map.get(key)[0]);
			}
		} catch (Throwable t) {
			Utils.jsonError(log, "Problem reading ws parameter", t);
		}
		return defaultValue;
	}

	public static JsonObject result(String message) {
		JsonObject element = new JsonObject();
		element.addProperty("result", message);
		// return result(element);
		return element;
	}

//	public static JsonArray result(JsonObject element) {
//		JsonArray ja = new JsonArray();
//		ja.add(element);
//		return ja;
//	}

	@Override
	public Object handle(Request request, Response response) throws Exception {
		log.info("{} from {}", request.pathInfo(), request.ip());
		response.type("application/json");
		response.status(HttpStatus.OK_200);
		try {
			return invoke(request.queryMap().toMap(), request.body());
		} catch (Throwable t) {
			response.status(HttpStatus.BAD_REQUEST_400);
			return result(t.getMessage());
		}
	}

}
