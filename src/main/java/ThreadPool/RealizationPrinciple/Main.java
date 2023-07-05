package ThreadPool.RealizationPrinciple;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String[] args) {



    }

    static void test1() {

        ThreadPoolExecutor pool = new ThreadPoolExecutor(2, 4,
                3, TimeUnit.SECONDS, new ArrayBlockingQueue<>(2));

    }

}
