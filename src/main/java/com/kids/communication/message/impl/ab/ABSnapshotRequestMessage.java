package com.kids.communication.message.impl.ab;

import com.kids.communication.message.impl.BasicMessage;
import com.kids.servent.ServentInfo;
import com.kids.communication.message.MessageType;

import java.io.Serial;
import java.util.Map;

public class ABSnapshotRequestMessage extends BasicMessage {

    @Serial
    private static final long serialVersionUID = 1887472498490324672L;

    public ABSnapshotRequestMessage(ServentInfo sender,
                                    ServentInfo receiver,
                                    ServentInfo neighbor,
                                    Map<Integer, Integer> senderVectorClock) {
        super(MessageType.AB_SNAPSHOT_REQUEST, sender, receiver, neighbor, senderVectorClock);
    }
}
