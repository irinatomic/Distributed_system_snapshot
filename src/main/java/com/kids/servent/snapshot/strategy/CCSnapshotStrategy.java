package com.kids.servent.snapshot.strategy;

import com.kids.communication.message.Message;
import com.kids.communication.message.impl.cc.CCResumeMessage;
import com.kids.communication.message.impl.cc.CCSnapshotRequestMessage;
import com.kids.communication.message.util.MessageUtil;
import com.kids.servent.bitcake.CCBitcakeManager;
import com.kids.servent.config.AppConfig;
import com.kids.servent.snapshot.data.CCSnapshot;
import lombok.Getter;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class CCSnapshotStrategy implements SnapshotStrategy {

    private final AtomicBoolean snapshotMode = new AtomicBoolean(false);
    private final Map<Integer, CCSnapshot> collectedData = new ConcurrentHashMap<>();
    private final CCBitcakeManager bitcakeManager;
    @Getter
    private Integer snapshotInitiatorId = null;

    public CCSnapshotStrategy(CCBitcakeManager bitcakeManager) {
        this.bitcakeManager = bitcakeManager;
    }

    @Override
    public void initiateSnapshot() {
        if (inSnapshotMode()) {
            AppConfig.timestampedStandardPrint("[SNAPSHOT] Already in snapshot mode");
        }
        startSnapshotModeInitiator();

        // Send SNAPSHOT_REQUEST message to all neighbours
        for (Integer neighbor : AppConfig.myServentInfo.neighbors()) {
            CCSnapshotRequestMessage neighborRequest = new CCSnapshotRequestMessage(
                    AppConfig.myServentInfo,
                    AppConfig.getInfoById(neighbor),
                    AppConfig.myServentInfo.id()
            );

            MessageUtil.sendMessage(neighborRequest);
        }

        // Save the current state
        CCSnapshot snapshotResult = new CCSnapshot(
                AppConfig.myServentInfo.id(),
                bitcakeManager.getCurrentBitcakeAmount()
        );
        collectedData.put(AppConfig.myServentInfo.id(), snapshotResult);
    }

    @Override
    public boolean isSnapshotComplete() {
        return collectedData.size() == AppConfig.getServentCount();
    }

    @Override
    public void processSnapshotEnding() {
        AppConfig.timestampedStandardPrint("[SNAPSHOT] Received ACK from all nodes. Ending snapshot.");

        // Broadcast RESUME messages
        for (Integer neighbour : AppConfig.myServentInfo.neighbors()) {
            Message resumeMessage = new CCResumeMessage(
                    AppConfig.myServentInfo,
                    AppConfig.getInfoById(neighbour)
            );
            MessageUtil.sendMessage(resumeMessage);
        }

        // Print collected data
        int sum = 0;
        for (Map.Entry<Integer, CCSnapshot> entry : collectedData.entrySet()) {
            CCSnapshot snapshot = entry.getValue();
            sum += snapshot.getAmount();
            AppConfig.timestampedStandardPrint("[SNAPSHOT] Node" + snapshot.getServentId() + ": " + snapshot.getAmount() + " bitcakes");
        }
        AppConfig.timestampedStandardPrint("[SNAPSHOT] Total: " + sum + " bitcakes");

        endSnapshotMode();
    }

    private void startSnapshotModeInitiator() {
        snapshotMode.set(true);
        snapshotInitiatorId = AppConfig.myServentInfo.id();
    }

    public void startSnapshotModeNonInitiator(int snapshotInitiatorId) {
        snapshotMode.set(true);
        this.snapshotInitiatorId = snapshotInitiatorId;
    }

    /**
     * Called by initiator and non-initiator
     */
    public void endSnapshotMode() {
        snapshotMode.set(false);
        snapshotInitiatorId = null;
        collectedData.clear();

        processPendingTransactions();
    }

    private void processPendingTransactions() {
        for (Map.Entry<Integer, BlockingQueue<Message>> entry : MessageUtil.pendingMessages.entrySet()) {
            BlockingQueue<Message> queue = entry.getValue();
            while (!queue.isEmpty()) {
                Message message = queue.poll();
                if (message != null) {
                    MessageUtil.sendMessage(message);
                }
            }
        }
    }

    public void addSnapshot(CCSnapshot snapshot) {
        collectedData.put(snapshot.getServentId(), snapshot);
    }

    public boolean inSnapshotMode() {
        return snapshotMode.get();
    }
}
