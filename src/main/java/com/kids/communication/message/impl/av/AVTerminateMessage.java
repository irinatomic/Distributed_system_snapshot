package com.kids.communication.message.impl.av;

import com.kids.communication.message.MessageType;
import com.kids.communication.message.impl.BasicMessage;
import com.kids.servent.ServentInfo;

import java.io.Serial;
import java.util.Map;

public class AVTerminateMessage extends BasicMessage {

    @Serial
    private static final long serialVersionUID = 1232938408790018273L;

    public AVTerminateMessage(ServentInfo sender,
                              ServentInfo receiver,
                              ServentInfo neighbor,
                              Map<Integer, Integer> senderVectorClock) {
        super(MessageType.AV_TERMINATE, sender, receiver, neighbor, senderVectorClock);
    }

}
