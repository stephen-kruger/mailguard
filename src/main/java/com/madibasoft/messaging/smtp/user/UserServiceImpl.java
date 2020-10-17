package com.madibasoft.messaging.smtp.user;

import java.io.File;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.madibasoft.messaging.smtp.Config;
import com.madibasoft.messaging.smtp.MailUtils;
import com.madibasoft.messaging.smtp.Utils;

/*
 * A production ready service should implement UserServiceInterface against some existing identity system. This is a simple properties file based example for illustration.
 */
public class UserServiceImpl implements UserServiceInterface {
	private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);
	private PropertiesConfiguration props;

	public UserServiceImpl() {
		try {
			String fileName = Config.getInstance().getString("mailguard_custom_user_properties");
			log.info("Looking for user config in {}", fileName);
			if (new File(fileName).exists()) {
				log.info("Loading custom user from home ({})", fileName);
				props = new PropertiesConfiguration();
				props.load(fileName);
			} else {
				log.info("Loading internal user config");
				props = new PropertiesConfiguration(getClass().getResource("/user.properties"));
			}
		} catch (ConfigurationException e) {
			Utils.jsonError(log, "Something bad happened", e);
		}
	}

	@Override
	public String lookupEmailByUid(String uid) throws UserNotFoundException {
		String email = props.getString(uid);
		if (email == null) {
			throw new UserNotFoundException("Invalid uid " + uid);
		}
		return MailUtils.cleanEmail(email);
	}
	
	@Override
	public String lookupEmailByProxy(String proxyEmail) throws UserNotFoundException {
		return lookupEmailByUid(lookupUidByProxy(proxyEmail));
	}

	@Override
	public String lookupProxyByUid(String uid) throws UserNotFoundException {
		String email = lookupEmailByUid(uid);
		String proxy = lookupProxyByEmail(email);
		return MailUtils.cleanEmail(proxy);
	}

	@Override
	public String lookupProxyByEmail(String email) throws UserNotFoundException {
		if (!props.containsKey(email)) {
			throw new UserNotFoundException("No email found :" + email);
		}
		return MailUtils.cleanEmail(props.getString(email));
	}

	@Override
	public String lookupUidByEmail(String email) throws UserNotFoundException {
		if (email != null) {
			if (props.containsKey(email))
				return props.getString(props.getString(email));
		} else {
			log.error("No uid found matching {}", email);
		}
		throw new UserNotFoundException("No uid found for email " + email);
	}

	public String lookupUidByProxy(String proxyEmail) {
		return props.getString(proxyEmail);
	}

	@Override
	public boolean isValidUid(String uid) throws UserNotFoundException {
		lookupEmailByUid(uid);
		return true;
	}

	@Override
	public void addUser(String uid, String proxyMail, String realMail) {
		props.setProperty(uid,realMail);
		log.info(uid+"<<<<<<<<<<>>>>>>>"+realMail);
		props.setProperty(realMail,proxyMail);
		log.info(realMail+"<<<<<<<<<<>>>>>>>"+proxyMail);
		props.setProperty(proxyMail,uid);
		log.info(proxyMail+"<<<<<<<<<<>>>>>>>"+uid);
	}

}
