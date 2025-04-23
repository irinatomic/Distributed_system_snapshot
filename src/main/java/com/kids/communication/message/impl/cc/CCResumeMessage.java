package com.kids.communication.message.impl.cc;

import com.kids.communication.message.MessageType;
import com.kids.communication.message.impl.BasicMessage;
import com.kids.servent.ServentInfo;

import java.io.Serial;

/**
 * Send by the snapshot initiator to all nodes after it has received
 * all the ACK messages.
 */
public class CCResumeMessage extends BasicMessage {

    @Serial
    private static final long serialVersionUID = 1234567890123456789L;

    public CCResumeMessage(ServentInfo sender, ServentInfo receiver) {
        super(MessageType.CC_RESUME, sender, receiver, receiver, "RESUME", null);
    }
}
