package com.kids.servent.bitcake;

import java.util.concurrent.atomic.AtomicInteger;

public class CCBitcakeManager implements BitcakeManager {

    private final AtomicInteger amount = new AtomicInteger(1000);

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
}
