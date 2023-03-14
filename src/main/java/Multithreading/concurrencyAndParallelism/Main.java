package Multithreading.concurrencyAndParallelism;

/**
 * 并发与并行
 * 我们经常听到并非编程 那么这个并非代表的是上面意思呢? 而与之相似的并行又是什么意思? 它们之间有什么区别?
 *
 * 比如现在一共有三个工作需要我们去完成
 *
 *      https://img-blog.csdnimg.cn/img_convert/5c316fd6bae0032a5308970855a13c53.png
 *
 * 顺序执行
 * 顺序执行其实很好理解 就是我们依次去将这些任务完成了:
 *
 *      https://img-blog.csdnimg.cn/img_convert/520c77f92cdb0e75374e46d55ba937c5.png
 *
 * 实际上就是我们同一时间只能处理一共任务 所以需要前一个任务完成之后 才能继续下一个任务 依次完成所有任务
 *
 * 并发执行
 * 并发执行也是我们同一时间只能处理一共任务 但是我们可以每个任务轮着做(时间片轮转):
 *
 *      https://img-blog.csdnimg.cn/img_convert/c0667ee8f8bb693b414bb217d6280a79.png
 *
 * 只要我们单次处理分配的时间足够的短 在宏观看来 就是三个任务在同时进行
 *
 * 而我们Java中的线程 正是这种机制 当我们需要同时处理上百个上千个任务时 很明显CPU的数量是不可能赶得上我们的线程数的
 * 所以说这时就要求我们的程序有良好的并发性能 来应对同一时间大量的任务处理 学习Java并发编程 能够让我们在以后的实际场景中 知道该如何应对高并发的情况
 *
 * 并行执行
 * 并行执行就是突破了同一时间只能处理一个任务的限制 我们同一时间可以做多个任务:
 *
 *      https://img-blog.csdnimg.cn/img_convert/7761e6c071384c5fa9744c890d57f918.png
 *
 * 比如我们要进行一些排序操作 就可以用到并行计算 只需要等待所有子任务完成 最后将结果汇总即可 包括分布式计算模型MapReduce 也是采用的并行计算思路
 */
public class Main {

    public static void main(String[] args) {

        int[] arr = new int[] {3, 1, 5, 2, 4};

        for (int i : arr) {
            new Thread(() -> {
                try {
                    Thread.sleep(i * 1000);
                    System.out.print(i + " ");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }

    }

}
