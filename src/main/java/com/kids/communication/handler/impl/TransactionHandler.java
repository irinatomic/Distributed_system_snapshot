package com.kids.communication.handler.impl;

import com.kids.servent.bitcake.BitcakeManagerInstance;
import com.kids.servent.config.AppConfig;
import com.kids.communication.message.util.CausalBroadcast;
import com.kids.servent.bitcake.BitcakeManager;
import com.kids.servent.bitcake.ABBitcakeManager;
import com.kids.communication.handler.MessageHandler;
import com.kids.communication.message.Message;
import com.kids.communication.message.MessageType;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TransactionHandler implements MessageHandler {

	private final Message clientMessage;
	private final BitcakeManager bitcakeManager;

	public TransactionHandler(Message clientMessage) {
		this.clientMessage = clientMessage;
		this.bitcakeManager = BitcakeManagerInstance.getInstance();
	}

	@Override
	public void run() {
		if (clientMessage.getMessageType() == MessageType.TRANSACTION) {
			String amountString = clientMessage.getMessageText();
			
			int amountNumber;
			try {
				amountNumber = Integer.parseInt(amountString);
			} catch (NumberFormatException e) {
				AppConfig.timestampedErrorPrint("Couldn't parse amount: " + amountString);
				return;
			}
			
			bitcakeManager.addSomeBitcakes(amountNumber);

			if (bitcakeManager instanceof ABBitcakeManager) {
				CausalBroadcast.addReceivedMessage(clientMessage);
			}

			AppConfig.timestampedStandardPrint("Transaction handler got: " + clientMessage);
		}
	}

}
