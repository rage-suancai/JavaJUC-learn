package LockClass.ImplementLock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

public class Main {

    public static void main(String[] args) {

        MyLock myLock = new MyLock();
        Condition condition = myLock.newCondition();

        new Thread(() -> {
            myLock.lock();
            System.out.println("线程一进入等待状态");
            try {
                condition.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.out.println("线程一等待结束");
            myLock.unlock();
        }).start();

        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        new Thread(() -> {
            myLock.lock();
            System.out.println("线程二开始唤醒其他等待线程");
            condition.signal();
            System.out.println("线程二结束");
            myLock.unlock();
        }).start();

    }


}
