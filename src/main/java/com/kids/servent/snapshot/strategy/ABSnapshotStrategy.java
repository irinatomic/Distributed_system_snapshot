package com.kids.servent.snapshot.strategy;

import com.kids.servent.config.AppConfig;
import com.kids.communication.message.util.CausalBroadcast;
import com.kids.servent.bitcake.ABBitcakeManager;
import com.kids.servent.snapshot.ABSnapshot;
import com.kids.communication.message.Message;
import com.kids.communication.message.impl.ABSnapshotRequestMessage;
import com.kids.communication.message.util.MessageUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@AllArgsConstructor
public class ABSnapshotStrategy implements SnapshotStrategy {

    private final Map<Integer, ABSnapshot> collectedData = new ConcurrentHashMap<>();
    private final ABBitcakeManager bitcakeManager;

    @Override
    public void initiateSnapshot() {
        // Create SNAPSHOT_REQUEST message
        Map<Integer, Integer> vectorClock = new ConcurrentHashMap<>(CausalBroadcast.getVectorClock());
        Message request = new ABSnapshotRequestMessage(AppConfig.myServentInfo, null, null, vectorClock);

        // Send SNAPSHOT_REQUEST message to all neighbors
        for (Integer neighbor : AppConfig.myServentInfo.neighbors()) {
            Message neighborRequest = request.changeReceiver(neighbor);
            MessageUtil.sendMessage(neighborRequest);
        }

        // Save the current state
        ABSnapshot snapshotResult = new ABSnapshot(
                AppConfig.myServentInfo.id(),
                bitcakeManager.getCurrentBitcakeAmount(),
                new ArrayList<>(CausalBroadcast.getSent()),
                new ArrayList<>(CausalBroadcast.getReceived())
        );
        collectedData.put(AppConfig.myServentInfo.id(), snapshotResult);

        CausalBroadcast.causalClockIncrement(request);
    }

    @Override
    public boolean isSnapshotComplete() {
        return collectedData.size() == AppConfig.getServentCount();
    }

    @Override
    public void processCollectedData() {
        int sum = 0;
        int serventCount = AppConfig.getServentCount();

        AppConfig.timestampedStandardPrint("[SNAPSHOT] " + collectedData.get(4).getReceived());

        for (Map.Entry<Integer, ABSnapshot> entry : collectedData.entrySet()) {
            int j = entry.getKey();
            ABSnapshot snapshot = entry.getValue();

            int bitcakeAmount = snapshot.getAmount();
            sum += bitcakeAmount;
            AppConfig.timestampedStandardPrint("[SNAPSHOT] node" + j + ": " + bitcakeAmount + " bitcake");

            for (int i = 0; i < serventCount; i++) {
                if (i == j) continue;

                int sentToNode = bitcakeCountSentToNode(snapshot.getSent(), i);
                int receivedFromNode = bitcakeCountReceivedFromNode(
                        collectedData.get(i).getReceived(),
                        j
                );
                int inChannel = sentToNode - receivedFromNode;
                sum += inChannel;
                AppConfig.timestampedStandardPrint("[SNAPSHOT] Channel  [" + j + "->" + i +"]: " + inChannel);
            }
        }

        AppConfig.timestampedStandardPrint("[SNAPSHOT] System bitcake count: " + sum);
        collectedData.clear();
    }

    private int bitcakeCountSentToNode(List<Message> sent, int nodeId) {
        int sum = 0;
        for (Message sentTransaction : sent) {
            if (sentTransaction.getOriginalReceiverInfo().id() == nodeId) {
                int amountNumber = Integer.parseInt(sentTransaction.getMessageText());
                sum += amountNumber;
            }
        }
        return sum;
    }

    private int bitcakeCountReceivedFromNode(List<Message> received, int ourNodeId) {
        int sum = 0;
        for (Message receivedTransaction : received) {
            if (receivedTransaction.getOriginalSenderInfo().id() == ourNodeId) {
                int amountNumber = Integer.parseInt(receivedTransaction.getMessageText());
                sum += amountNumber;
            }
        }
        return sum;
    }
}
