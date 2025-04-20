package com.kids.communication.message.impl.ab;

import com.kids.communication.message.impl.BasicMessage;
import com.kids.servent.config.AppConfig;
import com.kids.servent.ServentInfo;
import com.kids.communication.message.Message;
import com.kids.communication.message.MessageType;
import lombok.Getter;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Getter
public class ABSnapshotResponseMessage extends BasicMessage {

    @Serial
    private static final long serialVersionUID = 1932837451964281053L;
    private final List<Message> sent;
    private final List<Message> received;

    public ABSnapshotResponseMessage(ServentInfo sender, ServentInfo receiver, ServentInfo neighbor, Map<Integer, Integer> senderVectorClock,
                                     int amount, List<Message> sentTransactions, List<Message> receivedTransactions) {
        super(MessageType.AB_SNAPSHOT_RESPONSE, sender, receiver, neighbor, String.valueOf(amount), senderVectorClock);

        this.sent = new ArrayList<>(sentTransactions);
        this.received = new ArrayList<>(receivedTransactions);
    }

    protected ABSnapshotResponseMessage(ServentInfo originalSenderInfo, ServentInfo originalReceiverInfo, ServentInfo receiverInfo, Map<Integer, Integer> senderVectorClock,
                                        List<ServentInfo> routeList, String messageText, int messageId, List<Message> sentTransactions, List<Message> receivedTransactions) {
        super(MessageType.AB_SNAPSHOT_RESPONSE, originalSenderInfo, originalReceiverInfo, receiverInfo, senderVectorClock, routeList, messageText, messageId);

        this.sent = new CopyOnWriteArrayList<>(sentTransactions);
        this.received = new CopyOnWriteArrayList<>(receivedTransactions);
    }

    /**
     * Creates a new ABSnapshotResponseMessage with updated route information.
     */
    @Override
    public Message makeMeASender() {
        ServentInfo newRouteItem = AppConfig.myServentInfo;
        List<ServentInfo> newRouteList = new ArrayList<>(getRoute());
        newRouteList.add(newRouteItem);

        return new ABSnapshotResponseMessage(
                getOriginalSenderInfo(),
                getOriginalReceiverInfo(),
                getReceiverInfo(),
                getSenderVectorClock(),
                newRouteList,
                getMessageText(),
                getMessageId(),
                getSent(),
                getReceived()
        );
    }

    /**
     * Creates a new ABSnapshotResponseMessage with a changed receiver.
     * If the receiver is not a neighbor, returns null.
     */
    @Override
    public Message changeReceiver(Integer newReceiverId) {
        if (AppConfig.myServentInfo.neighbors().contains(newReceiverId)) {
            ServentInfo newReceiverInfo = AppConfig.getInfoById(newReceiverId);

            return new ABSnapshotResponseMessage(
                    getOriginalSenderInfo(),
                    getOriginalReceiverInfo(),
                    newReceiverInfo,
                    getSenderVectorClock(),
                    getRoute(),
                    getMessageText(),
                    getMessageId(),
                    getSent(),
                    getReceived()
            );
        } else {
            AppConfig.timestampedErrorPrint("Trying to make a message for " + newReceiverId + " who is not a neighbor.");
            return null;
        }
    }
}
