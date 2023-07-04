package AtomicClass.Introduce;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.*;

public class Main {

    private static AtomicInteger i = new AtomicInteger(0);

    public static void main(String[] args) {

        // test1();
        // test2();
        // test3();
        // test4();
        // test5();

        /*System.out.println("使用AtomicLong的时间消耗: " + ExampleTwo() + "ms");
        System.out.println("使用LongAdder的时间消耗: " + ExampleOne() + "ms");*/

        // test6();
        test7();

    }

    static void test1() {

        AtomicInteger i = new AtomicInteger(1);
        System.out.println(i.getAndIncrement());

    }

    static void test2() {

        Runnable r = () -> {
            for (int j = 0; j < 100000; j++)
                i.getAndIncrement();
            System.out.println("自增完成");
        };
        new Thread(r).start();
        new Thread(r).start();
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println(i.get());

    }

    static void test3() {

        AtomicInteger integer = new AtomicInteger(10);
        System.out.println(integer.compareAndSet(30, 20));
        System.out.println(integer.compareAndSet(10, 20));
        System.out.println(integer);

    }

    static void test4() {

        AtomicIntegerArray array = new AtomicIntegerArray(new int[]{0, 4, 1, 3, 5});

        Runnable r = () -> {
            for (int i = 0; i < 100000; i++) {
                array.getAndAdd(0, 1);
            }
        };
        new Thread(r).start();
        new Thread(r).start();
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println(array.get(0));

    }

    static void test5() {

        LongAdder adder = new LongAdder();

        Runnable r = () -> {
            for (int i = 0; i < 100000; i++) adder.add(1);
        };
        for (int i = 0; i < 100; i++) new Thread(r).start();
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println(adder.sum());

    }

    static long ExampleOne() {

        CountDownLatch latch = new CountDownLatch(100);
        LongAdder adder = new LongAdder();
        long timeStart = System.currentTimeMillis();

        Runnable r = () -> {
            for (int i = 0; i < 100000; i++) adder.add(1);
            latch.countDown();
        };
        for (int i = 0; i < 100; i++) new Thread(r).start();
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return System.currentTimeMillis() - timeStart;
    }
    static long ExampleTwo() {

        CountDownLatch latch = new CountDownLatch(100);
        AtomicLong atomicLong = new AtomicLong();
        long timeStart = System.currentTimeMillis();

        Runnable r = () -> {
            for (int i = 0; i < 100000; i++) atomicLong.incrementAndGet();
            latch.countDown();
        };
        for (int i = 0; i < 100; i++) new Thread(r).start();
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return System.currentTimeMillis() - timeStart;

    }

    static void test6() {

        String a = "Hello"; String b = "World";
        AtomicReference<String> reference = new AtomicReference<>(a);

        reference.compareAndSet(a, b);
        System.out.println(reference.get());

    }

    static void test7() {

        Student student = new Student();

        AtomicIntegerFieldUpdater<Student> fieldUpdater = AtomicIntegerFieldUpdater.newUpdater(Student.class, "age");
        System.out.println(fieldUpdater.incrementAndGet(student));

    }

}
