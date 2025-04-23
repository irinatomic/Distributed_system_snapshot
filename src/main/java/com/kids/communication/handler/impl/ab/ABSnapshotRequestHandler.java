package com.kids.communication.handler.impl.ab;

import com.kids.servent.bitcake.BitcakeManagerInstance;
import com.kids.servent.config.AppConfig;
import com.kids.communication.message.util.CausalBroadcast;
import com.kids.communication.handler.MessageHandler;
import com.kids.communication.message.Message;
import com.kids.communication.message.MessageType;
import com.kids.communication.message.impl.ab.ABSnapshotResponseMessage;
import com.kids.communication.message.util.MessageUtil;
import lombok.AllArgsConstructor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@AllArgsConstructor
public class ABSnapshotRequestHandler implements MessageHandler {

    private final Message clientMessage;

    @Override
    public void run() {
        if (clientMessage.getMessageType() == MessageType.AB_SNAPSHOT_REQUEST) {
            Message message = getMessage();
            CausalBroadcast.causalClockIncrement(message);

            AppConfig.myServentInfo.neighbors()
                    .forEach(neighbor -> MessageUtil.sendMessage(message.changeReceiver(neighbor).makeMeASender()));

        } else {
            AppConfig.timestampedErrorPrint("SNAPSHOT REQUEST HANDLER: Amount handler got: " + clientMessage);
        }
    }

    private Message getMessage() {
        int currentAmount = BitcakeManagerInstance.getInstance().getCurrentBitcakeAmount();
        Map<Integer, Integer> vectorClock = new ConcurrentHashMap<>(CausalBroadcast.getVectorClock());

        return new ABSnapshotResponseMessage(
                AppConfig.myServentInfo,
                clientMessage.getOriginalSenderInfo(),
                null,
                vectorClock,
                currentAmount,
                CausalBroadcast.getSent(),
                CausalBroadcast.getReceived()
        );
    }
}
