package MultitHreading.jmm_MemoryModel;

/**
 * JMM内存模型
 * 注意: 这里提到的内存模型和我们在JVM中介绍的内存模型不在同一个层次 JVM中的内存模型是虚拟机规范对整个内存区域的规划
 * 而Java内存模型 是在JVM内存模式之上面的抽象模型 具体实现依然是基于JVM内存模型实现的 我们会在后面介绍
 *
 * Java内存模型
 * 我们在计算机组成原理中学习过 在我们的CPU中 一般都会有高速缓存 而它的出现 是为了解决内存的速度跟不上处理器的处理速度的问题
 * 所以CPU内部会添加一级或多级高速缓存来提高处理器的数据获取效率 但是这样也会导致一个很明显的问题 因为现在基本都是多核心处理器
 * 处理器都有一个自己的高速缓存 那么又该怎么去保证每个处理器的高速缓存内容一致呢?
 *
 *      https://img-blog.csdnimg.cn/img_convert/53625582b0f5b38222cfb8b88409be23.png
 *
 * 为了解决缓存一致性的问题 需要各个处理器访问缓存时都遵循一些协议 在读写时要根据协议来进行操作
 * 这类协议有MSI, MESI(Illonois Protocol), MOSI, Synapse, Firefly及Dragon Protocol等
 *
 * 而Java也采用了类似的模型来实现支持多线程的内存模型:
 *
 *      https://img-blog.csdnimg.cn/img_convert/a164c627ae1997cec19d7f7ad35966e4.png
 *
 * JMM(Java Memory Model)内存模型规定如下:
 *
 *      > 所有的变量全部存储在主内存 (注意这里包括下面提到的变量 指的都是会出现竞争的变量 包括成员变量 静态变量等 而局部变量这种属于线程私有 不包括在内)
 *      > 每条线程有着自己的工作内存(可以类比CPU的高速缓存)线程对变量的所有操作 必须在工作内存中进行 不能直接操作主内存中的数据
 *      > 不同线程之间的工作内存相互隔离 如果需要在线程之间传递内容 只能通过主内存完成 无法直接访问对方的工作内存
 *
 * 也就是说 每一条线程如果要操作主内存中的数据 那么得先拷贝到自己的工作内存中 并对工作内存中数据的副本进行操作
 * 操作完成之后 也需要从工作副本中将结果拷贝回主内存中 具体的操作就是Save(保存)和Load(加载)操作
 *
 * 那么各位肯定会好奇 这个内存模型 结合之前JVM所讲的内容 具体是怎么实现的呢?
 *
 *      > 主内存: 对应堆中存放对象实例的部分
 *      > 工作内存: 对应线程的虚拟机栈的部分区域 虚拟机可能会对这部分内存进行优化 将其放在CPU的寄存器或是高速缓存中 比如在访问数组时
 *                 由于数组是一段连续的内存空间 所以可以将一部分连续空间放入到CPU高速缓存中 那么之后如果我们顺序读取这个数组 那么大概率会直接缓存命中
 *
 * 前面我们提到 在CPU中可能遇到缓存不一致的问题 而Java中 也会遇到 比如下面这种情况:
 *
 *                  private static int i = 0;
 *                  public static void main(String[] args) throws InterruptedException {
 *
 *                      new Thread(() -> {
 *                          for (int j = 0; j < 100000; j++) i++;
 *                          System.out.println("线程1结束");
 *                      }).start();
 *                      new Thread(() -> {
 *                          for (int j = 0; j < 100000; j++) i++;
 *                          System.out.println("线程2结束");
 *                      }).start();
 *                      // 等上面两个线程结束
 *                      Thread.sleep(1000);
 *                      System.out.println(i);
 *
 *                  }
 *
 * 可以看到这里是两个线程同时对变量i各自进行100000次自增操作 但是实际得到的结果并不是我们所期望的那样
 *
 * 那么为什么会这样呢? 在之前学习了JVM之后 相信各位应该已经知道自增操作实际上并不是由一条指令完成的(注意: 一定不要理解为一行代码就是一个指令完成的):
 *
 *                   0 iconst_0
 *                   1 istore_0
 *                   2 iload_0
 *                   3 ldc #44 <100000>
 *                   5 if_icmpge 22 (+17)
 *                   8 getstatic #32 <javajuc6/Main.i : I>
 *                  11 iconst_1
 *                  12 iadd
 *                  13 putstatic #32 <javajuc6/Main.i : I>
 *                  16 iinc 0 by 1
 *                  19 goto 2 (-17)
 *                  22 getstatic #26 <java/lang/System.out : Ljava/io/PrintStream;>
 *                  25 ldc #45 <线程2结束>
 *                  27 invokevirtual #47 <java/io/PrintStream.println : (Ljava/lang/String;)V>
 *                  30 return
 *
 * 包括变量i的获取 修改 保存 都是被拆分为一个一个的操作完成的 那么这个时候就有可能出现在修改完保存之前 另一条线程也保存了 但是当前线程是毫不知情的:
 *
 *      https://img-blog.csdnimg.cn/img_convert/6df47c756cd7939a53a0a2b7ea75659d.png
 *
 * 所以说 我们当时在JavaSE阶段讲解这个问题的时候 是通过synchronized关键字添加同步代码块解决的 当然 我们后面还会讲解另外的解决方案(原子类)
 */
public class Main {

    private static int i = 0;
    public static void main(String[] args) throws InterruptedException {

        new Thread(() -> {
            for (int j = 0; j < 100000; j++) i++;
            System.out.println("线程1结束");
        }).start();
        new Thread(() -> {
            for (int j = 0; j < 100000; j++) i++;
            System.out.println("线程2结束");
        }).start();
        Thread.sleep(1000);
        System.out.println(i);

    }

}
