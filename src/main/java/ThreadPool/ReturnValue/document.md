### 执行带返回值的任务
一个多线程任务不仅仅可以是void无返回值任务 比如我们现在需要执行一个任务 但是我们需要再任务执行之后得到一个结果 这个时候怎么办呢?

这里我们就可以使用到Future了 它可以返回任务的计算结果 我们可以通过它来获取任务的结果以及任务当前是否完成:

                    static void test() throws ExecutionException, InterruptedException {

                        ExecutorService pool = Executors.newSingleThreadExecutor(); // 直接用Executors创建 方便就完事了
                
                        Future<String> future = pool.submit(() -> "我是字符串"); // 使用submit提交任务 会返回一个Future对象 注意提交的对象可以是Runable也可以是Callable 这里使用的是Callable能够自定义返回值
                        System.out.println(future.get()); // 如果任务未完成 get会被阻塞 任务完成返回Callable执行结果返回值
                        pool.shutdown();
                
                    }

当然结果也可以一开始就定义好 然后等待Runnable执行完之后再返回:

                     static void test() throws ExecutionException, InterruptedException {

                        ExecutorService pool = Executors.newSingleThreadExecutor();
                
                        Future<String> future = pool.submit(() -> {
                            try {
                                TimeUnit.SECONDS.sleep(3);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }, "我是字符串");
                        System.out.println(future.get());
                        pool.shutdown();
                
                    }

还可以通过传入FutureTask对象的方式:

                    static void test() throws ExecutionException, InterruptedException {

                        ExecutorService pool = Executors.newSingleThreadExecutor();
                
                        FutureTask<String> task = new FutureTask<>(() -> "我是字符串");
                        pool.submit(task);
                        System.out.println(task.get());
                        pool.shutdown();
                
                    }

我们可以还通过Future对象获取当前任务的一些状态:

                    static void test() throws ExecutionException, InterruptedException {

                        ExecutorService pool = Executors.newSingleThreadExecutor();
                
                        Future<String> future = pool.submit(() -> "你好");
                        System.out.println(future.get());
                        System.out.println("任务是否执行完成: " + future.isDone());
                        System.out.println("任务是否被取消: " + future.isCancelled());
                        pool.shutdown();
                
                    }

我们来试试看在任务执行途中取消任务:

                    static void test() {

                        ExecutorService pool = Executors.newSingleThreadExecutor();
                
                        Future<String> future = pool.submit(() -> {
                            TimeUnit.SECONDS.sleep(10);
                            return "下次一定";
                        });
                        System.out.println(future.cancel(true));
                        System.out.println(future.isCancelled());
                        pool.shutdown();
                
                    }