package com.madibasoft.messaging.smtp.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.madibasoft.messaging.smtp.Config;
import com.madibasoft.messaging.smtp.Utils;

public class MySQL extends DbAbstract implements DbInterface {
	private static final Logger log = LoggerFactory.getLogger(MySQL.class);
	private Connection conn = null;
	private boolean running = false;

	public MySQL() throws SQLException {
		log.info("Connecting to MYSQL on {}", Config.getInstance().getString(Config.MAILGUARD_MYSQL_HOST));
	}

	@Override
	public void start() throws Exception {
		if (running) {
			throw new RuntimeException("database already running");
		}
		setupTables();
		log.debug("Started H2 repository.");
		running = true;
	}

	@Override
	public void stop() {
		try {
			if ((conn != null) && (!conn.isClosed()))
				conn.close();
		} catch (Throwable t) {
			Utils.jsonError(log, "Problem shutting down MySQL connection", t);
		}
		log.debug("Stopped H2 repository");
	}

	@Override
	public Connection getConnection() throws Exception {
		if ((conn == null) || (conn.isClosed())) {
			Config config = Config.getInstance();
			Class.forName("com.mysql.cj.jdbc.Driver");
			return conn = DriverManager.getConnection(
					"jdbc:mysql://" + config.getString(Config.MAILGUARD_MYSQL_HOST) + ":"
							+ config.getString(Config.MAILGUARD_MYSQL_PORT) + "/" + getDbName()
							+ "?rewriteBatchedStatements=true&relaxAutoCommit=true",
					config.getString(Config.MAILGUARD_MYSQL_USERNAME),
					config.getString(Config.MAILGUARD_MYSQL_PASSWORD));
		}
		return conn;
	}

	private String getDbName() {
		return Config.getInstance().getString(Config.MAILGUARD_DB_NAME);
	}

}