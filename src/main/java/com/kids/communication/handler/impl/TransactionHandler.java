package com.kids.communication.handler.impl;

import com.kids.communication.message.util.MessageUtil;
import com.kids.servent.bitcake.AVBitcakeManager;
import com.kids.servent.bitcake.BitcakeManagerInstance;
import com.kids.servent.config.AppConfig;
import com.kids.communication.message.util.CausalBroadcast;
import com.kids.servent.bitcake.BitcakeManager;
import com.kids.servent.bitcake.ABBitcakeManager;
import com.kids.communication.handler.MessageHandler;
import com.kids.communication.message.Message;
import com.kids.communication.message.MessageType;
import com.kids.servent.snapshot.strategy.AVSnapshotStrategy;
import lombok.RequiredArgsConstructor;

import java.util.Map;

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
			int amountNumber = MessageUtil.getAmountFromMessage(clientMessage);
			if (amountNumber == -1) return;

			if (bitcakeManager instanceof ABBitcakeManager) {
				handleTransactionAB(amountNumber);
			} else if (bitcakeManager instanceof AVBitcakeManager) {
				handleTransactionAV(amountNumber);
			}

			AppConfig.timestampedStandardPrint("Transaction handler got: " + clientMessage);
		}
	}

	private void handleTransactionAB(int amount) {
		bitcakeManager.addSomeBitcakes(amount);
		CausalBroadcast.addReceivedMessage(clientMessage);
	}

	private void handleTransactionAV(int amount) {
		AVSnapshotStrategy snapshotStrategy = (AVSnapshotStrategy) CausalBroadcast.getSnapshotStrategy();
		AVBitcakeManager avBitcakeManager = (AVBitcakeManager) bitcakeManager;

		if (!snapshotStrategy.isSnapshotInProgress()) {
			bitcakeManager.addSomeBitcakes(amount);
			return;
		}

		Map<Integer, Integer> senderClock = clientMessage.getSenderVectorClock();
		Map<Integer, Integer> initiatorClock = snapshotStrategy.getInitiatorVectorClock();

		if (CausalBroadcast.otherClockGreater(senderClock, initiatorClock)) {
			bitcakeManager.addSomeBitcakes(amount);
			avBitcakeManager.addMessageReceivedDuringSnapshot(clientMessage);
		} else {
			CausalBroadcast.addPendingMessage(clientMessage);
		}
	}

}
