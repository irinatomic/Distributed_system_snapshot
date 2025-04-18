package com.kids.communication.handler.impl;

import com.kids.servent.config.AppConfig;
import com.kids.communication.handler.MessageHandler;
import com.kids.communication.message.Message;
import lombok.RequiredArgsConstructor;

/**
 * This will be used if no proper handler is found for the message.
 */
@RequiredArgsConstructor
public class NullHandler implements MessageHandler {

	private final Message clientMessage;
	
	@Override
	public void run() {
		AppConfig.timestampedErrorPrint("Couldn't handle message: " + clientMessage);
	}
}
