package com.kids.communication.handler.impl;

import com.kids.communication.message.util.MessageUtil;
import com.kids.servent.bitcake.*;
import com.kids.servent.config.AppConfig;
import com.kids.communication.message.util.CausalBroadcast;
import com.kids.communication.handler.MessageHandler;
import com.kids.communication.message.Message;
import com.kids.communication.message.MessageType;
import com.kids.servent.snapshot.strategy.AVSnapshotStrategy;
import com.kids.servent.snapshot.strategy.CCSnapshotStrategy;
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

			if (bitcakeManager instanceof CCBitcakeManager) {
				handleTransactionCC(amountNumber);
			} else if (bitcakeManager instanceof ABBitcakeManager) {
				handleTransactionAB(amountNumber);
			} else if (bitcakeManager instanceof AVBitcakeManager) {
				handleTransactionAV(amountNumber);
			}
		}
	}

	private void handleTransactionCC(int amount) {
		CCSnapshotStrategy snapshotStrategy = (CCSnapshotStrategy) CausalBroadcast.getSnapshotStrategy();
		if (!snapshotStrategy.inSnapshotMode()) {
			bitcakeManager.addSomeBitcakes(amount);
			return;
		}

		try {
			int receiverId = clientMessage.getReceiverInfo().id();
			MessageUtil.pendingMessages.get(receiverId).put(clientMessage);
		} catch (InterruptedException e) {
			AppConfig.timestampedErrorPrint("Transaction handler interrupted while waiting for pending transactions");
			Thread.currentThread().interrupt();
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
