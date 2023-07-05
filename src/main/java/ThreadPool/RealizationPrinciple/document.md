### 线程池实现原理
前面我们了解了线程池的使用 那么接着我们来看看它的详细实现过程 结构稍微有点复杂 坐稳 发车了

这里需要首先介绍一下ctl变量:

                    // 这个变量比较关键 用到了原子AtomicInteger 用于同时保存线程池运行状态和线程数量(使用原子类是为了保证原子性)
                    // 它是通过拆分32个bit位来保存数据的 前3位保存状态 后29位保存工作线程数量(那要是工作线程数量29位装不下不就GG?)
                    private final AtomicInteger ctl = new AtomicInteger(ctlOf(RUNNING, 0));
                    private static final int COUNT_BITS = Integer.SIZE - 3; // 29位 线程数量位
                    private static final int CAPACITY   = (1 << COUNT_BITS) - 1; // 计算得出最大容量(1左移29位 最大容量为2的29次方-1)
                    
                    // 所有的运行状态 注意都是只占用前3位 不会占用后29位
                    // 接收新任务 并等待执行队列中的任务
                    private static final int RUNNING    = -1 << COUNT_BITS; // 111 | 0000... (后29数量位 下同)
                    // 不接收新任务 但是依然等待执行队列中的任务
                    private static final int SHUTDOWN   =  0 << COUNT_BITS; // 000 | 数量位
                    // 不接收新任务 也不执行队列中的任务 并且还要中断正在执行中的任务
                    private static final int STOP       =  1 << COUNT_BITS; // 001 | 数量位
                    // 所有的任务都已结束 线程数量为0 即将完全关闭
                    private static final int TIDYING    =  2 << COUNT_BITS; // 010 | 数量位
                    // 完全关闭
                    private static final int TERMINATED =  3 << COUNT_BITS; // 011 | 数量位
                    
                    // 封装和解析ctl变量的一些方法
                    private static int runStateOf(int c)     { return c & ~CAPACITY; } // 对CAPACITY取反就是后29位全部为0 前三位全部为1 接着与c进行与运算 这样就可以只得到前三位的结果了 所以这里是取运行状态
                    private static int workerCountOf(int c)  { return c & CAPACITY; }
                    // 同上 这里是为了得到后29位的结果 所以这里是取线程数量
                    private static int ctlOf(int rs, int wc) { return rs | wc; }   
                    // 比如上面的RUNNING, 0 进行与运算之后:
                    // 111 | 0000000000000000000000000

<img src="https://fast.itbaima.net/2023/03/06/yoFvxQJq84G6dcH.png">

我们先从最简单的入手 看看在调用execute方法之后 线程池会做什么:

                    // 这个就是我们指定的阻塞队列
                    private final BlockingQueue<Runnable> workQueue;
                    
                    // 再次提醒 这里没加锁!! 该有什么意识不用我说了吧 所以说ctl才会使用原子类
                    public void execute(Runnable command) {
                        if (command == null)
                            throw new NullPointerException(); // 如果任务为null 那执行个寂寞 所以说直接空指针
                        int c = ctl.get(); // 获取ctl的值 一会要读取信息的
                        if (workerCountOf(c) < corePoolSize) { // 判断工作线程数量是否小于核心线程数
                            if (addWorker(command, true)) // 如果是 那不管三七二十一 直接加新的线程执行 然后返回即可
                                return;
                            c = ctl.get(); // 如果线程添加失败(有可能其他线程也在对线程池进行操作) 那就更新一下c的值
                        }   
                        if (isRunning(c) && workQueue.offer(command)) { // 继续判断 如果当前线程池是运行状态 那就尝试向阻塞队列中添加一个新的等待任务
                            int recheck = ctl.get(); // 再次获取ctl的值
                            if (! isRunning(recheck) && remove(command)) // 这里是再次确认当前线程池是否关闭 如果添加等待任务后线程池关闭了 那就把刚刚加进去任务的又拿出来
                                reject(command); // 然后直接拒绝当前任务的提交(会根据我们的拒绝策略决定如何进行拒绝操作)
                            else if (workerCountOf(recheck) == 0) // 如果这个时候线程池依然在运行状态 那么就检查一下当前工作线程数是否为0 如果是那就直接添加新线程执行
                                addWorker(null, false); // 添加一个新的非核心线程 但是注意没添加任务
                                // 其他情况就啥也不用做了
                        }
                        else if (!addWorker(command, false)) // 这种情况要么就是线程池没有运行 要么就是队列满了 按照我们之前的规则 核心线程数已满且队列已满 那么会直接添加新的非核心线程 但是如果已经添加到最大数量 这里肯定是会失败的
                            reject(command);   // 确实装不下了 只能拒绝
                    }

是不是感觉思路还挺清晰的 我们接着来看addWorker是怎么创建和执行任务的 又是一大堆代码:

                    private boolean addWorker(Runnable firstTask, boolean core) {
                        // 这里给最外层循环打了个标签 方便一会的跳转操作
                        retry:
                        for (;;) { // 无限循环 老套路了 注意这里全程没加锁
                            int c = ctl.get(); // 获取ctl值
                            int rs = runStateOf(c); // 解析当前的运行状态
                    
                            // Check if queue empty only if necessary.
                            if (rs >= SHUTDOWN && // 判断线程池是否不是处于运行状态
                                ! (rs == SHUTDOWN && // 如果不是运行状态 判断线程是SHUTDOWN状态并、任务不为null、等待队列不为空 只要有其中一者不满足 直接返回false 添加失败
                                   firstTask == null &&   
                                   ! workQueue.isEmpty()))
                                return false;
                    
                            for (;;) { // 内层又一轮无限循环 这个循环是为了将线程计数增加 然后才可以真正地添加一个新的线程
                                int wc = workerCountOf(c); // 解析当前的工作线程数量
                                if (wc >= CAPACITY ||
                                    wc >= (core ? corePoolSize : maximumPoolSize)) // 判断一下还装得下不 如果装得下 看看是核心线程还是非核心线程 如果是核心线程 不能大于核心线程数的限制 如果是非核心线程 不能大于最大线程数限制
                                    return false;
                                if (compareAndIncrementWorkerCount(c)) // CAS自增线程计数 如果增加成功 任务完成 直接跳出继续
                                    break retry; // 注意这里要直接跳出最外层循环 所以用到了标签(类似于goto语句)
                                c = ctl.get(); // 如果CAS失败 更新一下c的值
                                if (runStateOf(c) != rs) // 如果CAS失败的原因是因为线程池状态和一开始的不一样了 那么就重新从外层循环再来一次
                                    continue retry; // 注意这里要直接从最外层循环继续 所以用到了标签(类似于goto语句)
                                // 如果是其他原因导致的CAS失败 那只可能是其他线程同时在自增 所以重新再来一次内层循环
                            }
                        }
                    
                        // 好了 线程计数自增也完了 接着就是添加新的工作线程了
                        boolean workerStarted = false; // 工作线程是否已启动
                        boolean workerAdded = false; // 工作线程是否已添加
                        Worker w = null; // 暂时理解为工作线程 别急 我们之后会解读Worker类
                        try {
                            w = new Worker(firstTask); // 创建新的工作线程 传入我们提交的任务
                            final Thread t = w.thread; // 拿到工作线程中封装的Thread对象
                            if (t != null) { // 如果线程不为null 那就可以安排干活了
                                final ReentrantLock mainLock = this.mainLock; // 又是ReentrantLock加锁环节 这里开始就是只有一个线程能进入了
                                mainLock.lock();
                                try {
                                    // Recheck while holding lock.
                                    // Back out on ThreadFactory failure or if
                                    // shut down before lock acquired.
                                    int rs = runStateOf(ctl.get()); // 获取当前线程的运行状态
                    
                                    if (rs < SHUTDOWN ||
                                        (rs == SHUTDOWN && firstTask == null)) { // 只有当前线程池是正在运行状态 或是SHUTDOWN状态且firstTask为空 那么就继续
                                        if (t.isAlive()) // 检查一下线程是否正在运行状态
                                            throw new IllegalThreadStateException(); // 如果是那肯定是不能运行我们的任务的
                                        workers.add(w); // 直接将新创建的Work丢进 workers 集合中
                                        int s = workers.size(); // 看看当前workers的大小
                                        if (s > largestPoolSize) // 这里是记录线程池运行以来 历史上的最多线程数
                                            largestPoolSize = s;
                                        workerAdded = true; // 工作线程已添加
                                    }
                                } finally {
                                    mainLock.unlock(); // 解锁
                                }
                                if (workerAdded) {
                                    t.start(); // 启动线程
                                    workerStarted = true; // 工作线程已启动
                                }
                            }
                        } finally {
                            if (! workerStarted) // 如果线程在上面的启动过程中失败了
                                addWorkerFailed(w); // 将w移出workers并将计数器-1 最后如果线程池是终止状态 会尝试加速终止线程池
                        }
                        return workerStarted; // 返回是否成功
                    }

接着我们来看Worker类是如何实现的 它继承自AbstractQueuedSynchronizer 时隔两章 居然再次遇到AQS 那也就是说 它本身就是一把锁:

                    private final class Worker
                        extends AbstractQueuedSynchronizer
                        implements Runnable {
                        // 用来干活的线程
                        final Thread thread;
                        // 要执行的第一个任务 构造时就确定了的
                        Runnable firstTask;
                        // 干活数量计数器 也就是这个线程完成了多少个任务
                        volatile long completedTasks;
                    
                        Worker(Runnable firstTask) {
                            setState(-1); // 执行Task之前不让中断 将AQS的state设定为-1
                            this.firstTask = firstTask;
                            this.thread = getThreadFactory().newThread(this); // 通过预定义或是我们自定义的线程工厂创建线程
                        }
                      
                        public void run() {
                            runWorker(this); // 真正开始干活 包括当前活干完了又要等新的活来 就从这里开始 一会详细介绍
                        }
                    
                        // 0就是没加锁 1就是已加锁
                        protected boolean isHeldExclusively() {
                            return getState() != 0;
                        }
                    
                        ...
                    }

最后我们来看看一个Worker到底是怎么在进行任务的:

                    final void runWorker(Worker w) {
                        Thread wt = Thread.currentThread(); // 获取当前线程
                        Runnable task = w.firstTask; // 取出要执行的任务
                        w.firstTask = null; // 然后把Worker中的任务设定为null
                        w.unlock(); // 因为一开始为-1 这里是通过unlock操作将其修改回0 只有state大于等于0才能响应中断
                        boolean completedAbruptly = true;
                        try {
                            // 只要任务不为null 或是任务为空但是可以从等待队列中取出任务不为空 那么就开始执行这个任务 注意这里是无限循环 也就是说如果当前没有任务了 那么会在getTask方法中卡住 因为要从阻塞队列中等着取任务
                            while (task != null || (task = getTask()) != null) {
                                w.lock(); // 对当前Worker加锁 这里其实并不是防其他线程 而是在shutdown时保护此任务的运行
                                
                                // 由于线程池在STOP状态及以上会禁止新线程加入并且中断正在进行的线程
                                if ((runStateAtLeast(ctl.get(), STOP) || // 只要线程池是STOP及以上的状态 那肯定是不能开始新任务的
                                     (Thread.interrupted() && // 线程是否已经被打上中断标记并且线程一定是STOP及以上
                                      runStateAtLeast(ctl.get(), STOP))) &&
                                    !wt.isInterrupted()) // 再次确保线程被没有打上中断标记
                                    wt.interrupt(); // 打中断标记
                                try {
                                    beforeExecute(wt, task); // 开始之前的准备工作 这里暂时没有实现
                                    Throwable thrown = null;
                                    try {
                                        task.run(); // OK 开始执行任务
                                    } catch (RuntimeException x) {
                                        thrown = x; throw x;
                                    } catch (Error x) {
                                        thrown = x; throw x;
                                    } catch (Throwable x) {
                                        thrown = x; throw new Error(x);
                                    } finally {
                                        afterExecute(task, thrown); // 执行之后的工作 也没实现
                                    }
                                } finally {
                                    task = null; // 任务已完成 不需要了
                                    w.completedTasks++; // 任务完成数++
                                    w.unlock(); // 解锁
                                }
                            }
                            completedAbruptly = false;
                        } finally {
                            // 如果能走到这一步 那说明上面的循环肯定是跳出了 也就是说这个Worker可以丢弃了
                            // 所以这里会直接将 Worker 从 workers 里删除掉
                            processWorkerExit(w, completedAbruptly);
                        }
                    }

那么它是怎么从阻塞队列里面获取任务的呢:

                    private Runnable getTask() {
                        boolean timedOut = false; // Did the last poll() time out?
                    
                        for (;;) { // 无限循环获取
                            int c = ctl.get(); // 获取ctl 
                            int rs = runStateOf(c); // 解析线程池运行状态
                    
                            // Check if queue empty only if necessary.
                            if (rs >= SHUTDOWN && (rs >= STOP || workQueue.isEmpty())) { // 判断是不是没有必要再执行等待队列中的任务了 也就是处于关闭线程池的状态了
                                decrementWorkerCount(); // 直接减少一个工作线程数量
                                return null; // 返回null 这样上面的runWorker就直接结束了 下同
                            }
                    
                            int wc = workerCountOf(c); // 如果线程池运行正常 那就获取当前的工作线程数量
                    
                            // Are workers subject to culling?
                            boolean timed = allowCoreThreadTimeOut || wc > corePoolSize; // 如果线程数大于核心线程数或是允许核心线程等待超时 那么就标记为可超时的
                    
                            // 超时或maximumPoolSize在运行期间被修改了 并且线程数大于1或等待队列为空 那也是不能获取到任务的
                            if ((wc > maximumPoolSize || (timed && timedOut))
                                && (wc > 1 || workQueue.isEmpty())) {
                                if (compareAndDecrementWorkerCount(c)) // 如果CAS减少工作线程成功
                                    return null; // 返回null
                                continue; // 否则开下一轮循环
                            }
                    
                            try {
                                Runnable r = timed ?
                                    workQueue.poll(keepAliveTime, TimeUnit.NANOSECONDS) : // 如果可超时 那么最多等到超时时间
                                    workQueue.take(); // 如果不可超时 那就一直等着拿任务
                                if (r != null) // 如果成功拿到任务 ok 返回
                                    return r;
                                timedOut = true; // 否则就是超时了 下一轮循环将直接返回null
                            } catch (InterruptedException retry) {
                                timedOut = false;
                            }
                            // 开下一轮循环吧
                        }
                    }

虽然我们的源码解读越来越深 但是只要各位的思路不断 依然是可以继续往下看的 到此 有关execute()方法的源码解读 就先到这里

接着我们来看当线程池关闭时会做什么事情:

                    // 普通的shutdown会继续将等待队列中的线程执行完成后再关闭线程池
                    public void shutdown() {
                        final ReentrantLock mainLock = this.mainLock;
                        mainLock.lock();
                        try {
                            // 判断是否有权限终止
                            checkShutdownAccess();
                            // CAS将线程池运行状态改为SHUTDOWN状态 还算比较温柔 详细过程看下面
                            advanceRunState(SHUTDOWN);
                            // 让闲着的线程(比如正在等新的任务)中断 但是并不会影响正在运行的线程 详细过程请看下面
                            interruptIdleWorkers();
                            onShutdown(); // 给ScheduledThreadPoolExecutor提供的钩子方法 就是等ScheduledThreadPoolExecutor去实现的 当前类没有实现
                        } finally {
                            mainLock.unlock();
                        }
                        tryTerminate(); // 最后尝试终止线程池
                    }

                    private void advanceRunState(int targetState) {
                        for (;;) {
                            int c = ctl.get(); // 获取ctl
                            if (runStateAtLeast(c, targetState) || // 是否大于等于指定的状态
                                ctl.compareAndSet(c, ctlOf(targetState, workerCountOf(c)))) // CAS设置ctl的值
                                break; // 任意一个条件OK就可以结束了
                        }
                    }

                    private void interruptIdleWorkers(boolean onlyOne) {
                        final ReentrantLock mainLock = this.mainLock;
                        mainLock.lock();
                        try {
                            for (Worker w : workers) {
                                Thread t = w.thread; // 拿到Worker中的线程
                                if (!t.isInterrupted() && w.tryLock()) { // 先判断一下线程是不是没有被中断然后尝试加锁 但是通过前面的runWorker()源代码我们得知 开始之后是让Worker加了锁的 所以如果线程还在执行任务 那么这里肯定会false
                                    try {
                                        t.interrupt(); // 如果走到这里 那么说明线程肯定是一个闲着的线程 直接给中断吧
                                    } catch (SecurityException ignore) {
                                    } finally {
                                        w.unlock(); // 解锁
                                    }
                                }
                                if (onlyOne) // 如果只针对一个Worker 那么就结束循环
                                    break;
                            }
                        } finally {
                            mainLock.unlock();
                        }
                    }

而shutdownNow()方法也差不多 但是这里会更直接一些:

                    // shutdownNow开始后 不仅不允许新的任务到来 也不会再执行等待队列的线程 而且会终止正在执行的线程
                    public List<Runnable> shutdownNow() {
                        List<Runnable> tasks;
                        final ReentrantLock mainLock = this.mainLock;
                        mainLock.lock();
                        try {
                            checkShutdownAccess();
                            // 这里就是直接设定为STOP状态了 不再像shutdown那么温柔
                            advanceRunState(STOP);
                            // 直接中断所有工作线程 详细过程看下面
                            interruptWorkers();
                            // 取出仍处于阻塞队列中的线程
                            tasks = drainQueue();
                        } finally {
                            mainLock.unlock();
                        }
                        tryTerminate();
                        return tasks; // 最后返回还没开始的任务
                    }

                    private void interruptWorkers() {
                        final ReentrantLock mainLock = this.mainLock;
                        mainLock.lock();
                        try {
                            for (Worker w : workers) // 遍历所有Worker
                                w.interruptIfStarted(); // 无差别对待 一律加中断标记
                        } finally {
                            mainLock.unlock();
                        }
                    }

最后的最后 我们再来看看tryTerminate()是怎么完完全全终止掉一个线程池的:

                    final void tryTerminate() {
                        for (;;) { // 无限循环
                            int c = ctl.get(); // 上来先获取一下ctl值
                             // 只要是正在运行 或是 线程池基本上关闭了 或是 处于SHUTDOWN状态且工作队列不为空 那么这时还不能关闭线程池 返回
                            if (isRunning(c) ||
                                runStateAtLeast(c, TIDYING) ||
                                (runStateOf(c) == SHUTDOWN && ! workQueue.isEmpty()))
                                return;
                          
                             // 走到这里 要么处于SHUTDOWN状态且等待队列为空或是STOP状态
                            if (workerCountOf(c) != 0) { // 如果工作线程数不是0 这里也会中断空闲状态下的线程
                                interruptIdleWorkers(ONLY_ONE); // 这里最多只中断一个空闲线程 然后返回
                                return;
                            }
                    
                            // 走到这里 工作线程也为空了 可以终止线程池了
                            final ReentrantLock mainLock = this.mainLock;
                            mainLock.lock();
                            try {
                                if (ctl.compareAndSet(c, ctlOf(TIDYING, 0))) { // 先CAS将状态设定为TIDYING表示基本终止 正在做最后的操作
                                    try {
                                        terminated(); // 终止 暂时没有实现
                                    } finally {
                                        ctl.set(ctlOf(TERMINATED, 0)); // 最后将状态设定为TERMINATED 线程池结束了它年轻的生命
                                        termination.signalAll(); // 如果有线程调用了awaitTermination方法 会等待当前线程池终止 到这里差不多就可以唤醒了
                                    }
                                    return; // 结束
                                }
                            // 注意如果CAS失败会直接进下一轮循环重新判断
                            } finally {
                                mainLock.unlock();
                            }
                            // else retry on failed CAS
                        }
                    }

OK 有关线程池的实现原理 我们就暂时先介绍到这里 关于更高级的定时任务线程池 这里就不做讲解了