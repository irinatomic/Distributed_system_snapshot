package com.kids.communication.handler.impl.av;

import com.kids.communication.handler.MessageHandler;
import com.kids.communication.message.Message;
import com.kids.communication.message.util.CausalBroadcast;
import com.kids.servent.bitcake.AVBitcakeManager;
import com.kids.servent.bitcake.BitcakeManagerInstance;
import com.kids.servent.config.AppConfig;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class AVTerminateHandler implements MessageHandler {

    private final Message clientMessage;

    @Override
    public void run() {
        AVBitcakeManager bitcakeManager = (AVBitcakeManager) BitcakeManagerInstance.getInstance();
        if (clientMessage.getOriginalSenderInfo().id() != AppConfig.myServentInfo.id()) {
            CausalBroadcast.checkPendingMessages();
            bitcakeManager.endSnapshotMode();
        }

        AppConfig.timestampedStandardPrint("[SNAPSHOT] Got a TERMINATE message from node" + clientMessage.getOriginalSenderInfo().id());
    }
}
