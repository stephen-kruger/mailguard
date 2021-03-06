package com.madibasoft.messaging.smtp.db;

import java.sql.Connection;
import java.sql.SQLException;

import org.h2.jdbcx.JdbcConnectionPool;
import org.h2.tools.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.madibasoft.messaging.smtp.Config;
import com.madibasoft.messaging.smtp.Utils;

public class H2 extends DbAbstract {
	private static final Logger log = LoggerFactory.getLogger(H2.class);
	private Server h2Server;
	private Connection conn = null;
	private JdbcConnectionPool cp;

	public H2() throws SQLException {
		h2Server = Server.createTcpServer(new String[] {});
		String url = "jdbc:h2:" + getDbName() + ";create=true";
		cp = JdbcConnectionPool.create(url,"","");
	}

	public void start() throws Exception {
		if (h2Server.isRunning(false)) {
			throw new RuntimeException("\"H2  Storage instance already started\"");
		}
		h2Server.start();
		setupTables();
		log.debug("Started H2 repository.");

	}

	public void stop() {
		try {
			if ((conn != null) && (!conn.isClosed()))
				conn.close();

			h2Server.stop();
		} catch (Throwable t) {
			Utils.jsonError(log, "Problem stopping H2 connection", t);
		}
		log.debug("Stopped H2 repository");
	}

	public Connection getConnection() throws Exception {
		if ((conn == null) || (conn.isClosed())) {
			log.debug("Creating new connection");
			conn = cp.getConnection();
		}
		return conn;
	}

	private String getDbName() {
		return Config.getInstance().getString(Config.MAILGUARD_H2_DB_PATH);
	}

}