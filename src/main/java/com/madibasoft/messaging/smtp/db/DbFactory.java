package com.madibasoft.messaging.smtp.db;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.madibasoft.messaging.smtp.Config;

public class DbFactory {
	private static final Logger log = LoggerFactory.getLogger(DbFactory.class);

	public static DbInterface getDatabase()
			throws SQLException, InstantiationException, IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException {
		log.debug("Instantiating database {}", Config.getInstance().getString(Config.MAILGUARD_DBCLASS));
		return (DbInterface) Class.forName(Config.getInstance().getString(Config.MAILGUARD_DBCLASS)).getConstructor()
				.newInstance();
	}
}
