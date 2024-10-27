package main.JUCAdvanced;

import java.util.Random;
import java.util.concurrent.*;

public class Main {

    public static void main(String[] args) throws InterruptedException, ExecutionException {

        //poolTest1();
        //poolTest2();
        //poolTest3();

        //counterTest();

        //cyclicBarrierTest();

        //exchangerTest();

        ForkJoinPool pool = new ForkJoinPool();
        System.out.println(pool.submit(new SubTask(1, 1000)).get());


    }

    public static void poolTest1() throws InterruptedException {

        /*ThreadPoolExecutor executor =
                new ThreadPoolExecutor(2, 4,
                        3, TimeUnit.SECONDS,
                        new ArrayBlockingQueue<>(2));

        for (int i = 0; i < 6; ++i) {
            int finalI = i;
            executor.execute(() -> {
                try {
                    System.out.println(Thread.currentThread().getName() + " 开始执行! (" + finalI);
                    TimeUnit.SECONDS.sleep(1);
                    System.out.println(Thread.currentThread().getName() + " 已结束! (" + finalI);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        TimeUnit.SECONDS.sleep(1);
        System.out.println("线程池中线程数量: " + executor.getPoolSize());
        TimeUnit.SECONDS.sleep(5);
        System.out.println("线程池中线程数量: " + executor.getPoolSize());
        executor.shutdownNow();*/

        /*ThreadPoolExecutor executor =
                new ThreadPoolExecutor(2, 4,
                        3, TimeUnit.SECONDS,
                        new SynchronousQueue<>(), new ThreadPoolExecutor.CallerRunsPolicy());

        for (int i = 0; i < 6; ++i) {
            int finalI = i;
            executor.execute(() -> {
                try {
                    System.out.println(Thread.currentThread().getName() + " 开始执行! (" + finalI);
                    TimeUnit.SECONDS.sleep(1);
                    System.out.println(Thread.currentThread().getName() + " 已结束! (" + finalI);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        TimeUnit.SECONDS.sleep(1);
        System.out.println("线程池中线程数量: " + executor.getPoolSize());
        TimeUnit.SECONDS.sleep(5);
        System.out.println("线程池中线程数量: " + executor.getPoolSize());
        executor.shutdownNow();*/

        /*ThreadPoolExecutor executor =
                new ThreadPoolExecutor(2, 4,
                        3, TimeUnit.SECONDS,
                        new ArrayBlockingQueue<>(1), new ThreadPoolExecutor.DiscardOldestPolicy());

        for (int i = 0; i < 6; ++i) {
            int finalI = i;
            executor.execute(() -> {
                try {
                    System.out.println(Thread.currentThread().getName() + " 开始执行! (" + finalI);
                    TimeUnit.SECONDS.sleep(1);
                    System.out.println(Thread.currentThread().getName() + " 已结束! (" + finalI);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        TimeUnit.SECONDS.sleep(1);
        System.out.println("线程池中线程数量: " + executor.getPoolSize());
        TimeUnit.SECONDS.sleep(5);
        System.out.println("线程池中线程数量: " + executor.getPoolSize());
        executor.shutdownNow();*/

        /*ThreadPoolExecutor executor =
                new ThreadPoolExecutor(2, 4,
                        3, TimeUnit.SECONDS,
                        new SynchronousQueue<>(),
                        new ThreadFactory() {
                            int counter = 0;
                            @Override
                            public Thread newThread(Runnable runnable) {
                                return new Thread(runnable, "我的的自定义线程-" + counter++);
                            }
                        });

        for (int i = 0; i < 4; ++i) {
            executor.execute(() -> System.out.println(Thread.currentThread().getName() + " 开始执行"));
        }*/

        ThreadPoolExecutor executor =
                new ThreadPoolExecutor(1, 1,
                        0, TimeUnit.MICROSECONDS, new LinkedBlockingDeque<>());
        executor.execute(() -> {
            System.out.println(Thread.currentThread().getName());
            throw new RuntimeException("我是异常!");
        });
        TimeUnit.SECONDS.sleep(1);
        executor.execute(() -> {
            System.out.println(Thread.currentThread().getName());
        });

    }

    public static void poolTest2() throws ExecutionException, InterruptedException {

        /*ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<String> future = executor.submit(() -> "我是字符串");
        System.out.println(future.get());
        executor.shutdown();*/

        /*ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<String> future = executor.submit(() -> {
            try {
                TimeUnit.SECONDS.sleep(3);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, "我是字符串!");
        System.out.println(future.get());
        executor.shutdown();*/

        /*ExecutorService service = Executors.newSingleThreadExecutor();
        FutureTask<String> task = new FutureTask<>(() -> "我是字符串!");
        service.submit(task);
        System.out.println(task.get());
        service.shutdown();*/

        /*ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<String> future = executor.submit(() -> "都看到这里了 不来个赞吗?");
        System.out.println(future.get());
        System.out.println("任务是否执行完成: " + future.isDone());
        System.out.println("任务是否被取消: " + future.isCancelled());
        executor.shutdown();*/

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<String> future = executor.submit(() -> {
            TimeUnit.SECONDS.sleep(10);
            return "这次一定!";
        });
        System.out.println(future.cancel(true));
        System.out.println(future.isCancelled());
        executor.shutdown();

    }

    public static void poolTest3() throws ExecutionException, InterruptedException {

        /*ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
        executor.schedule(() -> System.out.println("FuckWorld"), 3, TimeUnit.SECONDS);
        executor.shutdown();*/

        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(2);
        ScheduledFuture<String> future = executor.schedule(() -> "????", 3, TimeUnit.SECONDS);
        System.out.println("任务剩余等待时间: " + future.getDelay(TimeUnit.MILLISECONDS) / 1000.0 + "s");
        System.out.println("任务执行结果: " + future.get());
        executor.shutdown();

    }


    public static void counterTest() throws InterruptedException {

        CountDownLatch latch = new CountDownLatch(20);
        for (int i = 0; i < 20; ++i) {
            int finalI = i;
            new Thread(() -> {
                try {
                    Thread.sleep((long) (2000 * new Random().nextDouble()));
                    System.out.println("子任务" + finalI + "执行完成");
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                latch.countDown();
            }).start();
        }
        latch.await();
        System.out.println("所有子任务都完成! 任务完成!!!");


    }


    public static void cyclicBarrierTest() throws InterruptedException {

        /*CyclicBarrier barrier = new CyclicBarrier(10, () -> System.out.println("飞机马上就要起飞了 各位特种兵请准备"));
        for (int i = 0; i <= 10; ++i) {
            int finalI = i;
            new Thread(() -> {
                try {
                    Thread.sleep((long) (2000 * new Random().nextDouble()));
                    System.out.println("玩家 " + finalI + "进入房间进行等待... (" + barrier.getNumberWaiting() + "/10)");
                    barrier.await();
                    System.out.println("玩家 " + finalI + " 进入游戏");
                } catch (InterruptedException | BrokenBarrierException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }*/

        /*CyclicBarrier barrier = new CyclicBarrier(5);
        for (int i = 0; i <= 10; ++i) {
            int finalI = i;
            new Thread(() -> {
                try {
                    Thread.sleep((long) (2000 * new Random().nextDouble()));
                    System.out.println("玩家 " + finalI + "进入房间进行等待... (" + barrier.getNumberWaiting() + "/5)");
                    barrier.await();
                    System.out.println("玩家 " + finalI + " 进入游戏!");
                } catch (InterruptedException | BrokenBarrierException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }*/

        /*CyclicBarrier barrier = new CyclicBarrier(5);
        for (int i = 0; i < 3; ++i) {
            new Thread(() -> {
                try {
                    barrier.await();
                } catch (InterruptedException | BrokenBarrierException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }
        Thread.sleep(500);
        System.out.println("当前屏障前的等待线程数: " + barrier.getNumberWaiting());
        barrier.reset();
        System.out.println("重置后屏障前的等待线程数: " + barrier.getNumberWaiting());*/

        /*Semaphore semaphore = new Semaphore(2);
        for (int i = 0; i < 3; ++i) {
            new Thread(() -> {
                try {
                    semaphore.acquire();
                    System.out.println("许可证申请成功!");
                    semaphore.release();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }*/

        /*Semaphore semaphore = new Semaphore(3);
        for (int i = 0; i < 2; ++i) {
            new Thread(() -> {
                try {
                    semaphore.acquire(2);
                    System.out.println("许可证申请成功!");
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }*/

        /*Semaphore semaphore = new Semaphore(3);
        for (int i = 0; i < 5; ++i) new Thread(semaphore::acquireUninterruptibly).start();
        Thread.sleep(500);
        System.out.println("剩余许可证数量: " + semaphore.availablePermits());
        System.out.println("是否存在线程等待许可证: " + (semaphore.hasQueuedThreads() ? "是" : "否"));
        System.out.println("等待许可证线程数量: " + semaphore.getQueueLength());*/

        Semaphore semaphore = new Semaphore(3);
        new Thread(semaphore::acquireUninterruptibly).start();
        Thread.sleep(500);
        System.out.println("收回剩余许可数量: " + semaphore.drainPermits());

    }


    public static void exchangerTest() throws InterruptedException {

        Exchanger<Object> exchanger = new Exchanger<>();
        new Thread(() -> {
            try {
                System.out.println("收到主线程传递的交换数据: " + exchanger.exchange("AAAA"));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();
        System.out.println("收到子线程传递的交换数据: " + exchanger.exchange("BBBB"));

    }


    private static class SubTask extends RecursiveTask<Integer> {

        private final int start;
        private final int end;

        public SubTask(int start, int end) {

            this.start = start;
            this.end = end;

        }

        @Override
        protected Integer compute() {

            if (end - start > 125) {
                SubTask subTask1 = new SubTask(start, (end+start)/2);
                subTask1.fork();
                SubTask subTask2 = new SubTask((end+start)/2+1, end);
                subTask2.fork();
                return subTask1.join() + subTask2.join();
            } else {
                System.out.println(Thread.currentThread().getName() + " 开始计算 " + start + " - " + end + " 的值!");
                int res = 0;
                for (int i = start; i <= end; ++i) res += i;
                return res;
            }

        }

    }

}
