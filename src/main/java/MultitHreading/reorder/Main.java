package MultitHreading.reorder;

/**
 * 重排序
 * 在编译或执行时 为了优化程序的执行效率 编译器或处理器常常会对指令进行重排序 有以下情况:
 *
 *      > 编译器重排序: Java编译器通过对Java代码语义的理解 根据优化规则对代码指令进行重排序
 *      > 机器指令级别的重排序: 现代处理器很高级 能够自主判断和变更机器指令的执行顺序
 *
 * 指令重排序能够在不改变结果(单线程)的情况下 优化程序的运行效率 比如:
 *
 *                  public static void main(String[] args) {
 *
 *                      int a = 10;
 *                      int b = 20;
 *                      System.out.println(a+b);
 *
 *                  }
 *
 * 我们其实可以交换第一行和第二行:
 *
 *                  public static void main(String[] args) {
 *
 *                      int b = 10;
 *                      int a = 20;
 *                      System.out.println(a+b);
 *
 *                  }
 *
 * 即使发生交换 但是我们程序最后的运行结果是变不会变的 当然这里只通过代码的形式演示 实际上JVM在执行字节码指令时也会进行优化 可能两个指令并不会按照原有的顺序进行
 *
 * 虽然单线程下指令重排序确实可以起到一定程序的优化作用 但是在多线程下 似乎会导致一些问题:
 *
 *                  private static int a = 0;
 *                  private static int b = 0;
 *                  public static void main(String[] args) {
 *
 *                      new Thread(() -> {
 *                          if(b == 1) {
 *                              if(a == 0) {
 *                                  System.out.println("A");
 *                              } else {
 *                                  System.out.println("B");
 *                              }
 *                          }
 *                      }).start;
 *                      new Thread(() -> {
 *                          a = 1
 *                          b = 1;
 *                      }).start;
 *
 *                  }
 *
 * 上面这段代码 在正常情况下 按照我们的正常思维 是不可能输出A的 因为只要b等于1 那么a肯定也是1才对 因为a是在b之前完成的赋值 但是 如果进行了重排序 那么就有可能
 * a和b的赋值发生交换 先被赋值为1 而恰巧这个时候 线程1开始判定b是不是1了 这时a还没来得及被赋值为1 可能线程1就已经走到打印那里去了 所以 是有可能输出A的
 */
public class Main {

    private static int a = 0;
    private static int b = 0;

    public static void main(String[] args) {

        /*int a = 10;
        int b = 20;
        System.out.println(a+b);*/

        /*int b = 10;
        int a = 20;
        System.out.println(a+b);*/

        new Thread(() -> {
            if (b == 1) {
                if (a == 0) {
                    System.out.println("A");
                }else {
                    System.out.println("B");
                }
            }
        }).start();
        new Thread(() -> {
            a = 1;
            b = 1;
        }).start();

    }

}
