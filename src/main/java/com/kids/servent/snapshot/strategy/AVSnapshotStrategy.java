package com.kids.servent.snapshot.strategy;

import com.kids.communication.message.Message;
import com.kids.communication.message.impl.av.AVSnapshotRequestMessage;
import com.kids.communication.message.impl.av.AVTerminateMessage;
import com.kids.communication.message.util.CausalBroadcast;
import com.kids.communication.message.util.MessageUtil;
import com.kids.servent.bitcake.AVBitcakeManager;
import com.kids.servent.config.AppConfig;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class AVSnapshotStrategy implements SnapshotStrategy {

    @Getter @Setter
    private Map<Integer, Integer> initiatorVectorClock;
    @Getter @Setter
    private AtomicInteger doneMessagesCounter;
    private final AVBitcakeManager bitcakeManager;

    public AVSnapshotStrategy(AVBitcakeManager bitcakeManager) {
        this.doneMessagesCounter = new AtomicInteger(0);
        this.bitcakeManager = bitcakeManager;
    }

    @Override
    public void initiateSnapshot() {
        // Create SNAPSHOT_REQUEST message
        initiatorVectorClock = new ConcurrentHashMap<>(CausalBroadcast.getVectorClock());
        Message request = new AVSnapshotRequestMessage(AppConfig.myServentInfo, null, null, initiatorVectorClock);

        // Send SNAPSHOT_REQUEST message to all neighbors
        for (Integer neighbor : AppConfig.myServentInfo.neighbors()) {
            Message neighborRequest = request.changeReceiver(neighbor);
            MessageUtil.sendMessage(neighborRequest);
        }

        bitcakeManager.startSnapshotMode();
        initiatorVectorClock = new ConcurrentHashMap<>(CausalBroadcast.getVectorClock());
    }

    @Override
    public boolean isSnapshotComplete() {
        // -1 because we are not counting the initiator
        return doneMessagesCounter.get() == AppConfig.getServentCount() - 1;
    }

    /**
     * This method is called from the initiator node when it receives a DONE message from all other nodes.
     */
    @Override
    public void processSnapshotEnding() {
        AppConfig.timestampedStandardPrint("[SNAPSHOT] Received DONE from all nodes. Ending snapshot.");

        // Broadcast TERMINATE message
        Map<Integer, Integer> vectorClock = new ConcurrentHashMap<>(CausalBroadcast.getVectorClock());

        Message terminateMessage = new AVTerminateMessage(
                AppConfig.myServentInfo,
                null,
                null,
                vectorClock
        );

        AppConfig.myServentInfo.neighbors().stream()
                .map(terminateMessage::changeReceiver)
                .forEach(MessageUtil::sendMessage);

        CausalBroadcast.addPendingMessage(terminateMessage);
        CausalBroadcast.checkPendingMessages();

        // AVBitcakManager -> print my state
        bitcakeManager.endSnapshotMode();

        clearSnapshotData();
    }

    private void clearSnapshotData() {
        initiatorVectorClock = null;
        doneMessagesCounter.set(0);
    }

    public boolean isSnapshotInProgress() {
        return initiatorVectorClock != null;
    }
}
