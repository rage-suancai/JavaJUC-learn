package ThreadPool.ReturnValue;

import java.util.concurrent.*;

public class Main {

    public static void main(String[] args) throws ExecutionException, InterruptedException {

        // test1();
        // test2();
        // test3();
        // test4();
        test5();

    }

    static void test1() throws ExecutionException, InterruptedException {

        ExecutorService pool = Executors.newSingleThreadExecutor();

        Future<String> future = pool.submit(() -> "我是字符串");
        System.out.println(future.get());
        pool.shutdown();

    }

    static void test2() throws ExecutionException, InterruptedException {

        ExecutorService pool = Executors.newSingleThreadExecutor();

        Future<String> future = pool.submit(() -> {
            try {
                TimeUnit.SECONDS.sleep(3);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, "我是字符串");
        System.out.println(future.get());
        pool.shutdown();

    }

    static void test3() throws ExecutionException, InterruptedException {

        ExecutorService pool = Executors.newSingleThreadExecutor();

        FutureTask<String> task = new FutureTask<>(() -> "我是字符串");
        pool.submit(task);
        System.out.println(task.get());
        pool.shutdown();

    }

    static void test4() throws ExecutionException, InterruptedException {

        ExecutorService pool = Executors.newSingleThreadExecutor();

        Future<String> future = pool.submit(() -> "你好");
        System.out.println(future.get());
        System.out.println("任务是否执行完成: " + future.isDone());
        System.out.println("任务是否被取消: " + future.isCancelled());
        pool.shutdown();

    }

    static void test5() {

        ExecutorService pool = Executors.newSingleThreadExecutor();

        Future<String> future = pool.submit(() -> {
            TimeUnit.SECONDS.sleep(10);
            if (Thread.currentThread().isInterrupted()) {
                System.out.println("任务被取消"); return null;
            }
            return "下次一定";
        });
        boolean cancel = future.cancel(true);
        System.out.println(cancel);
        System.out.println(future.isCancelled());
        pool.shutdown();

    }

}
