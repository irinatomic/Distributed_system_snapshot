package com.kids.communication.message.impl.cc;

import com.kids.communication.message.MessageType;
import com.kids.communication.message.impl.BasicMessage;
import com.kids.servent.ServentInfo;

import java.io.Serial;

/**
 * This message is sent by a node to indicate that it has received
 * the snapshot request and contains the amount of bitcakes in the node.
 */
public class CCAckMessage extends BasicMessage {

    @Serial
    private static final long serialVersionUID = -4114118932792357339L;

    public CCAckMessage(ServentInfo sender, ServentInfo receiver, int amount) {
        super(MessageType.CC_ACK, sender, receiver, receiver, String.valueOf(amount), null);
    }

    public int getAmount() {
        return Integer.parseInt(getMessageText());
    }
}
