package com.kids.communication.handler.impl.cc;

import com.kids.communication.handler.MessageHandler;
import com.kids.communication.message.Message;
import com.kids.communication.message.impl.cc.CCAckMessage;
import com.kids.communication.message.impl.cc.CCSnapshotRequestMessage;
import com.kids.communication.message.util.CausalBroadcast;
import com.kids.communication.message.util.MessageUtil;
import com.kids.servent.bitcake.BitcakeManagerInstance;
import com.kids.servent.config.AppConfig;
import com.kids.servent.snapshot.strategy.CCSnapshotStrategy;
import lombok.AllArgsConstructor;

/**
 * Handles the snapshot request messages in the CC protocol.
 * Forwards the message to all neighbours and send an ACK message to the initiator.
 */
@AllArgsConstructor
public class CCSnapshotRequestHandler implements MessageHandler {

    private final Message clientMessage;

    @Override
    public void run() {
        CCSnapshotStrategy snapshotStrategy = (CCSnapshotStrategy) CausalBroadcast.getSnapshotStrategy();

        CCSnapshotRequestMessage message = (CCSnapshotRequestMessage) clientMessage;
        int initiatorId = message.getInitiatorId();

        if (snapshotStrategy.inSnapshotMode()) {
            AppConfig.timestampedStandardPrint("[SNAPSHOT] Already in snapshot mode. Ignoring request from: " + clientMessage.getOriginalSenderInfo().id());
            return;
        }

        // Start snapshot mode
        snapshotStrategy.startSnapshotModeNonInitiator(initiatorId);
        AppConfig.timestampedStandardPrint("[SNAPSHOT] Received snapshot request from: " + initiatorId);

        // Forward to neighbours
        for(Integer neighbour : AppConfig.myServentInfo.neighbors()) {
            if (neighbour != message.getOriginalSenderInfo().id()) {
                CCSnapshotRequestMessage forwardMessage = new CCSnapshotRequestMessage(
                        AppConfig.myServentInfo,
                        AppConfig.getInfoById(neighbour),
                        initiatorId
                );
                MessageUtil.sendMessage(forwardMessage);
            }
        }

        // Send ACK
        int amount = BitcakeManagerInstance.getInstance().getCurrentBitcakeAmount();
        for (Integer neighbor : AppConfig.myServentInfo.neighbors()) {
            Message ackMessage = new CCAckMessage(
                    AppConfig.myServentInfo,
                    AppConfig.getInfoById(neighbor),
                    amount,
                    AppConfig.myServentInfo.id()
            );
            MessageUtil.sendMessage(ackMessage);
            AppConfig.timestampedStandardPrint("[SNAPSHOT] Sending ACK to node" + neighbor + " should reach initiator node" + initiatorId);
        }
    }
}
