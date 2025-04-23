package com.kids.communication.handler.impl.cc;

import com.kids.communication.handler.MessageHandler;
import com.kids.communication.message.Message;
import com.kids.communication.message.impl.cc.CCAckMessage;
import com.kids.communication.message.util.CausalBroadcast;
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

        AppConfig.timestampedStandardPrint("[SNAPSHOT] Got an ACK message from node" + originalSenderId);

        // Create snapshot object from the message
        CCSnapshot snapshot =  new CCSnapshot(
                originalSenderId,
                response.getAmount()
        );

        CCSnapshotStrategy snapshotStrategy = (CCSnapshotStrategy) CausalBroadcast.getSnapshotStrategy();
        snapshotStrategy.addSnapshot(snapshot);
    }
}
