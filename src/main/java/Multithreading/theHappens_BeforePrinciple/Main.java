package Multithreading.theHappens_BeforePrinciple;

/**
 * happens-before原则
 * 经过我们前面的讲解 相信各位已经了解了JMM内存模型以及重排序等机制带来的优点和缺点 综上 JMM提出了happens-before(先行发生)原则
 * 定义一些禁止编译优化的场景 来向各位程序员做一些保证 只要我们是按照原则进行编程 那么就能够保持并发编程的正确性 具体如下:
 *
 *      > 程序次序规则: 同一个线程中 按照程序的顺序 前面的操作happens-before后续的任何操作
 *
 *          > 同一个线程内 代码的执行结果是有序的 其实就是 可能会发生指令重排 但是保证代码的执行结果一定是和按照顺序执行得到的一致
 *            程序前面对某一个变量的修改一定对后续操作可见的 不可能会出现前面才把a修改为1 接着读a居然是修改前面的结果 这也是程序运行最基本的要求
 *
 *      > 监视器锁规则: 对一个锁的解锁操作 happens-before后续对这个锁加锁操作
 *
 *          > 就是无论是在单线程环境还是多线程环境 对于同一个锁来说 一个线程对这个锁解锁之后 另一个线程获取了这个锁都能看到前一个线程的操作结果
 *            比如前一个线程将变量x的值修改为了12并解锁 之后另一个线程拿到了这把锁 对之前线程的操作是可见的 可以得到x是前一个线程修改后的结果12(所以synchronized是有happens-before规则的)
 *
 *      > volatile变量规则: 对一个volatile变量的写操作happens-before后续对这个变量的读操作
 *
 *          > 就是如果一个线程先去写一个volatile变量 紧接着另一个线程去读这个变量 那么这个写操作的结果一定对读的这个变量的线程可见
 *
 *      > 线程启动规则: 主线程A启动线程B 线程B中可以看到主线程启动B之前的操作
 *
 *          > 在主线程A执行过程泽 启动子线程B 那么线程A在启动子线程B之前对共享变量的修改结果对线程B可见
 *
 *      > 线程加入规则: 如果线程A执行操作join()线程B并成功返回 那么线程B中的任意操作happens-before线程A join()操作成功返回
 *
 *      > 传递性规则: 如果A happens-before B, B happens-before C 那么A happens-before C
 *
 * 那么我们来从happens-before原则的角度 来解释一下下面的程序结果:
 *
 *                  private static int a = 0;
 *                  private static int b = 0;
 *
 *                  public static void main(String[] args) {
 *
 *                      a = 10;
 *                      b = a + 1;
 *                      new Thread(() -> {
 *                          if (b > 10) System.out.println(a);
 *                      }).start();
 *
 *                  }
 *
 * 首先我们定义以上出现的操作:
 *
 *      > A: 将变量a的值修改为 10
 *      > B: 将变量b的值修改为 a + 1
 *      > C: 主线程启动了一个新的线程 并在新的线程中获取b 进行判断 如果为true那么就打印a
 *
 * 首先我们来分析 由于是同一个线程 并且B是一个赋值操作且读取了A 那么按照程序次序规则 A happens-before B 接着在B之后 马上执行了C 按照线程启动规则 在新的线程启动之前
 * 当前线程之前的所有操作对新的线程是可见的 所以B happens-before C 最后根据传递性规则 由于A happens-before B, B happens-before C 所以A happens-before C 因此在新的线程中会输出a修改后的结果10
 */
public class Main {

    private static int a = 0;
    private static int b = 0;

    public static void main(String[] args) {

        a = 10;
        b = a + 1;
        new Thread(() -> {
            if (b > 10) System.out.println(a);
        }).start();

    }

}
