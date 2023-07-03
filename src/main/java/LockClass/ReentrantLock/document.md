### 可重入锁
前面 我们讲解了锁框架的两个核心接口 那么我们接着来看看锁接口的具体实现类 我们前面用到了ReentrantLock
它其实是锁的一种 叫做可重入锁 那么这个可重入代表的是什么意思呢? 简单来说 就是同一个线程 可以反复进行加锁操作:

                    static void test() {

                        ReentrantLock lock = new ReentrantLock();
                
                        lock.lock(); lock.lock(); // 连续加锁两次
                        new Thread(() -> {
                            System.out.println("线程二想要获取锁");
                            lock.lock();
                            System.out.println("线程二成功获取到锁");
                        }).start();
                
                        lock.unlock();
                        System.out.println("线程一释放了一次锁");
                        try {
                            TimeUnit.SECONDS.sleep(1);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        lock.unlock();
                        System.out.println("线程一再次释放了一次锁"); // 释放两次后其他线程才能加锁
                
                    }

可以看到 主线程连续进行了两次加锁操作(此操作是不会被阻塞的)在当前线程持有锁的情况下继续加锁不会被阻塞
并且 加锁几次 就必须要解锁几次否则此线程依旧持有锁 我们可以使用getHoldCount()方法查看当前线程的加锁次数:

                    static void test() throws InterruptedException {

                        ReentrantLock lock = new ReentrantLock();
                        lock.lock(); lock.lock();
                
                        System.out.println("当前加锁次数: " + lock.getHoldCount() + " 是否被锁: " + lock.isLocked());
                        TimeUnit.SECONDS.sleep(1);
                        lock.unlock();
                        System.out.println("当前加锁次数: " + lock.getHoldCount() + " 是否被锁: " + lock.isLocked());
                        TimeUnit.SECONDS.sleep(1);
                        lock.unlock();
                        System.out.println("当前加锁次数: " + lock.getHoldCount() + " 是否被锁: " + lock.isLocked());
                
                    }

可以看到 当锁不再被任何线程持有时 值为0 并且通过isLocked()方法查询结果为false

实际上 如果存在线程持有当前的锁 那么其他线程在获取锁时 是会暂时进入到等待队列的 我们可以通过getQueueLength()方法获取等待中线程数量的预估值:

                    static void test() throws InterruptedException {

                        ReentrantLock lock = new ReentrantLock();
                        lock.lock();
                
                        Thread t1 = new Thread(lock::lock), t2 = new Thread(lock::lock);
                        t1.start(); t2.start();
                        TimeUnit.SECONDS.sleep(1);
                        System.out.println("当前等待锁释放的线程数: " + lock.getQueueLength());
                        System.out.println("线程一等待线程数: " + lock.hasQueuedThread(t1));
                        System.out.println("线程二等待线程数: " + lock.hasQueuedThread(t2));
                        System.out.println("当前线程是否在等待队列中: " + lock.hasQueuedThread(Thread.currentThread()));
                
                    }

我们可以通过hasQueuedThread()方法来判断某个线程是否正在等待获取锁状态

同样的 Condition也可以进行判断:

                    static void test() throws InterruptedException {

                        ReentrantLock lock = new ReentrantLock();
                        Condition condition = lock.newCondition();
                
                        new Thread(() -> {
                            lock.lock();
                            try {
                                condition.await();
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                            lock.unlock();
                        }).start();
                        TimeUnit.SECONDS.sleep(1);
                        lock.lock();
                        System.out.println("当前Condition的等待线程数: " + lock.getWaitQueueLength(condition));
                        condition.signal();
                        System.out.println("当前Condition的等待线程数: " + lock.getWaitQueueLength(condition));
                        lock.unlock();
                
                    }

通过使用getWaitQueueLength()方法能够查看同一个Condition目前有多少线程处于等待状态

### 公平锁与非公平锁
前面我们了解了如果线程之间争抢同一把锁 会暂时进入到等待队列中 那么多个线程获得锁的顺序是不是一定是根据线程调用lock()方法时间来定的呢
我们可以看到 ReentrantLock的构造方法中 是这样写的

                    public ReentrantLock() {
                        sync = new NonfairSync(); // 看名字貌似是非公平的
                    }

其实锁分为公平锁和非公平锁 默认我们创建出来的ReentrantLock是采用的非公平锁作为底层锁机制 那么什么是公平锁什么又是非公平锁呢?
- 公平锁: 多个线程按照申请锁的顺序去获得锁 线程会直接进入队列去排队 永远都是队列的第一位才能得到锁
- 非公平锁: 多个线程去获取锁的时候 会直接去尝试获取 获取不到 再去进入等待队列 如果能获取到 就直接获取到锁

简单来说 公平锁不让插队 都老老实实排着 非公平锁让插队 但是排队的人让不让你插队就是另一回事了

我们可以来测试一下公平锁和非公平锁的表现情况:

                    public ReentrantLock(boolean fair) {
                        sync = fair ? new FairSync() : new NonfairSync();
                    }

这里我们选择使用第二个构造方法 可以选择是否为公平锁实现:

                    static void test() {

                        ReentrantLock lock = new ReentrantLock(false);
                
                        Runnable action = () -> {
                            System.out.println("线程 " + Thread.currentThread().getName() + " 开始获取锁..."); lock.lock();
                            System.out.println("线程 " + Thread.currentThread().getName() + " 成功获取锁"); lock.unlock();
                        };
                
                        for (int i = 0; i < 10; i++) { // 建立10个线程
                            new Thread(action, "T" + i).start();
                        }
                
                    }

这里我们只需要对比将在1秒后开始获取锁...和成功获取锁的顺序是否一致即可 如果是一致 那说明所有的线程都是按顺序排队获取的锁 如果不是 那说明肯定是有线程插队了

运行结果可以发现 在公平模式下 确实是按照顺序进行的 而在非公平模式下 一般会出现这种情况: 线程刚开始获取锁马上就能抢到 并且此时之前早就开始的线程还在等待状态 很明显的插队行为

那么 接着下一个问题 公平锁在任何情况下都一定是公平的吗? 有关这个问题 我们会留到队列同步器中再进行讨论