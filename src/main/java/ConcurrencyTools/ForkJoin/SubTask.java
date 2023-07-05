package ConcurrencyTools.ForkJoin;

import java.util.concurrent.RecursiveTask;

public class SubTask extends RecursiveTask<Integer> {

    private final int start;
    private final int end;

    public SubTask(int start, int end) {
        this.start = start;
        this.end = end;
    }

    @Override
    protected Integer compute() {

        if (end - start > 125) {
            SubTask subTask1 = new SubTask(start, (end + start) / 2);
            subTask1.fork();
            SubTask subTask2 = new SubTask((end + start) / 2 + 1, end);
            subTask2.fork();
            return subTask1.join() + subTask2.join();
        } else {
            System.out.println(Thread.currentThread().getName() + " 开始计算 " + start + "-" + end + " 的值");
            int res = 0;
            for (int i = start; i <= end; i++) {
                res += i;
            }
            return res;
        }

    }

}
