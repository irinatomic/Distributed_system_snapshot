package com.kids.communication.handler.impl.cc;

import com.kids.communication.handler.MessageHandler;
import com.kids.communication.message.Message;
import com.kids.communication.message.impl.cc.CCAckMessage;
import com.kids.communication.message.util.CausalBroadcast;
import com.kids.communication.message.util.MessageUtil;
import com.kids.servent.config.AppConfig;
import com.kids.servent.snapshot.data.CCSnapshot;
import com.kids.servent.snapshot.strategy.CCSnapshotStrategy;
import lombok.AllArgsConstructor;

/**
 * Handles ACK messages in the CC protocol.
 * ACK messages are sent by nodes to the initiator of the snapshot.
 * After the initiator node collect all ACK messages, it sends RESUME messages to all nodes.
 */
@AllArgsConstructor
public class CCAckHandler implements MessageHandler {

    private final Message clientMessage;

    @Override
    public void run() {
        CCAckMessage response = (CCAckMessage) clientMessage;
        int originalSenderId = clientMessage.getOriginalSenderInfo().id();

        CCSnapshotStrategy snapshotStrategy = (CCSnapshotStrategy) CausalBroadcast.getSnapshotStrategy();

        // If we are the snapshot initiator store the response
        // Else, forward the message to all our neighbours
        if (AppConfig.myServentInfo.id() == snapshotStrategy.getSnapshotInitiatorId()) {
            AppConfig.timestampedStandardPrint("[SNAPSHOT] Got an ACK message from node" + originalSenderId);
            CCSnapshot snapshot =  new CCSnapshot(
                    response.getNodeId(),
                    response.getAmount()
            );
            snapshotStrategy.addSnapshot(snapshot);
            return;
        }

        AppConfig.timestampedStandardPrint("[SNAPSHOT] Forwarding ACK to all neighbours except the original sender: " + originalSenderId);
        for (Integer neighbour : AppConfig.myServentInfo.neighbors()) {
            if (neighbour != response.getOriginalSenderInfo().id()) {
                CCAckMessage forwardMessage = new CCAckMessage(
                        AppConfig.myServentInfo,
                        AppConfig.getInfoById(neighbour),
                        response.getAmount(),
                        response.getNodeId()
                );
                MessageUtil.sendMessage(forwardMessage);
                AppConfig.timestampedStandardPrint("[SNAPSHOT] Forwarding ACK to node" + neighbour + " should reach initiator node" + originalSenderId);
            }
        }
    }
}
