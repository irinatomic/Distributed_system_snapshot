package com.kids.servent.bitcake;

import com.kids.communication.message.Message;
import com.kids.communication.message.MessageType;
import com.kids.communication.message.util.MessageUtil;
import com.kids.servent.config.AppConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class AVBitcakeManager implements BitcakeManager {

    private final AtomicInteger amount = new AtomicInteger(1000);
    private final AtomicInteger snapshotStartAmount = new AtomicInteger(0);
    private final List<Message> receivedDuringSnapshot = new ArrayList<>();

    @Override
    public void takeSomeBitcakes(int amount) {
        this.amount.getAndAdd(-amount);
    }

    @Override
    public void addSomeBitcakes(int amount) {
        this.amount.getAndAdd(amount);
    }

    @Override
    public int getCurrentBitcakeAmount() {
        return amount.get();
    }

    public void startSnapshotMode() {
        snapshotStartAmount.set(amount.get());
    }

    public void endSnapshotMode() {

        int totalSnapshotAmount = snapshotStartAmount.get();
        AppConfig.timestampedStandardPrint("[SNAPSHOT] Start amount: " + snapshotStartAmount.get());

        for (Message message : receivedDuringSnapshot) {
            if (MessageType.TRANSACTION == message.getMessageType()) {
                int amount = MessageUtil.getAmountFromMessage(message);
                if (amount == -1) continue;
                totalSnapshotAmount += amount;
                AppConfig.timestampedStandardPrint("[SNAPSHOT] From node" + message.getOriginalSenderInfo().id() + ": " + amount);
            }
        }

        AppConfig.timestampedStandardPrint("[SNAPSHOT] Total amount: " + totalSnapshotAmount);
        snapshotStartAmount.set(0);
        receivedDuringSnapshot.clear();
    }

    public void addMessageReceivedDuringSnapshot(Message message) {
        receivedDuringSnapshot.add(message);
    }
}
