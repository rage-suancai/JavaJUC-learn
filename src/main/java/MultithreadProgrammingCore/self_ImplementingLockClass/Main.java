package MultithreadProgrammingCore.self_ImplementingLockClass;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * 自行实现锁类
 * 既然前面了解了那么多AQS的功能 那么我们就仿照这些锁类来实现一个简单的锁:
 *
 *      > 要求: 同一时间只能有一个线程持有锁 不要求可重入(反复加锁无视即可)
 *
 * 设计思路
 *
 *      1. 锁被占用 那么exclusiveOwnerThread应该被记录 并且state = 1
 *      2. 锁没有被占用 那么exclusiveOwnerThread为null 并且state = 0
 *
 * 到这里 我们对队列同步器AQS的讲解就先到此为止了 当然 AQS的全部机制并非仅仅只有我们讲解的内容 一些我们没有提到的内容 还请各位自行探索 会有满满的成就感哦~
 */
public class Main {

    public static void main(String[] args) {

        MyLock lock = new MyLock();
        Condition condition = lock.newCondition();
        lock.lock();
        System.out.println("主线程加锁成功");
        new Thread(() -> {
            try {
                TimeUnit.SECONDS.sleep(1);
                lock.lock();
                System.out.println("线程一加锁成功");
                System.out.println("线程一开始唤醒主线程"); condition.signal();
                lock.unlock();
                System.out.println("线程一解锁成功");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
        try {
            System.out.println("主线程开始等待..."); condition.await();
            System.out.println("主线程结束等待");
            lock.unlock();
            System.out.println("主线程解锁成功");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private static class MyLock implements Lock {

        private class Sync extends AbstractQueuedSynchronizer {

            @Override
            protected boolean tryAcquire(int arg) {
                if (isHeldExclusively()) return true; // 无需可重入功能 如果是当前线程直接返回true
                if (compareAndSetState(0, arg)) { // CAS操作进行状态替换
                    setExclusiveOwnerThread(Thread.currentThread()); // 成功后设置当前的所有者线程
                    return true;
                }
                return false;
            }

            @Override
            protected boolean tryRelease(int arg) {
                if (getState() == 0)
                    throw new IllegalMonitorStateException(); // 没加锁情况下是不能直接解锁的
                if (isHeldExclusively()) { // 只有持有锁的线程才能解锁
                    setExclusiveOwnerThread(null); // 设置所有者线程为null
                    setState(0); // 状态变为0
                    return true;
                }
                return false;
            }

            @Override
            protected boolean isHeldExclusively() {
                return getExclusiveOwnerThread() == Thread.currentThread();
            }

            protected Condition newCondition() {
                return new ConditionObject(); // 直接使用现成的
            }

        }

        Sync sync = new Sync();

        @Override
        public void lock() {
            sync.acquire(1);
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
            sync.acquireInterruptibly(1);
        }

        @Override
        public boolean tryLock() {
            return sync.tryAcquire(1);
        }

        @Override
        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
            return sync.tryAcquireNanos(1, unit.toNanos(time));
        }

        @Override
        public void unlock() {
            sync.release(1);
        }

        @Override
        public Condition newCondition() {
            return sync.newCondition();
        }

    }

}
