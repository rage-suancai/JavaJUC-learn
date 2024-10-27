package main.MyLock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class Main {

    public static void main(String[] args) throws InterruptedException {

        MyLock myLock = new MyLock();
        Condition condition = myLock.newCondition();

        new Thread(() -> {
            myLock.lock();
            System.out.println("线程一进入等待状态!");
            try {
                condition.await();
                System.out.println("线程一等待结束!");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            myLock.unlock();
        }).start();
        Thread.sleep(100);
        new Thread(() -> {
            myLock.lock();
            System.out.println("线程二开始唤醒其它等待线程");
            condition.signal();
            System.out.println("线程二结束");
            myLock.unlock();
        }).start();

    }

    private static class MyLock implements Lock {

        private static class Sync extends AbstractQueuedSynchronizer {

            @Override
            protected boolean tryAcquire(int arg) {

                if (compareAndSetState(0, arg)) {
                    setExclusiveOwnerThread(Thread.currentThread()); return true;
                }
                return false;

            }

            @Override
            protected boolean tryRelease(int arg) {

                if (getState() == 0) throw new IllegalMonitorStateException();
                setExclusiveOwnerThread(null); setState(0); return true;

            }

            @Override
            protected boolean isHeldExclusively() {
                return getExclusiveOwnerThread() == Thread.currentThread();
            }

            protected  Condition newCondition() {
                return new ConditionObject();
            }

        }

        private final Sync sync = new Sync();

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
        public boolean tryLock(long l, TimeUnit timeUnit) throws InterruptedException {
            return sync.tryAcquireNanos(1, timeUnit.toNanos(l));
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
