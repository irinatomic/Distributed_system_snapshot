package com.kids.communication.message.util;

import com.kids.communication.message.MessageType;
import com.kids.servent.config.AppConfig;
import com.kids.communication.message.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class MessageUtil {

	// for debugging purposes
	public static final boolean MESSAGE_UTIL_PRINTING = true;
	
	public static Map<Integer, BlockingQueue<Message>> pendingMessages = new ConcurrentHashMap<>();
	public static Map<Integer, BlockingQueue<Message>> pendingMarkers = new ConcurrentHashMap<>();
	
	public static void initializePendingMessages() {
		for (Integer neighbor : AppConfig.myServentInfo.neighbors()) {
			pendingMarkers.put(neighbor, new LinkedBlockingQueue<>());
			pendingMessages.put(neighbor, new LinkedBlockingQueue<>());
		}
	}
	
	public static Message readMessage(Socket socket) {
		Message clientMessage = null;
			
		try {
			ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
			clientMessage = (Message) ois.readObject();
			
			if (AppConfig.IS_FIFO) {
				String response = "ACK";
				ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
				oos.writeObject(response);
				oos.flush();
			}
			
			socket.close();
		} catch (IOException e) {
			AppConfig.timestampedErrorPrint("Error in reading socket on " +
					socket.getInetAddress() + ":" + socket.getPort());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		
		if (MESSAGE_UTIL_PRINTING) {
			AppConfig.timestampedStandardPrint("Got message " + clientMessage);
		}
		return clientMessage;
	}
	
	public static void sendMessage(Message message) {
		if (AppConfig.IS_FIFO) {
			sendMessageFifo(message);
		} else {
			sendMessageNotFifo(message);
		}
	}

	private static void sendMessageNotFifo(Message message) {
		Thread delayedSender = new Thread(new DelayedMessageSender(message));
		delayedSender.start();
	}

	private static void sendMessageFifo(Message message) {
		int receiverId = message.getOriginalReceiverInfo().id();
		try {
			if (isCCSnapshotMessage(message.getMessageType())) {
				pendingMarkers.get(receiverId).put(message);
			} else {
				pendingMessages.get(receiverId).put(message);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private static boolean isCCSnapshotMessage(MessageType type) {
		return type == MessageType.CC_SNAPSHOT_REQUEST
				|| type == MessageType.CC_ACK
				|| type == MessageType.CC_RESUME;
	}

	public static int getAmountFromMessage(Message message) {
		String amountString = message.getMessageText();

		int amountNumber;
		try {
			amountNumber = Integer.parseInt(amountString);
		} catch (NumberFormatException e) {
			AppConfig.timestampedErrorPrint("Couldn't parse amount: " + amountString);
			return -1;
		}

		return amountNumber;
	}
}
