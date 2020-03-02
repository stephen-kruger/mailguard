package com.madibasoft.messaging.smtp.user;

import java.lang.reflect.InvocationTargetException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.madibasoft.messaging.smtp.Config;

public class UserServiceFactory {
	private static final Logger log = LoggerFactory.getLogger(UserServiceFactory.class);
	private static UserServiceInterface usi;

	/*
	 * Mainly used in Unit tests to override the default user service, but can be
	 * used by anyone
	 */
	public static void setInstance(String className)
			throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
			NoSuchMethodException, SecurityException, ClassNotFoundException {
		log.debug("Setting user service implementation to {}", className);
		usi = (UserServiceInterface) Class.forName(className).getConstructor().newInstance();
	}

	public static UserServiceInterface getInstance() {
		if (usi == null) {
			try {
				setInstance(Config.getInstance().getString(Config.MAILGUARD_USERSERVICE));
			} catch (Throwable e) {
				log.error("Unable to load user service {}",
						Config.getInstance().getString(Config.MAILGUARD_USERSERVICE));
			}
		}
		return usi;
	}
}
