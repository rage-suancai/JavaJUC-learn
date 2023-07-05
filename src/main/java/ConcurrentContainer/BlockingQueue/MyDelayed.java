package ConcurrentContainer.BlockingQueue;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class MyDelayed implements Delayed {

    private final long time;
    private final int priority;
    private final long startTime;
    private final String data;

    public MyDelayed(long time, int priority, String data) {
        this.time = TimeUnit.SECONDS.toMillis(time);
        this.priority = priority;
        this.startTime = System.currentTimeMillis();
        this.data = data;
    }

    @Override
    public long getDelay(TimeUnit unit) {

        long leftTime = time - (System.currentTimeMillis() - startTime);
        return unit.convert(leftTime, TimeUnit.MILLISECONDS);

    }

    @Override
    public int compareTo(Delayed o) {

        if (o instanceof MyDelayed) return priority - ((MyDelayed) o).priority;
        return 0;

    }

    @Override
    public String toString() {
        return data;
    }

}
