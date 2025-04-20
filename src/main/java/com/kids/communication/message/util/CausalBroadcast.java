package com.kids.communication.message.util;

import com.kids.communication.handler.impl.av.AVDoneHandler;
import com.kids.communication.handler.impl.av.AVSnapshotRequestHandler;
import com.kids.communication.handler.impl.av.AVTerminateHandler;
import com.kids.servent.config.AppConfig;
import com.kids.servent.snapshot.collector.SnapshotCollector;
import com.kids.communication.handler.impl.ab.ABSnapshotRequestHandler;
import com.kids.communication.handler.impl.ab.ABSnapshotResponseHandler;
import com.kids.communication.handler.impl.TransactionHandler;
import com.kids.communication.message.Message;
import com.kids.communication.message.impl.BasicMessage;
import com.kids.servent.snapshot.strategy.SnapshotStrategy;
import lombok.Getter;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

/**
 * This class contains shared data for the Causal Broadcast implementation:
 * <ul>
 * <li> Vector clock for current instance
 * <li> Commited message list
 * <li> Pending queue
 * </ul>
 * As well as operations for working with all of the above.

 */
public class CausalBroadcast {

    private static final ExecutorService executor = Executors.newWorkStealingPool();

    @Getter private static final Map<Integer, Integer> vectorClock = new ConcurrentHashMap<>();
    private static final Queue<Message> pendingMessages = new ConcurrentLinkedQueue<>();
    private static final Object lock = new Object();

    @Getter private static SnapshotCollector snapshotCollector;
    @Getter private static SnapshotStrategy snapshotStrategy;

    // AB Snapshot
    @Getter  private static final List<Message> sent = new CopyOnWriteArrayList<>();
    @Getter  private static final List<Message> received = new CopyOnWriteArrayList<>();
    private static final Set<Message> receivedAbRequest = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * Initializes the vector clock with the given number of servents.
     * @param serventCount the number of servents in the system.
     */
    public static void initializeVectorClock(int serventCount) {
        Stream.iterate(0, i -> i + 1)
                .limit(serventCount)
                .forEach(i -> vectorClock.put(i, 0));
    }

    /**
     * Compares two vector clocks.
     * Returns true if any entry in clock2 is greater than the corresponding entry in clock1.
     * @param clock1 the first vector clock.
     * @param clock2 the second vector clock.
     * @return true if clock2 is greater in any entry, false otherwise.
     * @throws IllegalArgumentException if the clocks are of different sizes.
     */
    public static boolean otherClockGreater(Map<Integer, Integer> clock1, Map<Integer, Integer> clock2) {
        if (clock1.size() != clock2.size()) {
            throw new IllegalArgumentException("Clocks are not same size how why");
        }

        for(int i = 0; i < clock1.size(); i++) {
            if (clock2.get(i) > clock1.get(i)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Increments the causal clock based on a new message.
     * Also checks for and processes any pending messages.
     *
     * @param newMessage the message triggering the clock update.
     */
    public static void causalClockIncrement(Message newMessage) {
        AppConfig.timestampedStandardPrint("Committing # " + newMessage);
        incrementClock(newMessage.getOriginalSenderInfo().id());
        checkPendingMessages();
    }

    /**
     * Checks the pending messages queue and commits messages that satisfy the causal order.
     */
    public static void checkPendingMessages() {
        boolean working = true;

        while (working) {
            working = false;

            synchronized (lock) {
                Iterator<Message> iterator = pendingMessages.iterator();
                // Create a copy of current vector clock to compare with each pending message.
                Map<Integer, Integer> myVectorClock = getVectorClock();

                while (iterator.hasNext()) {
                    Message pendingMessage = iterator.next();
                    BasicMessage basicMessage = (BasicMessage) pendingMessage;

                    // If current vector clock does not lag behind the sender's clock, commit the message.
                    if (!otherClockGreater(myVectorClock, basicMessage.getSenderVectorClock())) {
                        working = true;

                        AppConfig.timestampedStandardPrint("Committing: " + pendingMessage);
                        // Update the vector clock for the message's original sender
                        incrementClock(pendingMessage.getOriginalSenderInfo().id());

                        boolean added;
                        switch (basicMessage.getMessageType()) {
                            case TRANSACTION -> {
                                if (basicMessage.getOriginalReceiverInfo().id() == AppConfig.myServentInfo.id())
                                    executor.submit(new TransactionHandler(basicMessage));
                            }
                            case AB_SNAPSHOT_REQUEST -> {
                                added = receivedAbRequest.add(basicMessage);
                                if (added) executor.submit(new ABSnapshotRequestHandler(basicMessage));
                            }
                            case AB_SNAPSHOT_RESPONSE -> {
                                if (basicMessage.getOriginalReceiverInfo().id() == AppConfig.myServentInfo.id())
                                    executor.submit(new ABSnapshotResponseHandler(basicMessage, snapshotCollector));
                            }
                            case AV_SNAPSHOT_REQUEST -> {
                                executor.submit(new AVSnapshotRequestHandler(basicMessage));
                            }
                            case AV_DONE -> {
                                if (basicMessage.getOriginalReceiverInfo().id() == AppConfig.myServentInfo.id()) {
                                    executor.submit(new AVDoneHandler(basicMessage));
                                }
                            }
                            case AV_TERMINATE -> {
                                executor.submit(new AVTerminateHandler(basicMessage));
                            }
                        }

                        iterator.remove();
                        break;
                    }
                }
            }
        }
    }

    /**
     * Increments the vector clock entry for a specific servent.
     *
     * @param serventId the identifier of the servent.
     */
    public static void incrementClock(int serventId) {
        vectorClock.computeIfPresent(serventId, (key, oldValue) -> oldValue + 1);
    }

    public static void injectSnapshotCollector(SnapshotCollector snapshotCollector) {
        CausalBroadcast.snapshotCollector = snapshotCollector;
        CausalBroadcast.snapshotStrategy = snapshotCollector.getSnapshotStrategy();
    }

    public static void addPendingMessage(Message msg) {
        pendingMessages.add(msg);
    }

    public static void addReceivedMessage(Message receivedMessage) {
        received.add(receivedMessage);
    }

    public static void addSentMessage(Message sentMessage) {
        sent.add(sentMessage);
    }

}
