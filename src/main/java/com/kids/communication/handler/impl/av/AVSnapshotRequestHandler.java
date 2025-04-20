package com.kids.communication.handler.impl.av;

import com.kids.communication.handler.MessageHandler;
import com.kids.communication.message.Message;
import com.kids.communication.message.impl.av.AVDoneMessage;
import com.kids.communication.message.util.CausalBroadcast;
import com.kids.communication.message.util.MessageUtil;
import com.kids.servent.bitcake.AVBitcakeManager;
import com.kids.servent.bitcake.BitcakeManagerInstance;
import com.kids.servent.config.AppConfig;
import com.kids.servent.snapshot.strategy.AVSnapshotStrategy;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class AVSnapshotRequestHandler implements MessageHandler {

    private final Message clientMessage;

    @Override
    public void run() {
        // Note values for the start of the snapshot
        AVSnapshotStrategy snapshotStrategy = (AVSnapshotStrategy) CausalBroadcast.getSnapshotStrategy();
        snapshotStrategy.setInitiatorVectorClock(clientMessage.getSenderVectorClock());

        snapshotStrategy.setInitiatorNodeId(clientMessage.getOriginalSenderInfo().id());

        AVBitcakeManager bitcakeManager = (AVBitcakeManager) BitcakeManagerInstance.getInstance();
        bitcakeManager.startSnapshotMode();

        AppConfig.timestampedStandardPrint("[SNAPSHOT] Received request for snapshot initiated by node " + clientMessage.getOriginalSenderInfo().id());

        // Send DONE message to all neighbors -> it is supposed to reach the initiator
        Message doneMessage = new AVDoneMessage(
                AppConfig.myServentInfo,
                clientMessage.getOriginalSenderInfo(),
                null,
                clientMessage.getSenderVectorClock()
        );

        // Send DONE message to all neighbors
        for (Integer neighbor : AppConfig.myServentInfo.neighbors()) {
            doneMessage = doneMessage.changeReceiver(neighbor);
            MessageUtil.sendMessage(doneMessage);
        }

        MessageUtil.sendMessage(doneMessage);
        CausalBroadcast.causalClockIncrement(doneMessage);
    }
}
