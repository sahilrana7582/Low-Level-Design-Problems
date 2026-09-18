package entities;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;

public class MpscRingBuffer<T> {
    private final Object[] buffer;

    // Per-slot publish stamp. slotSequence[i] == pos means "slot i holds no
    // data yet and is claimable for position pos". slotSequence[i] == pos + 1
    // means "slot i's write for position pos is fully published and safe to
    // read". This is what offer()/poll() previously lacked: a per-slot signal
    // that is only raised AFTER the payload write, so a consumer can never
    // observe a reserved-but-unwritten slot.
    private final AtomicLongArray slotSequence;
    private final int capacity;

    // Next sequence that a producer will try to reserve.
    private final AtomicLong producerSequence = new AtomicLong(0);

    // Next sequence that the consumer will try to read.
    private final AtomicLong consumerSequence = new AtomicLong(0);

    public MpscRingBuffer(int capacity){
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be > 0");
        }

        this.capacity = capacity;
        this.buffer = new Object[capacity];
        this.slotSequence = new AtomicLongArray(capacity);
        for (int i = 0; i < capacity; i++) {
            slotSequence.set(i, i);
        }
    }

    public boolean offer(T value){
        long pos = producerSequence.get();

        while (true) {
            int index = (int) (pos % capacity);
            long seq = slotSequence.get(index);
            long diff = seq - pos;

            if (diff == 0) {
                if (producerSequence.compareAndSet(pos, pos + 1)) {
                    break;
                }
                pos = producerSequence.get(); // lost the CAS race; retry
            } else if (diff < 0) {
                return false; // slot still holds an unread element -> full
            } else {
                pos = producerSequence.get(); // another producer moved ahead; retry
            }
        }

        int index = (int) (pos % capacity);
        buffer[index] = value;
        slotSequence.set(index, pos + 1); // publish: data write now happens-before this
        return true;
    }


    @SuppressWarnings("unchecked")
    public T poll() {

        long pos = consumerSequence.get();
        int index = (int) (pos % capacity);
        long seq = slotSequence.get(index);

        // Producer hasn't published this exact slot yet.
        if (seq - (pos + 1) != 0) {
            return null;
        }

        T value = (T) buffer[index];

        buffer[index] = null;
        slotSequence.set(index, pos + capacity); // free the slot for the next lap

        consumerSequence.lazySet(pos + 1);

        return value;
    }


    public boolean isEmpty() {
        return consumerSequence.get() >= producerSequence.get();
    }

    public boolean isFull() {
        return producerSequence.get() - consumerSequence.get() >= capacity;
    }

    public int size() {
        return (int) (
                producerSequence.get() -
                        consumerSequence.get()
        );
    }

    public int capacity() {
        return capacity;
    }


}
