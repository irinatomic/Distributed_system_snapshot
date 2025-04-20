package com.kids.communication.message.impl.av;

import com.kids.communication.message.MessageType;
import com.kids.communication.message.impl.BasicMessage;
import com.kids.servent.ServentInfo;

import java.io.Serial;
import java.util.Map;

public class AVSnapshotRequestMessage extends BasicMessage {

    @Serial
    private static final long serialVersionUID = 1887472498490018273L;

    public AVSnapshotRequestMessage(ServentInfo sender,
                                    ServentInfo receiver,
                                    ServentInfo neighbor,
                                    Map<Integer, Integer> senderVectorClock) {
        super(MessageType.AV_SNAPSHOT_REQUEST, sender, receiver, neighbor, senderVectorClock);
    }

}
