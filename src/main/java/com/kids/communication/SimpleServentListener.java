package com.kids.communication;

import com.kids.servent.config.AppConfig;
import com.kids.servent.Cancellable;
import com.kids.communication.handler.MessageHandler;
import com.kids.communication.handler.impl.CausalBroadcastHandler;
import com.kids.communication.message.Message;
import com.kids.communication.message.util.MessageUtil;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Listens for incoming messages on a specified port and processes them using a thread pool.
 * It handles messages in a concurrent environment and integrates with a snapshot collector for distributed system snapshots.
 */
public class SimpleServentListener implements Runnable, Cancellable {

	/*
	 * Thread pool for executing the handlers. Each client will get its own handler thread.
	 */
	private final ExecutorService threadPool = Executors.newWorkStealingPool();

	private volatile boolean working = true;
	private final Set<Message> receivedBroadcasts = Collections.newSetFromMap(new ConcurrentHashMap<>());
	private final Object lock = new Object();
	
	@Override
	public void run() {
		ServerSocket listenerSocket = null;
		try {
			listenerSocket = new ServerSocket(AppConfig.myServentInfo.listenerPort(), 100);
			// If there is no connection after 1s, wake up and see if we should terminate
			listenerSocket.setSoTimeout(1000);
		} catch (IOException e) {
			AppConfig.timestampedErrorPrint("Couldn't open listener socket on: " + AppConfig.myServentInfo.listenerPort());
			System.exit(0);
		}
		
		while (working) {
			try {
				Message clientMessage;
				// This blocks for up to 1s, after which SocketTimeoutException is thrown
				Socket clientSocket = listenerSocket.accept();
				clientMessage = MessageUtil.readMessage(clientSocket);

				MessageHandler messageHandler = new CausalBroadcastHandler(
						clientMessage,
						receivedBroadcasts,
						lock
				);

				threadPool.submit(messageHandler);
			} catch (SocketTimeoutException timeoutEx) {
				// Uncomment the next line to see that we are waking up every second
				// AppConfig.timedStandardPrint("Waiting...");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void stop() {
		this.working = false;
	}

}
