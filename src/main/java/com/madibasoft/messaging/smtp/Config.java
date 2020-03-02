package com.madibasoft.messaging.smtp;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.SystemConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Config extends CompositeConfiguration {
	private static final Logger log = LoggerFactory.getLogger(Config.class);
	public static final String CONFIG_NAME = "mailguard.properties";

	private static Config configInstance;
	public static final String MAILGUARD_DB_NAME = "mailguard.dbname";
	public static final String MAILGUARD_H2_DB_PATH = "mailguard.h2.dbpath";
	public static final String MAILGUARD_USERSERVICE = "mailguard.userservice";
	public static final String MAILGUARD_DBCLASS = "mailguard.dbclass";

	public static final String MAILGUARD_PUBLIC_HOST = "mailguard.public.host";
	public static final String MAILGUARD_CUSTOM_PROPERTIES = "mailguard.custom.properties";
	public static final String MAILGUARD_LINK_VALIDITY = "mailguard.link.validity";
	public static final String MAILGUARD_REJECTION_SUBJECT = "mailguard.rejection.subject";
	public static final String MAILGUARD_REJECTION_BODY = "mailguard.rejection.body";

	// HTTP service settings
	public static final String MAILGUARD_HTTP_HOST = "mailguard.http.host";
	public static final String MAILGUARD_HTTP_PORT = "mailguard.http.port";
	public static final String MAILGUARD_HTTP_SECRET = "mailguard.http.secret";

	// Inbound SMTP service settings
	public static final String MAILGUARD_SMTP_IN_HOST = "mailguard.smtp.in.host";
	public static final String MAILGUARD_SMTP_IN_PORT = "mailguard.smtp.in.port";
	public static final String MAILGUARD_SMTP_MAX_IN_CONNECTIONS = "mailguard.smtp.in.max.connections";
	public static final String MAILGUARD_SMTP_IN_ACCEPT = "mailguard.smtp.in.accept";
	public static final String MAILGUARD_SMTP_IN_AUTO_ACCEPT = "mailguard.smtp.in.auto_accept";
	public static final String MAILGUARD_SMTP_IN_MAX_MAIL_SIZE = "mailguard.smtp.in.max_mail_size";

	// Outbound SMTP service settings
	public static final String MAILGUARD_SMTP_OUT_TYPE = "mailguard.smtp.out.type";
	// how to route incoming smtp messages
	public static final String MAILGUARD_SMTP_FORWARDING_TYPE = "mailguard.smtp.forwarding.type";

	/* milliseconds to retry outbound */
	public static final String MAILGUARD_SMTP_OUT_DIRECT_EXPIRY = "mailguard.smtp.out.direct.expiry";
	public static final String MAILGUARD_SMTP_OUT_HOST = "mailguard.smtp.out.host";
	public static final String MAILGUARD_SMTP_OUT_PORT = "mailguard.smtp.out.port";
	public static final String MAILGUARD_SMTP_OUT_USERNAME = "mailguard.smtp.out.username";
	public static final String MAILGUARD_SMTP_OUT_PASSWORD = "mailguard.smtp.out.password";

	// mysql settings
	public static final String MAILGUARD_MYSQL_HOST = "mailguard.mysql.host";
	public static final String MAILGUARD_MYSQL_PORT = "mailguard.mysql.port";
	public static final String MAILGUARD_MYSQL_USERNAME = "mailguard.mysql.username";
	public static final String MAILGUARD_MYSQL_PASSWORD = "mailguard.mysql.password";
	public static final String MAILGUARD_MESSAGING_API = "mailguard.messaging.api";

	public enum SmtpOutType {
		dummy, smtp, direct
	};

	public enum ForwardingType {
		email, chat, both
	};

	private Config() {
		setupSystem();
		setupCustom();
		setupBuiltin();
	}

	/**
	 * Setup builtin.
	 */
	private void setupBuiltin() {
		try {
			log.debug("Loading built-in config ({})", getClass().getResource("/" + CONFIG_NAME));
			addConfiguration(new PropertiesConfiguration(getClass().getResource("/" + CONFIG_NAME)));
		} catch (Throwable t) {
			log.warn("Problem loading built-in config :{}", t.getMessage());
		}
	}

	/**
	 * Setup custom.
	 */
	private void setupCustom() {
		try {
			PropertiesConfiguration builtin = new PropertiesConfiguration(getClass().getResource("/" + CONFIG_NAME));
			PropertiesConfiguration pconfig;
			String fileName = builtin.getString(MAILGUARD_CUSTOM_PROPERTIES);
			log.debug("Looking for custom config in {}", builtin.getString(MAILGUARD_CUSTOM_PROPERTIES));
			if (new File(fileName).exists()) {
				log.info("Loading custom config from home ({})", builtin.getString(MAILGUARD_CUSTOM_PROPERTIES));
				pconfig = new PropertiesConfiguration(builtin.getString(MAILGUARD_CUSTOM_PROPERTIES));
				FileChangedReloadingStrategy strategy = new FileChangedReloadingStrategy();
				strategy.setRefreshDelay(30000);
				pconfig.setReloadingStrategy(strategy);
			} else {
				log.info("No custom over-ride found in {}", fileName);
				pconfig = new PropertiesConfiguration();
			}

			addConfiguration(pconfig);
		} catch (Throwable e) {
			Utils.jsonError(log, "Problem loading custom configuration", e);
		}
	}

	/**
	 * Setup system.
	 */
	private void setupSystem() {
		log.debug("Loading system config");
		addConfiguration(new SystemConfiguration());
	}

	public static Config getInstance() {
		if (configInstance == null) {
			log.debug("Instanciating configuration");
			configInstance = new Config();
		}
		return configInstance;
	}

	public List<String> getStringList(String key) {
		List<Object> l = getList(key);
		List<String> res = new ArrayList<String>();
		for (Object o : l) {
			if (o.toString().length() > 0)
				res.add(o.toString());
		}
		return res;
	}

	public void setStringList(String key, List<String> list) {
		StringBuffer s = new StringBuffer();
		for (String entry : list) {
			s.append(entry).append(super.getListDelimiter());
		}
		if (s.length() > 0) {
			setProperty(key, s.substring(0, s.length() - 1));
		} else {
			setProperty(key, "");
		}
	}

	public String getString(String key) {
		return super.getString(key);
	}

	public void setString(String key, String value) {
		super.setProperty(key, value);
	}

	public static List<String> toList(String props) {
		List<String> res = new ArrayList<String>();
		StringTokenizer tok = new StringTokenizer(props, ",");
		while (tok.hasMoreTokens()) {
			res.add(tok.nextToken().trim());
		}
		return res;
	}

	public static String toString(List<String> list) {
		StringBuffer sb = new StringBuffer();
		for (String s : list) {
			sb.append(s).append(',');
		}
		if (list.size() > 0)
			return sb.substring(0, sb.length() - 1);
		else
			return sb.toString();
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		Iterator<String> keys = getKeys();
		while (keys.hasNext()) {
			String key = keys.next();
			sb.append(key).append('=').append(getProperty(key)).append('\n');
		}
		return sb.toString();
	}

	public SmtpOutType getSmtpOutType() {
		return SmtpOutType.valueOf(getString(Config.MAILGUARD_SMTP_OUT_TYPE));
	}

	public ForwardingType getForwardingType() {
		return ForwardingType.valueOf(getString(Config.MAILGUARD_SMTP_FORWARDING_TYPE));
	}
}