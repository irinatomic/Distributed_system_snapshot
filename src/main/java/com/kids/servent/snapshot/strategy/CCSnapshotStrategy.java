package com.kids.servent.snapshot.strategy;

import com.kids.communication.message.Message;
import com.kids.communication.message.impl.cc.CCResumeMessage;
import com.kids.communication.message.impl.cc.CCSnapshotRequestMessage;
import com.kids.communication.message.util.MessageUtil;
import com.kids.servent.bitcake.CCBitcakeManager;
import com.kids.servent.config.AppConfig;
import com.kids.servent.snapshot.data.CCSnapshot;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class CCSnapshotStrategy implements SnapshotStrategy {

    private final AtomicBoolean snapshotMode = new AtomicBoolean(false);
    private final Map<Integer, CCSnapshot> collectedData = new ConcurrentHashMap<>();
    private final CCBitcakeManager bitcakeManager;

    public CCSnapshotStrategy(CCBitcakeManager bitcakeManager) {
        this.bitcakeManager = bitcakeManager;
    }

    @Override
    public void initiateSnapshot() {
        AppConfig.timestampedStandardPrint("2");
        if (inSnapshotMode()) {
            AppConfig.timestampedStandardPrint("[SNAPSHOT] Already in snapshot mode");
        }
        startSnapshotMode();

        AppConfig.timestampedStandardPrint("3");

        // Send SNAPSHOT_REQUEST message to all neighbours
        for (Integer neighbor : AppConfig.myServentInfo.neighbors()) {
            CCSnapshotRequestMessage neighborRequest = new CCSnapshotRequestMessage(
                    AppConfig.myServentInfo,
                    AppConfig.getInfoById(neighbor),
                    AppConfig.myServentInfo.id()
            );

            MessageUtil.sendMessage(neighborRequest);
        }

        AppConfig.timestampedStandardPrint("4");

        // Save the current state
        CCSnapshot snapshotResult = new CCSnapshot(
                AppConfig.myServentInfo.id(),
                bitcakeManager.getCurrentBitcakeAmount()
        );
        collectedData.put(AppConfig.myServentInfo.id(), snapshotResult);
    }

    @Override
    public boolean isSnapshotComplete() {
        return inSnapshotMode() && collectedData.size() == AppConfig.getServentCount();
    }

    @Override
    public void processSnapshotEnding() {
        AppConfig.timestampedStandardPrint("[SNAPSHOT] Received ACK from all nodes. Ending snapshot.");

        broadcastResumeMessage();
        printCollectedData();
        endSnapshotMode();
    }

    private void broadcastResumeMessage() {
        Message resumeMessage = new CCResumeMessage(
                AppConfig.myServentInfo,
                null
        );
        AppConfig.myServentInfo.neighbors().stream()
                .map(resumeMessage::changeReceiver)
                .forEach(MessageUtil::sendMessage);
    }

    private void printCollectedData() {
        int sum = 0;
        for (Map.Entry<Integer, CCSnapshot> entry : collectedData.entrySet()) {
            CCSnapshot snapshot = entry.getValue();
            sum += snapshot.getAmount();
            AppConfig.timestampedStandardPrint("[SNAPSHOT] Node" + snapshot.getServentId() + ": " + snapshot.getAmount() + " bitcakes");
        }
        AppConfig.timestampedStandardPrint("[SNAPSHOT] Total: " + sum + " bitcakes");
    }

    public void addSnapshot(CCSnapshot snapshot) {
        if (inSnapshotMode()) {
            collectedData.put(snapshot.getServentId(), snapshot);
        } else {
            AppConfig.timestampedStandardPrint("[SNAPSHOT] Not in snapshot mode, ignoring snapshot from node" + snapshot.getServentId());
        }
    }

    public boolean inSnapshotMode() {
        return snapshotMode.get();
    }

    public void startSnapshotMode() {
        snapshotMode.set(false);
    }

    public void endSnapshotMode() {
        snapshotMode.set(false);

        // Process any pending transactions
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
}
