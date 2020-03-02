package com.madibasoft.messaging.smtp;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.http.HttpStatus;
import org.masukomi.aspirin.Aspirin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subethamail.smtp.server.SMTPServer;

import com.madibasoft.messaging.smtp.db.DbFactory;
import com.madibasoft.messaging.smtp.db.DbInterface;
import com.madibasoft.messaging.smtp.user.UserNotFoundException;
import com.madibasoft.messaging.smtp.ws.AuthFilter;
import com.madibasoft.messaging.smtp.ws.link.ExpireService;
import com.madibasoft.messaging.smtp.ws.link.SetService;
import com.madibasoft.messaging.smtp.ws.message.ListService;
import com.madibasoft.messaging.smtp.ws.message.SendService;

public class MailGuard {
	private static final Logger log = LoggerFactory.getLogger(MailGuard.class);
	private static final String NAME = "mailguard.name";
	private SMTPServer smtpServer;
	private DbInterface db;

	public MailGuard(MessageHandlerFactoryImpl mhf)
			throws SQLException, InstantiationException, IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException {

		log.info("Initialising {}", Config.getInstance().getString(NAME));
		// init our db
		db = DbFactory.getDatabase();
		mhf.setDb(db);

		smtpServer = SMTPServer //
				.port(Config.getInstance().getInt(Config.MAILGUARD_SMTP_IN_PORT)) //
				.connectionTimeout(1, TimeUnit.MINUTES) //
				.backlog(100) //
				.requireTLS(false) //
				.hideTLS(true) //
				.enableTLS(false) //
				.hostName(Config.getInstance().getString(Config.MAILGUARD_SMTP_IN_HOST)) //
				.maxMessageSize(Config.getInstance().getInt(Config.MAILGUARD_SMTP_IN_MAX_MAIL_SIZE, 100000)) //
				.maxConnections(Config.getInstance().getInt(Config.MAILGUARD_SMTP_MAX_IN_CONNECTIONS)) //
				.maxRecipients(200) //
				.softwareName(Config.getInstance().getString(NAME)) //
				.messageHandlerFactory(mhf).build();
	}

	public synchronized void start() {
		smtpServer.start();
		log.info("Started " + Config.getInstance().getString(NAME));
		Config config = Config.getInstance();
		// start the database
		int retryCount = 10;
		while (retryCount-- > 0) {
			try {
				db.start();
				retryCount = 0;
			} catch (Exception e) {
				log.info("Unable to connect, retrying on 10 seconds ({})", e.getMessage());
				try {
					Thread.sleep(10000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
		}

		// configure the web service
		spark.Spark.ipAddress(config.getString(Config.MAILGUARD_HTTP_HOST));
		log.info("Binding http to {}", config.getString(Config.MAILGUARD_HTTP_HOST));
		spark.Spark.port(config.getInt(Config.MAILGUARD_HTTP_PORT));
		// add api key auth filter to all paths
		spark.Spark.before(new AuthFilter());

		// define the error responses
		spark.Spark.notFound("<html><body><h1>404 Not found</h1></body></html>");
		spark.Spark.exception(UserNotFoundException.class, (e, req, res) -> {
			res.status(HttpStatus.BAD_REQUEST_400);
			res.body(e.getMessage());
		});
		spark.Spark.exception(MissingParameterException.class, (e, req, res) -> {
			res.status(HttpStatus.BAD_REQUEST_400);
			res.body(e.getMessage());
		});
		spark.Spark.exception(Exception.class, (e, req, res) -> {
			res.status(HttpStatus.INTERNAL_SERVER_ERROR_500);
			res.body(e.getMessage());
		});

		// define the web services
		spark.Spark.delete("/link/expire", "text/plain", new ExpireService(db));
		spark.Spark.put("/link/set", "text/plain", new SetService(db));
		spark.Spark.get("/link//list", "application/json", new com.madibasoft.messaging.smtp.ws.link.ListService(db));
		spark.Spark.get("/message/list", "application/json", new ListService(db));
		spark.Spark.post("/message/send", "text/plain", new SendService(db));

		// start the web service
		spark.Spark.init();
		spark.Spark.awaitInitialization();
	}

	public synchronized void stop() {
		smtpServer.stop();
		log.info("Stopped " + smtpServer.getSoftwareName());
		db.stop();
		spark.Spark.stop();
		spark.Spark.awaitStop();
		Aspirin.shutdown();
	}

	public DbInterface getDb() {
		return db;
	}

	public static final void main(String[] args) {
		try {
			MailGuard server = new MailGuard(new MessageHandlerFactoryImpl(new Inbox(),
					Config.getInstance().getInt(Config.MAILGUARD_SMTP_IN_MAX_MAIL_SIZE, 100000)));
			server.start();
		} catch (Throwable t) {
			Utils.jsonError(log, "Main program loop", t);
		}
	}

}
