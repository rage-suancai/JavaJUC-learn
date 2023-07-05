## 并发工具类

### 计数器锁 CountDownLatch
多任务同步神器 它允许一个或多个线程 等待其他线程完成工作 比如现在我们有这样的一个需求:
- 有20个计算任务 我们需要先将这些任务的结果全部计算出来 每个任务的执行时间未知
- 当所有任务结束之后 立即整合统计最终结果

要实现这个需求 那么有一个很麻烦的地方 我们不知道任务到底什么时候执行完毕 那么可否将最终统计延迟一定时间进行呢?
但是最终统计无论延迟多久进行 要么不能保证所有任务都完成 要么可能所有任务都完成了而这里还在等

所以说 我们需要一个能够实现子任务同步的工具:

                    static void test() {

                        CountDownLatch latch = new CountDownLatch(20); // 创建一个初始值为10的计数器锁
                
                        for (int i = 0; i < 20; i++) {
                            int finalI = i;
                            new Thread(() -> {
                                try {
                                    Thread.sleep((long) (2000 * new Random().nextDouble()));
                                    System.out.println("子任务" + finalI + "执行完成");
                                } catch (InterruptedException e) {
                                    throw new RuntimeException(e);
                                }
                                latch.countDown(); // 每执行一次计数器都会-1
                            }).start();
                        }
                        try {
                            // 开始等待所有的线程完成 当计数器为0时 恢复运行
                            latch.await(); // 这个操作可以同时被多个线程执行 一起等待 这里只演示了一个
                            System.out.println("所有子任务都完成 任务完成");
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }

                        // 注意这个计数器只能使用一次 用完只能重新创一个 没有重置的说法
                
                    }

我们在调用await()方法之后 实际上就是一个等待计数器衰减为0的过程 而进行自减操作则由各个子线程来完成
当子线程完成工作后 那么就将计数器-1 所有的子线程完成之后 计数器为0 结束等待

那么它是如何实现的呢? 实现 原理非常简单:

                    public class CountDownLatch {
                        // 同样是通过内部类实现AbstractQueuedSynchronizer
                        private static final class Sync extends AbstractQueuedSynchronizer {
                            
                            Sync(int count) { // 这里直接使用AQS的state作为计数器(可见state能被玩出各种花样) 也就是说一开始就加了count把共享锁 当线程调用countdown时 就解一层锁
                                setState(count);
                            }
                    
                            int getCount() {
                                return getState();
                            }
                    
                            // 采用共享锁机制 因为可以被不同的线程countdown 所以实现的tryAcquireShared和tryReleaseShared
                            // 获取这把共享锁其实就是去等待state被其他线程减到0
                            protected int tryAcquireShared(int acquires) {
                                return (getState() == 0) ? 1 : -1;
                            }
                    
                            protected boolean tryReleaseShared(int releases) {
                                // 每次执行都会将state值-1 直到为0
                                for (;;) {
                                    int c = getState();
                                    if (c == 0)
                                        return false; // 如果已经是0了 那就false
                                    int nextc = c-1;
                                    if (compareAndSetState(c, nextc)) // CAS设置state值 失败直接下一轮循环
                                        return nextc == 0; // 返回c-1之后 是不是0 如果是那就true 否则false 也就是说只有刚好减到0的时候才会返回true
                                }
                            }
                        }
                    
                        private final Sync sync;
                    
                        public CountDownLatch(int count) {
                            if (count < 0) throw new IllegalArgumentException("count < 0"); // count那肯定不能小于0啊
                            this.sync = new Sync(count); // 构造Sync对象 将count作为state初始值
                        }
                    
                        // 通过acquireSharedInterruptibly方法获取共享锁 但是如果state不为0 那么会被持续阻塞 详细原理下面讲
                        public void await() throws InterruptedException {
                            sync.acquireSharedInterruptibly(1);
                        }
                    
                        // 同上 但是会超时
                        public boolean await(long timeout, TimeUnit unit)
                            throws InterruptedException {
                            return sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
                        }
                    
                        // countDown其实就是解锁一次
                        public void countDown() {
                            sync.releaseShared(1);
                        }
                    
                        // 获取当前的计数 也就是AQS中state的值
                        public long getCount() {
                            return sync.getCount();
                        }
                    
                        // 这个就不说了
                        public String toString() {
                            return super.toString() + "[Count = " + sync.getCount() + "]";
                        }
                    }

在深入讲解之前 我们先大致了解一下CountDownLatch的基本实现思路:
- 利用共享锁实现
- 在一开始的时候就是已经上了count层锁的状态 也就是state = count
- await()就是加共享锁 但是必须state为0才能加锁成功 否则按照AQS的机制 会进入等待队列阻塞 加锁成功后结束阻塞
- countDown()就是解1层锁 也就是靠这个方法一点一点把state的值减到0

由于我们前面只对独占锁进行了讲解 没有对共享锁进行讲解 这里还是稍微提一下它:

                    public final void acquireShared(int arg) {
                        if (tryAcquireShared(arg) < 0) // 上来就调用tryAcquireShared尝试以共享模式获取锁 小于0则失败 上面判断的是state==0返回1 否则-1 也就是说如果计数器不为0 那么这里会判断成功
                            doAcquireShared(arg); // 计数器不为0的时候 按照它的机制 那么会阻塞 所以我们来看看doAcquireShared中是怎么进行阻塞的
                    }

                    private void doAcquireShared(int arg) {
                        final Node node = addWaiter(Node.SHARED); // 向等待队列中添加一个新的共享模式结点
                        boolean failed = true;
                        try { 
                            boolean interrupted = false;
                            for (;;) { // 无限循环
                                final Node p = node.predecessor(); // 获取当前节点的前驱的结点
                                if (p == head) { // 如果p就是头结点 那么说明当前结点就是第一个等待节点
                                    int r = tryAcquireShared(arg); // 会再次尝试获取共享锁
                                    if (r >= 0) { // 要是获取成功
                                        setHeadAndPropagate(node, r); // 那么就将当前节点设定为新的头结点 并且会继续唤醒后继节点
                                        p.next = null; // help GC
                                        if (interrupted)
                                            selfInterrupt();
                                        failed = false;
                                        return;
                                    }
                                }
                                if (shouldParkAfterFailedAcquire(p, node) && // 和独占模式下一样的操作 这里不多说了
                                    parkAndCheckInterrupt())
                                    interrupted = true;
                            }
                        } finally {
                            if (failed)
                                cancelAcquire(node); // 如果最后都还是没获取到 那么就cancel
                        }
                    }
                    // 其实感觉大体上和独占模式的获取有点像 但是它多了个传播机制 会继续唤醒后续节点

                    private void setHeadAndPropagate(Node node, int propagate) {
                        Node h = head; // 取出头结点并将当前节点设定为新的头结点
                        setHead(node);
                        
                        // 因为一个线程成功获取到共享锁之后 有可能剩下的等待中的节点也有机会拿到共享锁
                        if (propagate > 0 || h == null || h.waitStatus < 0 ||
                            (h = head) == null || h.waitStatus < 0) { // 如果propagate大于0(表示共享锁还能继续获取)或是h.waitStatus < 0 这是由于在其他线程释放共享锁时 doReleaseShared会将状态设定为PROPAGATE表示可以传播唤醒 后面会讲
                            Node s = node.next;
                            if (s == null || s.isShared())
                                doReleaseShared(); // 继续唤醒下一个等待节点
                        }
                    }

我们接着来看 它的countdown过程:

                    public final boolean releaseShared(int arg) {
                        if (tryReleaseShared(arg)) { // 直接尝试释放锁 如果成功返回true(在CountDownLatch中只有state减到0的那一次 会返回true)
                            doReleaseShared(); // 这里也会调用doReleaseShared继续唤醒后面的结点
                            return true;
                        }
                        return false; // 其他情况false
                        // 不过这里countdown并没有用到这些返回值
                    }

                    private void doReleaseShared() {
                        for (;;) { // 无限循环
                            Node h = head; // 获取头结点
                            if (h != null && h != tail) { // 如果头结点不为空且头结点不是尾结点 那么说明等待队列中存在节点
                                int ws = h.waitStatus; // 取一下头结点的等待状态
                                if (ws == Node.SIGNAL) { // 如果是SIGNAL 那么就CAS将头结点的状态设定为初始值
                                    if (!compareAndSetWaitStatus(h, Node.SIGNAL, 0))
                                        continue; // 失败就开下一轮循环重来
                                    unparkSuccessor(h); // 和独占模式一样 当锁被释放 都会唤醒头结点的后继节点 doAcquireShared循环继续 如果成功 那么根据setHeadAndPropagate 又会继续调用当前方法 不断地传播下去 让后面的线程一个一个地获取到共享锁 直到不能再继续获取为止
                                }
                                else if (ws == 0 &&
                                         !compareAndSetWaitStatus(h, 0, Node.PROPAGATE)) // 如果等待状态是默认值0 那么说明后继节点已经被唤醒 直接将状态设定为PROPAGATE 它代表在后续获取资源的时候 够向后面传播
                                    continue; // 失败就开下一轮循环重来
                            }
                            if (h == head) // 如果头结点发生了变化 不会break 而是继续循环 否则直接break退出
                                break;
                        }
                    }

可能看完之后还是有点乱 我们再来理一下:
- 共享锁是线程共享的 同一时刻能有多个线程拥有共享锁
- 如果一个线程刚获取了共享锁 那么在其之后等待的线程也很有可能能够获取到锁 所以得传播下去继续尝试唤醒后面的结点 不像独占锁 独占的压根不需要考虑这些
- 如果一个线程刚释放了锁 不管是独占锁还是共享锁 都需要唤醒后续等待结点的线程

回到CountDownLatch 再结合整个AQS共享锁的实现机制 进行一次完整的推导 看明白还是比较简单的