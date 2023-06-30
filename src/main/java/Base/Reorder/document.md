### 重排序
在编译或执行时 为了优化程序的执行效率 编译器或处理器常常会对指令进行重排序 有以下情况:
1. 编译器重排序: Java编译器通过对Java代码语义的理解 根据优化规则对代码指令进行重排序
2. 机器指令级别的重排序: 现代处理器很高级 能够自主判断和变更机器指令的执行顺序

指令重排序能够在不改变结果(单线程)的情况下 优化程序的运行效率 比如:

                    static void test() {

                        int a = 10; int b = 20;
                        System.out.println(a + b);
                
                    }

我们其实可以交换第一行和第二行:

                    static void test() {

                        int b = 10; int a = 20;
                        System.out.println(a + b);
                
                    }

即使发生交换 但是我们程序最后的运行结果是不会变的 当然这里只通过代码的形式演示 实际上JVM在执行字节码指令时也会进行优化 可能两个指令并不会按照原有的顺序进行

虽然单线程下指令重排确实可以起到一定程度的优化作用 但是在多线程下 似乎会导致一些问题:
                    
                    private static int a = 0;
                    private static int b = 0;
                    static void test() {
        
                        new Thread(() -> {
                            if (b == 1) {
                                if (a == 0) System.out.println("A");
                                else System.out.println("B");
                            }
                        }).start();
                        
                        new Thread(() -> {
                            a = 1; b = 1;
                        }).start();
                        
                    }

上面这段代码 在正常情况下 按照我们的正常思维 是不可能输出A的 因为只要b等于1 那么a肯定也是1才对 因为a是在b之前完成的赋值 但是 如果进行了重排序 那么就有可能
a和b的赋值发生交换 b先被赋值为1 而恰巧这个时候 线程1开始判定b是不是1了 这时a还没来得及被赋值为1 可能线程1就已经走到打印那里去了 所以 是有可能输出A的

### volatile关键字
好久好久都没有认识新的关键字了 今天我们来看一个新的关键字volatile 开始之前我们先介绍三个词语:
- 原子性: 其实之前讲过很多次了 就是要做什么事情要么做完 要么就不做 不存在做一半的情况
- 可见性: 指当多个线程访问同一个变量时 一个线程修改了这个变量的值 其他线程能够立即看得到修改的值
- 有序性: 即程序执行的顺序按照代码的先后顺序执行

我们之前说了 如果多线程访问同一个变量 那么这个变量会被线程拷贝到自己的工作内存中进行操作 而不是直接对主内存中的变量本体进行操作 下面这个操作看起来是一个有限循环 但是是无限的:

                    private static int a = 0;
                    static void test() {

                        new Thread(() -> {
                            while (a == 0);
                            System.out.println("线程结束");
                        }).start();
                
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        System.out.println("正在修改a的值...");
                        a = 1; // 很明显 按照我们的逻辑来说 a的值被修改那么另一个线程将不再循环
                
                    }

实际上这就是我们之前说的 虽然我们主线程中修改了a的值 但是另一个线程不知道a的值发生了改变 所以循环中依然是使用旧值在进行判断 因此 普通变量是不具有可见性的

要解决这种问题 我们第一个想到的肯定是加锁 同一时间只能由一个线程使用 这样总行了吧 确实 这样的话肯定是可以解决问题的:

                    private static int a = 0;
                    static void test() {

                        new Thread(() -> {
                            while (a == 0) synchronized (Main.class) {}
                            System.out.println("线程结束");
                        }).start();
                
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        System.out.println("正在修改a的值...");
                        synchronized (Main.class) {
                            a = 1;
                        }
                
                    }

但是 除了硬加一把锁的方案 我们也可以使用volatile关键字来解决 此关键字的第一个作用 就是保证变量的可见性 当写一个volatile变量时
JMM会把该线程本地内存中的变量强制刷新到主内存中去 并且这个写入操作会导致其他线程中的volatile变量缓存无效
这样 另一个线程修改了这个变时 当前线程会立即得知 并将工作内存中的变量更新为最新的版本

那么我们就来试试看:

                    private static volatile int a = 0;
                    static void test() {

                        new Thread(() -> {
                            while (a == 0);
                            System.out.println("线程结束");
                        }).start();
                
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        System.out.println("正在修改a的值...");
                        a = 1;
                
                    }

结果还真的如我们所说的那样 当a发生改变时 循环立即结束

当然 虽然说volatile能够保证可见性 但是不能保证原子性 要解决我们上面的i++的问题 以我们目前所学的知识 还是只能使用加锁来完成:
                    
                    private static volatile int a = 0;
                    static void test() {

                        Runnable r = () -> {
                          for (int i = 0; i < 10000; i++) a++;
                          System.out.println("任务完成");
                        };
                        new Thread(r).start();
                        new Thread(r).start();
                
                        try {
                            Thread.sleep(1000); // 等待线程执行完成
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        System.out.println(a);
                
                    }

不对啊 volatile不是能在改变变量的时候其他线程可见吗 那为什么还是不能保证原子性呢? 还是那句话 自增操作是被瓜分为了多个步骤完成的 虽然保证了可见性 但是只要手速够快
依然会出现两个线程同时写同一个值的问题(比如线程1刚刚将a的值更新为100 这时线程2可能也已经执行到更新a的值这条指令了 已经刹不住车了 所以依然会将a的值再更新为一次100)

那要是真的遇到这种情况 那么我们不可能都去写个锁吧? 后面 我们会介绍原子类来专门解决这种问题

最后一个功能就是volatile会禁止指令重排 也就是说 如果我们操作的是一个volatile变量 它将不会出现重排序的情况 也就解决了我们最上面的问题
那么它是怎么解决的重排序问题呢? 若用volatile修饰共享变量 在编译时 会在指令序列中插入内存屏障来禁止特定类型的处理器重排序

    内存屏障(Memory Barrier)又称内存栅栏 是一个CPU指令 它的作用有两个:
        1. 保证特定操作的顺序
        2. 保证某些变量的内存可见性(volatile的内存可见性 其实就是依靠这个实现的)
    由于编译器和处理器都能执行指令重排的优化 如果在指令间插入一条Memory Barrier则会告诉编译器和CPU 不管什么指令都不能和这条Memory Barrier指令重排序

<img src="https://fast.itbaima.net/2023/03/06/upc28A41DwCBNO9.png"/>

    屏障类型	                   指令示例	                                      说明
    LoadLoad	       Load1;LoadLoad;Load2	              保证Load1的读取操作在Load2及后续读取操作之前执行
    StoreStore	       Store1;StoreStore;Store2	          在Store2及其后的写操作执行前 保证Store1的写操作已刷新到主内存
    LoadStore	       Load1;LoadStore;Store2	          在Store2及其后的写操作执行前 保证Load1的读操作已读取结束
    StoreLoad	       Store1;StoreLoad;Load2	          保证load1的写操作已刷新到主内存之后 load2及其后的读操作才能执行

所以volatile能够保证 之前的指令一定全部执行 之后的指令一定都没有执行 并且前面语句的结果对后面的语句可见

最后我们来总结一下volatile关键字的三个特性:
- 保证可见性
- 不保证原子性
- 防止指令重排

在之后我们的设计模式系列中 还会讲解单例模式下volatile的运用