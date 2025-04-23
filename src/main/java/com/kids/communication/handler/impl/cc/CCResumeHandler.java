package com.kids.communication.handler.impl.cc;

import com.kids.communication.handler.MessageHandler;
import com.kids.communication.message.Message;
import com.kids.communication.message.util.CausalBroadcast;
import com.kids.servent.snapshot.strategy.CCSnapshotStrategy;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class CCResumeHandler implements MessageHandler {

    private final Message clientMessage;

    @Override
    public void run() {
        CCSnapshotStrategy snapshotStrategy = (CCSnapshotStrategy) CausalBroadcast.getSnapshotStrategy();
        snapshotStrategy.endSnapshotMode();
    }
}
