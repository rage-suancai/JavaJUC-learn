### 多线程执行定时任务
既然线程池怎么强大 那么线程池能不能执行定时任务呢? 我们之前如果需要执行一个定时任务 那么肯定会用到Timer和TimerTask
但是它只会创建一个线程处理我们的定时任务 无法实现多线程调度 并且它无法处理异常情况一旦抛出未捕获异常那么会直接终止 显然我们需要一个更加强大的定时器

JDK5之后 我们可以使用ScheduledThreadPoolExecutor来提交定时任务 它继承自ThreadPoolExecutor
并且所有的构造方法都必须要求最大线程池容量为Integer.MAX_VALUE 并且都是采用的DelayedWorkQueue作为等待队列:

                    public ScheduledThreadPoolExecutor(int corePoolSize) {
                        super(corePoolSize, Integer.MAX_VALUE, 0, NANOSECONDS,
                              new DelayedWorkQueue());
                    }
                    
                    public ScheduledThreadPoolExecutor(int corePoolSize,
                                                       ThreadFactory threadFactory) {
                        super(corePoolSize, Integer.MAX_VALUE, 0, NANOSECONDS,
                              new DelayedWorkQueue(), threadFactory);
                    }
                    
                    public ScheduledThreadPoolExecutor(int corePoolSize,
                                                       RejectedExecutionHandler handler) {
                        super(corePoolSize, Integer.MAX_VALUE, 0, NANOSECONDS,
                              new DelayedWorkQueue(), handler);
                    }
                    
                    public ScheduledThreadPoolExecutor(int corePoolSize,
                                                       ThreadFactory threadFactory,
                                                       RejectedExecutionHandler handler) {
                        super(corePoolSize, Integer.MAX_VALUE, 0, NANOSECONDS,
                              new DelayedWorkQueue(), threadFactory, handler);
                    }

我们来测试一下它的方法 这个方法可以提交一个延时任务 只有到达指定时间之后才会开始:

                    static void test() {

                        ScheduledThreadPoolExecutor pool = new ScheduledThreadPoolExecutor(1); // 直接设定核心线程数为1
                        pool.schedule(() -> System.out.println("Hello Java😪"), 3, TimeUnit.SECONDS); // 这里我们计划再3秒后执行
                        pool.shutdown();
                
                    }

我们也可以像之前一样 传入一个Callable对象 用于接收返回值:

                    static void test2() throws ExecutionException, InterruptedException {

                        ScheduledThreadPoolExecutor pool = new ScheduledThreadPoolExecutor(2);
                
                        ScheduledFuture<String> future = pool.schedule(() -> "????", 3, TimeUnit.SECONDS);
                        System.out.println("任务剩余等待时间: " + future.getDelay(TimeUnit.MILLISECONDS) / 1000.0 + "s");
                        System.out.println("任务执行结果: " + future.get());
                        pool.shutdown();
                
                    }

可以看到schedule方法返回了一个ScheduledFuture对象 和Future一样 它也支持返回值的获取、包括对任务的取消同时还支持获取剩余等待时间

那么如果我们希望按照一定的频率不断执行任务呢?

                    static void test3() {

                        ScheduledThreadPoolExecutor pool = new ScheduledThreadPoolExecutor(2);
                        pool.scheduleAtFixedRate(() -> System.out.println("Hello Java😪"),
                            3, 1,TimeUnit.SECONDS); // 三秒钟延迟开始 之后每隔一秒钟执行一次
                
                    }

Executors也为我们预置了newScheduledThreadPool方法用于创建线程池:

                    static void test4() {

                        ScheduledExecutorService pool = Executors.newScheduledThreadPool(1);
                        pool.schedule(() -> System.out.println("Hello Java😪"), 1,TimeUnit.SECONDS);
                        pool.shutdown();

                    }