package com.kids.communication.message.util;

import com.kids.communication.message.Message;
import com.kids.communication.message.MessageType;
import com.kids.servent.Cancellable;
import com.kids.servent.ServentInfo;
import com.kids.servent.config.AppConfig;
import com.kids.servent.snapshot.strategy.CCSnapshotStrategy;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

/**
 * This worker implements the sending of messages for FIFO causal broadcast.
 * If a message is found in the marker queue, it is sent right away. If not,
 * we check the regular messages queue.
 */
@RequiredArgsConstructor
public class FifoMessageSender implements Runnable, Cancellable {

    private final int neighbour;
    private volatile boolean working = true;

    @Override
    public void run() {
        CCSnapshotStrategy snapshotStrategy = (CCSnapshotStrategy) CausalBroadcast.getSnapshotStrategy();
        Message messageToSend;

        while (working) {
            try {
                messageToSend = MessageUtil.pendingMarkers.get(neighbour).poll(1000, TimeUnit.MILLISECONDS);

                if (!snapshotStrategy.inSnapshotMode()) {
                    if (messageToSend == null) {
                        messageToSend = MessageUtil.pendingMessages.get(neighbour).poll(1000, TimeUnit.MILLISECONDS);
                    }
                }

                if (messageToSend == null) continue;
                if (messageToSend.getMessageType() == MessageType.POISON) break;

                if (MessageUtil.MESSAGE_UTIL_PRINTING) {
                    AppConfig.timestampedStandardPrint("Sending message " + messageToSend);
                }

                ServentInfo receiverInfo = messageToSend.getReceiverInfo();

                try {
                    Socket sendSocket = new Socket(receiverInfo.ipAddress(), receiverInfo.listenerPort());
                    ObjectOutputStream oos = new ObjectOutputStream(sendSocket.getOutputStream());

                    oos.writeObject(messageToSend);
                    oos.flush();
                    messageToSend.sendEffect();

                    ObjectInputStream ois = new ObjectInputStream(sendSocket.getInputStream());
                    Object ack = ois.readObject();
                    if (!"ACK".equals(ack)) {
                        AppConfig.timestampedErrorPrint("Did not receive ACK from " + receiverInfo);
                    }

                    sendSocket.close();
                } catch (IOException e) {
                    AppConfig.timestampedErrorPrint("Failed to send message to " + receiverInfo + ": " + e.getMessage());
                }

            } catch (Exception e) {
                AppConfig.timestampedErrorPrint("Unexpected error in FifoMessageSender: " + e.getMessage());
            }
        }
    }

    @Override
    public void stop() {
        working = false;
    }
}
