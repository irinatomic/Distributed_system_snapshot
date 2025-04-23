package com.kids.communication.handler.impl.cc;

import com.kids.communication.handler.MessageHandler;
import com.kids.communication.message.Message;
import com.kids.communication.message.impl.cc.CCAckMessage;
import com.kids.communication.message.impl.cc.CCSnapshotRequestMessage;
import com.kids.communication.message.util.CausalBroadcast;
import com.kids.communication.message.util.MessageUtil;
import com.kids.servent.bitcake.BitcakeManagerInstance;
import com.kids.servent.bitcake.CCBitcakeManager;
import com.kids.servent.config.AppConfig;
import com.kids.servent.snapshot.strategy.AVSnapshotStrategy;
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
        CCSnapshotRequestMessage message = (CCSnapshotRequestMessage) clientMessage;
        int initiatorId = message.getInitiatorId();

        AppConfig.timestampedStandardPrint("[SNAPSHOT] Received snapshot request from: " + initiatorId);

        // Start snapshot mode
        CCSnapshotStrategy snapshotStrategy = (CCSnapshotStrategy) CausalBroadcast.getSnapshotStrategy();
        snapshotStrategy.startSnapshotMode();

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

        // Send ACK to initiator
        AppConfig.timestampedStandardPrint("[SNAPSHOT] Sending ACK to initiator: " + initiatorId);

        CCBitcakeManager bitcakeManager = (CCBitcakeManager) BitcakeManagerInstance.getInstance();
        CCAckMessage response = new CCAckMessage(
                AppConfig.myServentInfo,
                AppConfig.getInfoById(initiatorId),
                bitcakeManager.getCurrentBitcakeAmount()
        );
        MessageUtil.sendMessage(response);
    }
}
