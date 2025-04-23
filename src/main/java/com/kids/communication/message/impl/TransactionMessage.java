package com.kids.communication.message.impl;

import com.kids.servent.ServentInfo;
import com.kids.servent.bitcake.BitcakeManager;
import com.kids.communication.message.MessageType;
import com.kids.servent.config.AppConfig;

import java.io.Serial;
import java.util.Map;

/**
 * Represents a bitcake transaction. We are sending some bitcakes to another node.
 */
public class TransactionMessage extends BasicMessage {

	@Serial
	private static final long serialVersionUID = -333251402058492901L;

	private final transient BitcakeManager bitcakeManager;

	public TransactionMessage(ServentInfo sender, ServentInfo receiver, ServentInfo neighbor, int amount, BitcakeManager bitcakeManager, Map<Integer, Integer> senderVectorClock) {
		super(MessageType.TRANSACTION, sender, receiver, neighbor, String.valueOf(amount), senderVectorClock);
		this.bitcakeManager = bitcakeManager;
	}
	
	/**
	 * We want to take away our amount exactly as we are sending, so our snapshots don't mess up.
	 * This method is invoked by the sender just before sending, and with a lock that guarantees
	 * that we are white when we are doing this in Chandy-Lamport.
	 */
	@Override
	public void sendEffect() {
		int amount = Integer.parseInt(getMessageText());
		bitcakeManager.takeSomeBitcakes(amount);
	}
}
