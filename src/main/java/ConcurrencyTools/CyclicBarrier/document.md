### 循环屏障 CyclicBarrier
好比一场游戏 我们必须等待房间内人数足够之后才能开始 并且游戏开始之后玩家需要同时进入游戏以保证公平性

假如现在游戏房间一共5人 但是游戏开始需要10人 所以我们必须等待剩下5个到来之后才能开始游戏 并且保证游戏开始时所有玩家都是同时进入
那么怎么实现这个功能呢? 我们可以使用CyclicBarrier 翻译过来就是循环屏障 那么这个屏障正是为了解决这个问题而出现的:

                    static void test() {

                        CyclicBarrier barrier = new CyclicBarrier(10, // 创建一共初始值为10的循环屏障
                                () -> System.out.println("飞机马上就要起飞了 各位特种兵请准备")); // 人等够之后执行的任务
                
                        for (int i = 0; i < 10; i++) {
                            int finalI = i;
                            new Thread(() -> {
                                try {
                                    Thread.sleep((long) (2000 * new Random().nextDouble()));
                                    System.out.println("玩家 " + finalI + " 进入房间进行等待... - " + barrier.getNumberWaiting() + "/10");
                                    barrier.await(); // 调用await方法进行等待 直到等待的线程足够多为止
                                    System.out.println("玩家 " + finalI + " 进入游戏"); // 开始游戏 所有玩家一起进入游戏
                                } catch (InterruptedException | BrokenBarrierException e) {
                                    throw new RuntimeException(e);
                                }
                            }).start();
                        }
                
                    }

可以看到 循环屏障会不断阻挡线程 直到被阻挡的线程足够多时 才能一起冲破屏障 并且在冲破屏障时 我们也可以做一些其他的任务
这和人多力量大的道理是差不多的当人足够多时方能冲破阻碍 到达美好的明天 当然 屏障由于是可循环的 所以它在被冲破后 会重新开始计数 继续阻挡后续的线程:

                    static void test() {

                        CyclicBarrier barrier = new CyclicBarrier(5); // 创建一个初始值为5的相循环屏障
                
                        for (int i = 0; i < 10; i++) { // 创建10个线程
                            int finalI = i;
                            new Thread(() -> {
                                try {
                                    Thread.sleep((long) (2000 * new Random().nextDouble()));
                                    System.out.println("玩家 " + finalI + " 进入房间进行等待... - " + barrier.getNumberWaiting() + "/5");
                                    barrier.await(); // 调用await方法进行等待 直到等待线程到达5才会一起继续执行
                                    System.out.println("玩家 " + finalI + " 进入游戏"); // 人数到齐之后 可以开始游戏了
                                } catch (InterruptedException | BrokenBarrierException e) {
                                    throw new RuntimeException(e);
                                }
                            }).start();
                        }
                
                    }

可以看到 通过使用循环屏障 我们可以对线程进行一波一波地放行 每一波都放行5个线程 当然除了自动重置之外 我们也可以调用reset()方法来手动进行重置操作 同样会重新计数:

                    tatic void test() {

                        CyclicBarrier barrier = new CyclicBarrier(5); // 创建一个初始值为5的计数器锁
                
                        for (int i = 0; i < 3; i++) {
                            new Thread(() -> {
                                try {
                                    barrier.await();
                                } catch (InterruptedException | BrokenBarrierException e) {
                                    throw new RuntimeException(e);
                                }
                            }).start();
                        }
                
                        try {
                            Thread.sleep(500); // 等一下上面的线程开始运行
                            System.out.println("当前屏障前的等待线程数: " + barrier.getNumberWaiting());
                            barrier.reset();
                            System.out.println("重置后屏障前的等待线程数: " + barrier.getNumberWaiting());
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                
                    }

可以看到 在调用reset()之后 处于等待状态下的线程 全部被中断并且抛出BrokenBarrierException异常
循环屏障等待线程数归零 那么要是处于等待状态下的线程被中断了呢? 屏障的线程等待数量会不会自动减少:

                    static void test() {

                        CyclicBarrier barrier = new CyclicBarrier(10);
                
                        Runnable r = () -> {
                            try {
                                barrier.await();
                            } catch (InterruptedException | BrokenBarrierException e) {
                                throw new RuntimeException(e);
                            }
                        };
                        Thread t = new Thread(r);
                        t.start(); t.interrupt();
                        new Thread(r).start();
                
                    }

可以看到 当await()状态下的线程被中断 那么屏障会直接变成损坏状态 一旦屏障损坏 那么这一轮就无法再做任何等待操作了 也就是说 本来大家计划一起合力冲破屏障 结果有一个摆烂中途退出了
那么所有人的努力都前功尽弃 这一轮的屏障也不可能再被冲破了(所以CyclicBarrier告诉我们 不要做那个害群之马 要相信你的团队 不然没有好果汁吃) 只能进行reset()重置操作进行重置才能恢复正常

乍一看 怎么感觉和之前讲的CountDownLatch有点像 好了 这里就得区分一下了 千万别搞混:
- CountDownLatch:
  - 它只能使用一次 是一个一次性的工具
  - 它是一个或多个线程用于等待其他线程完成的同步工具

- CyclicBarrier: 
  - 它可以反复使用 允许自动或手动重置计数
  - 它是让一定数量的线程在同一时间开始运行的同步工具

我们接着来看循环屏障的实现细节:

                    public class CyclicBarrier {
                        // 内部类 存放broken标记 表示屏障是否损坏 损坏的屏障是无法正常工作的
                        private static class Generation {
                            boolean broken = false;
                        }
                    
                        /** 内部维护一个可重入锁 */
                        private final ReentrantLock lock = new ReentrantLock();
                        /** 再维护一个Condition */
                        private final Condition trip = lock.newCondition();
                        /** 这个就是屏障的最大阻挡容量 就是构造方法传入的初始值 */
                        private final int parties;
                        /* 在屏障破裂时做的事情 */
                        private final Runnable barrierCommand;
                        /** 当前这一轮的Generation对象 每一轮都有一个新的 用于保存broken标记 */
                        private Generation generation = new Generation();
                    
                        // 默认为最大阻挡容量 每来一个线程-1 和CountDownLatch挺像 当屏障破裂或是被重置时 都会将其重置为最大阻挡容量
                        private int count;
                    
                        // 构造方法
                        public CyclicBarrier(int parties, Runnable barrierAction) {
                            if (parties <= 0) throw new IllegalArgumentException();
                            this.parties = parties;
                            this.count = parties;
                            this.barrierCommand = barrierAction;
                        }
                      
                        public CyclicBarrier(int parties) {
                            this(parties, null);
                        }
                      
                        // 开启下一轮屏障 一般屏障被冲破之后 就自动重置了 进入到下一轮
                        private void nextGeneration() {
                            // 唤醒所有等待状态的线程
                            trip.signalAll();
                            // 重置count的值
                            count = parties;
                            // 创建新的Generation对象
                            generation = new Generation();
                        }
                    
                        // 破坏当前屏障 变为损坏状态 之后就不能再使用了 除非重置
                        private void breakBarrier() {
                            generation.broken = true;
                            count = parties;
                            trip.signalAll();
                        }
                      
                          // 开始等待
                          public int await() throws InterruptedException, BrokenBarrierException {
                            try {
                                return dowait(false, 0L);
                            } catch (TimeoutException toe) {
                                throw new Error(toe); // 因为这里没有使用定时机制 不可能发生异常 如果发生怕是出了错误
                            }
                        }
                        
                        // 可超时的等待
                        public int await(long timeout, TimeUnit unit)
                            throws InterruptedException,
                                   BrokenBarrierException,
                                   TimeoutException {
                            return dowait(true, unit.toNanos(timeout));
                        }
                    
                        // 这里就是真正的等待流程了 让我们细细道来
                        private int dowait(boolean timed, long nanos)
                            throws InterruptedException, BrokenBarrierException,
                                   TimeoutException {
                            final ReentrantLock lock = this.lock;
                            lock.lock(); // 加锁 注意 因为多个线程都会调用await方法 因此只有一个线程能进 其他都被卡着了
                            try {
                                final Generation g = generation; // 获取当前这一轮屏障的Generation对象
                    
                                if (g.broken)
                                    throw new BrokenBarrierException(); // 如果这一轮屏障已经损坏 那就没办法使用了
                    
                                if (Thread.interrupted()) { // 如果当前等待状态的线程被中断 那么会直接破坏掉屏障 并抛出中断异常(破坏屏障的第1种情况)
                                    breakBarrier();
                                    throw new InterruptedException();
                                }
                    
                                int index = --count; // 如果上面都没有出现不正常 那么就走正常流程 首先count自减并赋值给index index表示当前是等待的第几个线程
                                if (index == 0) { // 如果自减之后就是0了 那么说明来的线程已经足够 可以冲破屏障了
                                    boolean ranAction = false;
                                    try {
                                        final Runnable command = barrierCommand;
                                        if (command != null)
                                            command.run(); // 执行冲破屏障后的任务 如果这里抛异常了 那么会进finally
                                        ranAction = true;
                                        nextGeneration(); // 一切正常 开启下一轮屏障(方法进入之后会唤醒所有等待的线程 这样所有的线程都可以同时继续运行了)然后返回0 注意最下面finally中会解锁 不然其他线程唤醒了也拿不到锁啊
                                        return 0;
                                    } finally {
                                        if (!ranAction) // 如果是上面出现异常进来的 那么也会直接破坏屏障(破坏屏障的第2种情况)
                                            breakBarrier();
                                    }
                                }
                    
                                // 能走到这里 那么说明当前等待的线程数还不够多 不足以冲破屏障
                                for (;;) { // 无限循环 一直等 等到能冲破屏障或是出现异常为止
                                    try {
                                        if (!timed)
                                            trip.await(); // 如果不是定时的 那么就直接永久等待
                                        else if (nanos > 0L)
                                            nanos = trip.awaitNanos(nanos); // 否则最多等一段时间
                                    } catch (InterruptedException ie) { // 等的时候会判断是否被中断(依然是破坏屏障的第1种情况)
                                        if (g == generation && ! g.broken) {
                                            breakBarrier();
                                            throw ie;
                                        } else {
                                            Thread.currentThread().interrupt();
                                        }
                                    }
                    
                                    if (g.broken)
                                        throw new BrokenBarrierException(); // 如果线程被唤醒之后发现屏障已经被破坏 那么直接抛异常
                    
                                    if (g != generation) // 成功冲破屏障开启下一轮 那么直接返回当前是第几个等待的线程
                                        return index;
                    
                                    if (timed && nanos <= 0L) { // 线程等待超时 也会破坏屏障(破坏屏障的第3种情况) 然后抛异常
                                        breakBarrier();
                                        throw new TimeoutException();
                                    }
                                }
                            } finally {
                                lock.unlock(); // 最后别忘了解锁 不然其他线程拿不到锁
                            }
                        }
                    
                        // 不多说了
                        public int getParties() {
                            return parties;
                        }
                    
                        // 判断是否被破坏 也是加锁访问 因为有可能这时有其他线程正在执行dowait
                        public boolean isBroken() {
                            final ReentrantLock lock = this.lock;
                            lock.lock();
                            try {
                                return generation.broken;
                            } finally {
                                lock.unlock();
                            }
                        }
                    
                        // 重置操作 也要加锁
                        public void reset() {
                            final ReentrantLock lock = this.lock;
                            lock.lock();
                            try {
                                breakBarrier(); // 先破坏这一轮的线程 注意这个方法会先破坏再唤醒所有等待的线程 那么所有等待的线程会直接抛BrokenBarrierException异常(详情请看上方dowait倒数第13行)
                                nextGeneration(); // 开启下一轮
                            } finally {
                                lock.unlock();
                            }
                        }
                        
                        // 获取等待线程数量 也要加锁
                        public int getNumberWaiting() {
                            final ReentrantLock lock = this.lock;
                            lock.lock();
                            try {
                                return parties - count; // 最大容量 - 当前剩余容量 = 正在等待线程数
                            } finally {
                                lock.unlock();
                            }
                        }
                    }

看完了CyclicBarrier的源码之后 是不是感觉比CountDownLatch更简单一些?