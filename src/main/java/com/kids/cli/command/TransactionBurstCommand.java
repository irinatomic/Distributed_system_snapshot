package com.kids.cli.command;

import com.kids.servent.bitcake.BitcakeManager;
import com.kids.servent.bitcake.BitcakeManagerInstance;
import com.kids.servent.config.AppConfig;
import com.kids.communication.message.util.CausalBroadcast;
import com.kids.servent.ServentInfo;
import com.kids.servent.bitcake.ABBitcakeManager;
import com.kids.communication.message.Message;
import com.kids.communication.message.impl.TransactionMessage;
import com.kids.communication.message.util.MessageUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Executes a burst of random transactions by creating multiple worker threads.
 * Each worker picks a different receiving node and sends a transaction message with a randomly selected amount.
 */
public class TransactionBurstCommand implements CLICommand {

	private static final int TRANSACTION_COUNT = 5;
	private static final int BURST_WORKERS = 5;
	private static final int MAX_TRANSFER_AMOUNT = 5;

	private final BitcakeManager bitcakeManager;
	private final Object lock = new Object();

	public TransactionBurstCommand() {
		this.bitcakeManager = BitcakeManagerInstance.getInstance();
	}

	private class TransactionBurstWorker implements Runnable {

		@Override
		public void run() {
			for (int i = 0; i < TRANSACTION_COUNT; i++) {
				ServentInfo receiverInfo = AppConfig.getInfoById((int) (Math.random() * AppConfig.getServentCount()));

				// Choose a random receiver that is not us
				while (receiverInfo.id() == AppConfig.myServentInfo.id()) {
					receiverInfo = AppConfig.getInfoById((int) (Math.random() * AppConfig.getServentCount()));
				}

				int amount = 1 + (int) (Math.random() * MAX_TRANSFER_AMOUNT);

				Message transaction;
				synchronized (lock) {
					Map<Integer, Integer> vectorClock = new ConcurrentHashMap<>(CausalBroadcast.getVectorClock());

					transaction = new TransactionMessage(
							AppConfig.myServentInfo,
							receiverInfo,
							null,
							amount,
							bitcakeManager,
							vectorClock
					);

					if (bitcakeManager instanceof ABBitcakeManager) {
						CausalBroadcast.addSentMessage(transaction);
					}

					// Deduct the amount and send the message
					transaction.sendEffect();
					CausalBroadcast.causalClockIncrement(transaction);
				}

				AppConfig.myServentInfo.neighbors()
						.forEach(neighbor -> MessageUtil.sendMessage(transaction.changeReceiver(neighbor).makeMeASender()));
			}
		}
	}
	
	@Override
	public String commandName() {
		return "transaction_burst";
	}

	@Override
	public void execute(String args) {
		for (int i = 0; i < BURST_WORKERS; i++) {
			Thread t = new Thread(new TransactionBurstWorker());
			t.start();
		}
	}
}
