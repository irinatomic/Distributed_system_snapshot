package com.kids.communication.message.impl.cc;

import com.kids.communication.message.MessageType;
import com.kids.communication.message.impl.BasicMessage;
import com.kids.servent.ServentInfo;
import lombok.Getter;

import java.io.Serial;

/**
 * Message to request a snapshot in the Coordinated Checkpoint algorithm.
 */
public class CCSnapshotRequestMessage extends BasicMessage {

    @Serial
    private static final long serialVersionUID = 1279393238480293839L;
    @Getter
    private final int initiatorId;

    public CCSnapshotRequestMessage(ServentInfo sender, ServentInfo receiver, int initiatorId) {
        super(MessageType.CC_SNAPSHOT_REQUEST, sender, receiver, receiver, String.valueOf(initiatorId), null);
        this.initiatorId = initiatorId;
    }
}
