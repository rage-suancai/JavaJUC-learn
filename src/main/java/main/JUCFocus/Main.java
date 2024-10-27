package main.JUCFocus;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicStampedReference;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.*;

public class Main {

    static Lock lock1 = new ReentrantLock();
    static ReentrantLock lock2 = new ReentrantLock();
    static ReentrantLock lock3 = new ReentrantLock(false);
    static ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    public static void main(String[] args) throws InterruptedException {

        //lockTest1();
        //lockTest2();
        //lockTest3();
        //lockTest4();
        //lockTest5();
        //lockTest6();
        //lockTest7();
        //lockTest8();

        //atomicTest1();
        //System.out.println("使用AtomicLong的时间消耗: " + atomicPerformance() + "ms");
        //System.out.println("使用LongAdder的时间消耗: " + longPerformance() + "ms");
        //atomicTest2();

        //concurrentTest1();
        //concurrentTest2();

        blockTest();

    }

    private static int a = 0;
    public static void lockTest1() throws InterruptedException {

        Runnable action = () -> {
            for (int j = 0; j < 100000; ++j) {
                lock1.lock();
                ++a;
                lock1.unlock();
            }
        };
        new Thread(action).start();
        new Thread(action).start();
        Thread.sleep(1000);
        System.out.println(a);

    }

    public static void lockTest2() throws InterruptedException {

        Condition condition = lock1.newCondition();

        /*new Thread(() -> {
            lock1.lock();
            System.out.println("线程一进入等待状态!");
            try {
                condition.await();
                System.out.println("线程一等待结束!");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            lock1.unlock();
        }).start();
        Thread.sleep(100);
        new Thread(() -> {
            lock1.lock();
            System.out.println("线程二开始唤醒其它等待线程");
            condition.signal();
            System.out.println("线程二结束");
            lock1.unlock();
        }).start();*/

        new Thread(() -> {
            lock1.lock();
            System.out.println("线程一进入等待状态!");
            try {
                lock1.newCondition().await();
                System.out.println("线程一等待结束!");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            lock1.unlock();
        }).start();
        Thread.sleep(100);
        new Thread(() -> {
            lock1.lock();
            System.out.println("线程二开始唤醒其它等待线程");
            lock1.newCondition().signal();
            System.out.println("线程二结束");
            lock1.unlock();
        }).start();

    }

    public static void lockTest3() throws InterruptedException {

        /*new Thread(() -> {
            lock1.lock();
            try {
                System.out.println("等待是否未超时: " + lock1.newCondition().await(3, TimeUnit.SECONDS));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();*/

        /*System.out.println("60秒 = " + TimeUnit.SECONDS.toMinutes(60) + "分钟");
        System.out.println("365天 = " + TimeUnit.DAYS.toSeconds(365) + "秒");*/

        /*synchronized (Main.class) {
            System.out.println("开始等待");
            TimeUnit.SECONDS.timedWait(Main.class, 3);
            System.out.println("等待结束");
        }*/

        TimeUnit.SECONDS.sleep(1);

    }

    public static void lockTest4() throws InterruptedException {

        Condition condition = lock2.newCondition();

        /*lock2.lock();
        lock2.lock();
        new Thread(() -> {
            System.out.println("线程二想要获取锁");
            lock2.lock();
            System.out.println("线程二成功获取到锁");
        }).start();
        lock2.unlock();
        System.out.println("线程一释放了一次锁");
        TimeUnit.SECONDS.sleep(1);
        lock2.unlock();
        System.out.println("线程一再次释放了一次锁");*/

        /*lock2.lock();
        lock2.lock();
        System.out.println("当前加锁次数: " + lock2.getHoldCount() + ", 是否被锁: " + lock2.isLocked());
        TimeUnit.SECONDS.sleep(1);
        lock2.unlock();
        System.out.println("当前加锁次数: " + lock2.getHoldCount() + ", 是否被锁: " + lock2.isLocked());
        TimeUnit.SECONDS.sleep(1);
        lock2.unlock();
        System.out.println("当前加锁次数: " + lock2.getHoldCount() + ", 是否被锁: " + lock2.isLocked());*/

        /*lock2.lock();
        Thread t1 = new Thread(lock2::lock), t2 = new Thread(lock2::lock);
        t1.start(); t2.start();
        TimeUnit.SECONDS.sleep(1);
        System.out.println("当前等待锁释放的线程数: ");
        System.out.println("线程一是否在等待队列中: " + lock2.hasQueuedThread(t1));
        System.out.println("线程二是否在等待队列中: " + lock2.hasQueuedThread(t2));
        System.out.println("当前线程是否在等待队列中: " + lock2.hasQueuedThread(Thread.currentThread()));*/

        new Thread(() -> {
            lock2.lock();
            try {
                condition.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            lock2.unlock();
        }).start();
        TimeUnit.SECONDS.sleep(1);
        lock2.lock();
        System.out.println("当前Condition的等待线程数: " + lock2.getWaitQueueLength(condition));
        condition.signal();
        System.out.println("当前Condition的等待线程数: " + lock2.getWaitQueueLength(condition));
        lock2.unlock();

    }

    public static void lockTest5() {

        Runnable action = () -> {
            System.out.println("线程 " + Thread.currentThread().getName() + " 开始获取锁...");
            lock3.lock();
            System.out.println("线程 " + Thread.currentThread().getName() + " 成功获取锁!");
            lock3.unlock();
        };
        for (int i = 0; i < 10; ++i) new Thread(action, "T" + i).start();

    }

    public static void lockTest6() throws InterruptedException {

        /*readWriteLock.readLock().lock();
        new Thread(readWriteLock.readLock()::lock).start();*/

        /*readWriteLock.readLock().lock();
        new Thread(readWriteLock.writeLock()::lock).start();*/

        /*readWriteLock.writeLock().lock();
        new Thread(readWriteLock.readLock()::lock).start();*/

        /*readWriteLock.writeLock().lock();
        readWriteLock.writeLock().lock();
        new Thread(() -> {
            readWriteLock.writeLock().lock();
            System.out.println("成功获取到写锁!");
        }).start();
        System.out.println("释放第一层锁!");
        readWriteLock.writeLock().unlock();
        TimeUnit.SECONDS.sleep(1);
        System.out.println("释放第二层锁!");
        readWriteLock.writeLock().unlock();*/

        Runnable action = () -> {
            System.out.println("线程 " + Thread.currentThread().getName() + " 将在1秒后开始获取锁...");
            readWriteLock.writeLock().lock();
            System.out.println("线程 " + Thread.currentThread().getName() + " 成功获取锁!");
            readWriteLock.writeLock().unlock();
        };
        for (int i = 0; i < 10; ++i) new Thread(action, "T" + i).start();

    }

    public static void lockTest7() throws InterruptedException {

        /*readWriteLock.writeLock().lock();
        readWriteLock.readLock().lock();
        System.out.println("成功加读锁!");*/

        /*readWriteLock.writeLock().lock();
        readWriteLock.readLock().lock();
        new Thread(() -> {
            System.out.println("开始加读锁!");
            readWriteLock.readLock().lock();
            System.out.println("读锁添加成功!");
        }).start();
        TimeUnit.SECONDS.sleep(1);
        readWriteLock.writeLock().unlock();*/

        readWriteLock.readLock().lock();
        readWriteLock.writeLock().lock();
        System.out.println("锁升级成功!");

    }

    public static void lockTest8() {

        Thread t = Thread.currentThread();
        new Thread(() -> {
            try {
                TimeUnit.SECONDS.sleep(1);
                System.out.println("主线程可以继续运行了!");
                LockSupport.unpark(t);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();
        System.out.println("主线程被挂起!");
        LockSupport.park();
        System.out.println("主线程继续运行!");

    }


    private static AtomicInteger b = new AtomicInteger(0);
    public static void atomicTest1() throws InterruptedException {

        /*int i = 1;
        System.out.println(++i);*/

        /*AtomicInteger i = new AtomicInteger(1);
        System.out.println(i.getAndIncrement());*/

        /*Runnable r = () -> {
            for (int j = 0; j < 100000; ++j) b.getAndIncrement();
            System.out.println("自增完成!");
        };
        new Thread(r).start();
        new Thread(r).start();
        TimeUnit.SECONDS.sleep(1);
        System.out.println(b.get());*/

        /*AtomicInteger integer = new AtomicInteger(10);
        System.out.println(integer.compareAndSet(30, 20));
        System.out.println(integer.compareAndSet(10, 20));
        System.out.println(integer);*/

        /*AtomicInteger integer = new AtomicInteger(1);
        integer.lazySet(2);*/

        /*AtomicIntegerArray array = new AtomicIntegerArray(new int[]{0, 4, 1, 3, 5});
        Runnable r = () -> {
            for (int i = 0; i < 100000; ++i) array.getAndAdd(0, 1);
        };
        new Thread(r).start();
        new Thread(r).start();
        TimeUnit.SECONDS.sleep(1);
        System.out.println(array.get(0));*/

        LongAdder adder = new LongAdder();
        Runnable r = () -> {
            for (int i = 0; i < 100000; ++i) adder.add(1);
        };
        for (int i = 0; i < 100; ++i) new Thread(r).start();
        TimeUnit.SECONDS.sleep(1);
        System.out.println(adder.sum());

    }

    public static long longPerformance() throws InterruptedException {

        CountDownLatch latch = new CountDownLatch(100);
        LongAdder adder = new LongAdder();

        long timeStart = System.currentTimeMillis();
        Runnable r = () -> {
            for (int i = 0; i < 100000; ++i) adder.add(1);
            latch.countDown();
        };
        for (int i = 0; i < 100; ++i) new Thread(r).start();
        latch.await();
        return System.currentTimeMillis() - timeStart;

    }
    public static long atomicPerformance() throws InterruptedException {

        CountDownLatch latch = new CountDownLatch(100);
        AtomicLong atomicLong = new AtomicLong();

        long timeStart = System.currentTimeMillis();
        Runnable r = () -> {
            for (int i = 0; i < 100000; ++i) atomicLong.incrementAndGet();
            latch.countDown();
        };
        for (int i = 0; i < 100; ++i) new Thread(r).start();
        latch.await();
        return System.currentTimeMillis() - timeStart;

    }

    public static void atomicTest2() {

        String a = "Fuck";
        String b = "World";

        AtomicStampedReference<Object> reference = new AtomicStampedReference<>(a, 1);
        reference.attemptStamp(a, 2);
        System.out.println(reference.compareAndSet(a,b, 2,3));

    }


    public static void concurrentTest1() throws InterruptedException {

        /*List<String> list = new ArrayList<>();
        Runnable r = () -> {
            for (int i = 0; i < 100; i++) list.add("fuc");
        };
        for (int i = 0; i < 100; i++) new Thread(r).start();
        TimeUnit.SECONDS.sleep(1);
        System.out.println(list.size());*/

        HashMap<Integer, Object> map = new HashMap<>();
        for (int i = 0; i < 100; ++i) {
            int finalI = i;
            new Thread(() -> {
                for (int j = 0; j < 100; ++j) map.put(finalI*1000+j, "fuck");
            }).start();
        }
        TimeUnit.SECONDS.sleep(1);
        System.out.println(map.size());

    }

    public static void concurrentTest2() throws InterruptedException {

        /*List<String> list = new CopyOnWriteArrayList<>();
        Runnable r = () -> {
            for (int i = 0; i < 100; ++i) list.add("fuck");
        };
        for (int i = 0; i < 100; ++i) new Thread(r).start();
        TimeUnit.SECONDS.sleep(1);
        System.out.println(list.size());*/

        Map<Integer, String> map = new ConcurrentHashMap<>();
        for (int i = 0; i < 100; ++i) {
            int finalI = i;
            new Thread(() -> {
                for (int j = 0; j < 100; ++j) map.put(finalI*100+j, "fuck");
            }).start();
        }
        TimeUnit.SECONDS.sleep(1);
        System.out.println(map.size());

    }


    public static void blockTest() throws InterruptedException {

        /*PriorityBlockingQueue<Integer> queue = new PriorityBlockingQueue<>(10, Integer::compare);
        queue.add(3); queue.add(1); queue.add(2);
        System.out.println(queue);
        System.out.println(queue.poll());
        System.out.println(queue.poll());
        System.out.println(queue.poll());*/

        DelayQueue<Delayed> queue = new DelayQueue<>();
        queue.add(new Delay(1, 2, "2号"));
        queue.add(new Delay(3, 1, "1号"));
        System.out.println(queue.take());
        System.out.println(queue.take());



    }
    private static class Delay implements Delayed {

        private final long time;
        private final int priority;
        private final long startTime;
        private final String data;

        private Delay(long time, int priority, String data) {

            this.time = TimeUnit.SECONDS.toMillis(time);
            this.priority = priority;
            this.startTime = System.currentTimeMillis();
            this.data = data;

        }

        @Override
        public long getDelay(TimeUnit timeUnit) {

            long leftTime = time - (System.currentTimeMillis() - startTime);
            return timeUnit.convert(leftTime, TimeUnit.MILLISECONDS);

        }

        @Override
        public int compareTo(Delayed delayed) {

            if (delayed instanceof Delay) return priority - ((Delay) delayed).priority;
            return 0;

        }

        @Override
        public String toString() {
            return data;
        }

    }

}
