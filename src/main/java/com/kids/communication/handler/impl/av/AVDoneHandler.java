package com.kids.communication.handler.impl.av;

import com.kids.communication.handler.MessageHandler;
import com.kids.communication.message.Message;
import com.kids.communication.message.util.CausalBroadcast;
import com.kids.servent.config.AppConfig;
import com.kids.servent.snapshot.strategy.AVSnapshotStrategy;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class AVDoneHandler implements MessageHandler {

    private final Message clientMessage;

    @Override
    public void run() {
        AVSnapshotStrategy snapshotStrategy = (AVSnapshotStrategy) CausalBroadcast.getSnapshotStrategy();
        snapshotStrategy.getDoneMessagesCounter().incrementAndGet();

        AppConfig.timestampedStandardPrint("[SNAPSHOT] Got a DONE message from node" + clientMessage.getOriginalSenderInfo().id());
    }
}
