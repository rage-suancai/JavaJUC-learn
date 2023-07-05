package ThreadPool.TimedTask;

import java.util.concurrent.*;

public class Main {

    public static void main(String[] args) throws ExecutionException, InterruptedException {

        // test1();
        // test2();
        // test3();
        test4();

    }

    static void test1() {

        ScheduledThreadPoolExecutor pool = new ScheduledThreadPoolExecutor(1);
        pool.schedule(() -> System.out.println("Hello Java😪"), 3, TimeUnit.SECONDS);
        pool.shutdown();

    }

    static void test2() throws ExecutionException, InterruptedException {

        ScheduledThreadPoolExecutor pool = new ScheduledThreadPoolExecutor(2);

        ScheduledFuture<String> future = pool.schedule(() -> "????", 3, TimeUnit.SECONDS);
        System.out.println("任务剩余等待时间: " + future.getDelay(TimeUnit.MILLISECONDS) / 1000.0 + "s");
        System.out.println("任务执行结果: " + future.get());
        pool.shutdown();

    }

    static void test3() {

        ScheduledThreadPoolExecutor pool = new ScheduledThreadPoolExecutor(2);
        pool.scheduleAtFixedRate(() -> System.out.println("Hello Java😪"),
                3, 1,TimeUnit.SECONDS);

    }

    static void test4() {

        ScheduledExecutorService pool = Executors.newScheduledThreadPool(1);
        pool.schedule(() -> System.out.println("Hello Java😪"), 1,TimeUnit.SECONDS);
        pool.shutdown();

    }

}
