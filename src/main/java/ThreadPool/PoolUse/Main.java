package ThreadPool.PoolUse;

import java.util.concurrent.*;

public class Main {

    public static void main(String[] args) {

        // test1();
        // test2();
        // test3();
        // test4();
        // test5();
        // test6();
        test7();

    }

    static void test1() {

        ThreadPoolExecutor pool =
                new ThreadPoolExecutor(2, 4,
                        3,TimeUnit.SECONDS,
                        new ArrayBlockingQueue<>(2));

        for (int i = 0; i < 6; i++) {
            int finalI = i;
            pool.execute(() -> {
                try {
                    System.out.println(Thread.currentThread().getName() + " 开始执行 - " + finalI);
                    TimeUnit.SECONDS.sleep(1);
                    System.out.println(Thread.currentThread().getName() + " 已结束 - " + finalI);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        try {
            TimeUnit.SECONDS.sleep(1);
            System.out.println("线程池中线程数量: " + pool.getPoolSize());
            TimeUnit.SECONDS.sleep(5);
            System.out.println("线程池中线程数量: " + pool.getPoolSize());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        pool.shutdown();
        // pool.shutdownNow();

    }

    static void test2() {

        ThreadPoolExecutor pool =
                new ThreadPoolExecutor(2, 4,
                        3,TimeUnit.SECONDS,
                        new SynchronousQueue<>(),
                        new ThreadPoolExecutor.CallerRunsPolicy());

        for (int i = 0; i < 6; i++) {
            int finalI = i;
            pool.execute(() -> {
                try {
                    System.out.println(Thread.currentThread().getName() + " 开始执行 - " + finalI);
                    TimeUnit.SECONDS.sleep(1);
                    System.out.println(Thread.currentThread().getName() + " 已结束 - " + finalI);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        try {
            TimeUnit.SECONDS.sleep(1);
            System.out.println("线程池中线程数量: " + pool.getPoolSize());
            TimeUnit.SECONDS.sleep(5);
            System.out.println("线程池中线程数量: " + pool.getPoolSize());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        pool.shutdown();

    }

    static void test3() {

        ThreadPoolExecutor pool =
                new ThreadPoolExecutor(2, 4,
                        3,TimeUnit.SECONDS,
                        new ArrayBlockingQueue<>(1),
                        new ThreadPoolExecutor.DiscardOldestPolicy());

        for (int i = 0; i < 6; i++) {
            int finalI = i;
            pool.execute(() -> {
                try {
                    System.out.println(Thread.currentThread().getName() + " 开始执行 - " + finalI);
                    TimeUnit.SECONDS.sleep(1);
                    System.out.println(Thread.currentThread().getName() + " 已结束 - " + finalI);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        try {
            TimeUnit.SECONDS.sleep(1);
            System.out.println("线程池中线程数量: " + pool.getPoolSize());
            TimeUnit.SECONDS.sleep(5);
            System.out.println("线程池中线程数量: " + pool.getPoolSize());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        pool.shutdown();

    }

    static void test4() {

        ThreadPoolExecutor pool =
                new ThreadPoolExecutor(2, 4,
                        3,TimeUnit.SECONDS,
                        new SynchronousQueue<>(),
                        new ThreadPoolExecutor.DiscardOldestPolicy());

        for (int i = 0; i < 6; i++) {
            int finalI = i;
            pool.execute(() -> {
                try {
                    System.out.println(Thread.currentThread().getName() + " 开始执行 - " + finalI);
                    TimeUnit.SECONDS.sleep(1);
                    System.out.println(Thread.currentThread().getName() + " 已结束 - " + finalI);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        try {
            TimeUnit.SECONDS.sleep(1);
            System.out.println("线程池中线程数量: " + pool.getPoolSize());
            TimeUnit.SECONDS.sleep(5);
            System.out.println("线程池中线程数量: " + pool.getPoolSize());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        pool.shutdown();

    }

    static void test5() {

        ThreadPoolExecutor pool =
                new ThreadPoolExecutor(2, 4,
                        3,TimeUnit.SECONDS,
                        new SynchronousQueue<>(),
                        (r, executor) -> {
                            System.out.println("哎呀 线程池和等待队列都满了 你自己耗子尾汁吧");
                            r.run();
                        });

        for (int i = 0; i < 6; i++) {
            int finalI = i;
            pool.execute(() -> {
                try {
                    System.out.println(Thread.currentThread().getName() + " 开始执行 - " + finalI);
                    TimeUnit.SECONDS.sleep(1);
                    System.out.println(Thread.currentThread().getName() + " 已结束 - " + finalI);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        try {
            TimeUnit.SECONDS.sleep(1);
            System.out.println("线程池中线程数量: " + pool.getPoolSize());
            TimeUnit.SECONDS.sleep(5);
            System.out.println("线程池中线程数量: " + pool.getPoolSize());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        pool.shutdown();

    }

    static void test6() {

        ThreadPoolExecutor pool =
                new ThreadPoolExecutor(2, 4,
                        3, TimeUnit.SECONDS,
                        new SynchronousQueue<>(),
                        new ThreadFactory() {
                            int counter = 0;
                            @Override
                            public Thread newThread(Runnable r) {
                                return new Thread(r, "我的自定义线程 - " + counter++);
                            }
                        });

        for (int i = 0; i < 4; i++)
            pool.execute(() -> System.out.println(Thread.currentThread().getName() + " 开始执行"));
        pool.shutdown();

    }

    static void test7() {

        ThreadPoolExecutor pool = new ThreadPoolExecutor(1, 1,
                0,TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>());

        pool.execute(() -> {
            System.out.println(Thread.currentThread().getName()); throw new RuntimeException("我是异常");
        });
        try {
            TimeUnit.SECONDS.sleep(1);
            pool.execute(() -> {
                System.out.println(Thread.currentThread().getName());
            });
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        pool.shutdown();

    }

    static void test8() {

        // ExecutorService pool = Executors.newFixedThreadPool(2);
        // ExecutorService pool = Executors.newSingleThreadExecutor();

        // ExecutorService pool1 = Executors.newSingleThreadExecutor();
        // ExecutorService pool2 = Executors.newFixedThreadPool(1);

        ExecutorService pool = Executors.newCachedThreadPool();

    }

}
