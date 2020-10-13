package com.madibasoft.messaging.smtp.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Used for stress testing
 */
public class RandomUserService implements UserServiceInterface {
	private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

	public RandomUserService() {
		log.info("Loading RandomUserService");
	}

	@Override
	public String lookupEmailByUid(String uid) throws UserNotFoundException {
		return uid + "@here.com";
	}

	@Override
	public String lookupProxyByUid(String uid) throws UserNotFoundException {
		return "proxy." + uid + "@localhost";

	}

	@Override
	public String lookupUidByEmail(String email) throws UserNotFoundException {
		if (email == null) {
			throw new UserNotFoundException("Email null");
		}
		return email.substring(0, email.indexOf('@'));
	}

	@Override
	public String lookupUidByProxy(String proxy) {
		return proxy.substring(proxy.indexOf('.') + 1, proxy.indexOf('@'));
	}

	@Override
	public String lookupProxyByEmail(String email) throws UserNotFoundException {
		String uid = lookupUidByEmail(email);
		return lookupProxyByUid(uid);
	}

	@Override
	public boolean isValidUid(String uid) throws UserNotFoundException {
		lookupEmailByUid(uid);
		return true;
	}

}
