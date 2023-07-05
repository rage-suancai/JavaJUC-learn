### 信号量 Semaphore
还记得我们在《操作系统》中详细的信号量机制吗? 它在解决进程之间的同步问题中起着非常大的作用

    信号量(Semaphore) 有时被称为信号灯 是在多线程环境下使用的一种设施 是可以用来保证两个或多个关键代码段不被并发调用 在进入一个关键代码段之前
    线程必须获取一个信号量 一旦该关键代码段完成了 那么该线程必须释放信号量 其它想进入该关键代码段的线程必须等待直到第一个线程释放信号量

通过使用信号量 我们可以决定某个资源同一时间能够被访问的最大线程数 它相当于对某个资源的访问进行了流量控制 简单来说 它就是一个可以被N个线程占用的排它锁
(因此也支持公平和非公平模式) 我们可以在最开始设定Semaphore的许可证数量 每个线程都可以获得1个或n个许可证 当许可证耗尽或不足以供其他线程获取时 其他线程将被阻塞:

                    static void test() {
                        
                        // 每一个Semaphore都会在一开始获得指定的许可证数量 也就是许可证配额
                        Semaphore semaphore = new Semaphore(2); // 许可证配额设定为2
                
                        for (int i = 0; i < 3; i++) {
                            new Thread(() -> {
                                try {
                                    semaphore.acquire(); // 申请一个许可证
                                    System.out.println("许可证申请成功");
                                    semaphore.release(); // 归还一个许可证
                                } catch (InterruptedException e) {
                                    throw new RuntimeException(e);
                                }
                            }).start();
                        }
                
                    }

                    static void test() {

                        Semaphore semaphore = new Semaphore(3);
                
                        for (int i = 0; i < 2; i++) {
                            new Thread(() -> {
                                try {
                                    semaphore.acquire(2); // 一次性申请两个许可证
                                    System.out.println("许可证申请成功");
                                } catch (InterruptedException e) {
                                    throw new RuntimeException(e);
                                }
                            }).start();
                        }
                
                    }

我们也可以通过Semaphore获取一些常规信息:

                    static void test() {

                        Semaphore semaphore = new Semaphore(3); // 只配置三个许可证 5个线程进行争抢 不内卷还想要许可证?
                
                        for (int i = 0; i < 5; i++) 
                            new Thread(semaphore::acquireUninterruptibly).start(); // 可以以不响应中断(主要是能简写一行 方便)
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        System.out.println("剩余许可证数量: " + semaphore.availablePermits());
                        System.out.println("是否存在线程等待许可证: " + (semaphore.hasQueuedThreads() ? "是" : "否"));
                        System.out.println("等待许可证线程数量: " + semaphore.getQueueLength());
                
                    }

我们可以手动回收掉所有的许可证:

                    static void test4() {

                        Semaphore semaphore = new Semaphore(3);
                        
                        new Thread(semaphore::acquireUninterruptibly).start();
                        try {
                            Thread.sleep(500);
                            System.out.println("收回剩余许可数量: " + semaphore.drainPermits());
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                
                    }

这里我们模拟一下 比如现在有10个线程同时进行任务 任务要求是执行某个方法 但是这个方法最多同时只能由5个线程执行 这里我们使用信号量就非常合适