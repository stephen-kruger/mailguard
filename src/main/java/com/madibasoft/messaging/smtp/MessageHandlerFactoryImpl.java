package com.madibasoft.messaging.smtp;

import org.subethamail.smtp.helper.BasicMessageHandlerFactory;
import org.subethamail.smtp.helper.BasicMessageListener;

import com.madibasoft.messaging.smtp.db.DbInterface;

public class MessageHandlerFactoryImpl extends BasicMessageHandlerFactory {

	private BasicMessageListener inbox;

	public MessageHandlerFactoryImpl(BasicMessageListener listener, int maxMessageSize) {
		super(listener, maxMessageSize);
		this.inbox = listener;
	}

	public void setDb(DbInterface db) {
		((Inbox) inbox).setDb(db);
	}

}
