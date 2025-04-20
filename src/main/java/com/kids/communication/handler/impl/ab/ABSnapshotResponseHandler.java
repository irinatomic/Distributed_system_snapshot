package com.kids.communication.handler.impl.ab;

import com.kids.servent.config.AppConfig;
import com.kids.servent.snapshot.collector.SnapshotCollector;
import com.kids.servent.snapshot.ABSnapshot;
import com.kids.communication.handler.MessageHandler;
import com.kids.communication.message.Message;
import com.kids.communication.message.MessageType;
import com.kids.communication.message.impl.ab.ABSnapshotResponseMessage;
import com.kids.servent.snapshot.strategy.ABSnapshotStrategy;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ABSnapshotResponseHandler implements MessageHandler {

    private final Message clientMessage;
    private final SnapshotCollector snapshotCollector;

    @Override
    public void run() {
        try {
            if (clientMessage.getMessageType() == MessageType.AB_SNAPSHOT_RESPONSE) {
                int neighborAmount = Integer.parseInt(clientMessage.getMessageText());
                ABSnapshotResponseMessage response = (ABSnapshotResponseMessage) clientMessage;

                ABSnapshot snapshotResult = new ABSnapshot(
                        clientMessage.getOriginalSenderInfo().id(),
                        neighborAmount,
                        response.getSent(),
                        response.getReceived()
                );

                ABSnapshotStrategy snapshotStrategy = (ABSnapshotStrategy) snapshotCollector.getSnapshotStrategy();
                snapshotStrategy
                        .getCollectedData()
                        .put(clientMessage.getOriginalSenderInfo().id(), snapshotResult);
            } else {
                AppConfig.timestampedErrorPrint("SNAPSHOT RESPONSE HANDLER: Amount handler got: " + clientMessage);
            }
        } catch (Exception e) {
            AppConfig.timestampedErrorPrint(e.getMessage());
        }
    }

}
