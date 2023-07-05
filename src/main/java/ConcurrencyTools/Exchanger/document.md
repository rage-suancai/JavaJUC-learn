### 数据交换 Exchanger
线程之间的数据传递也可以这么简单

使用Exchanger 它能够实现线程之间的数据交换:

                    static void test1() throws InterruptedException {

                        Exchanger<String> exchanger = new Exchanger<>();
                
                        new Thread(() -> {
                            try {
                                System.out.println("收到主线程传递的交换数据: " + exchanger.exchange("AAAA"));
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }).start();
                        System.out.println("收到子线程传递的交换数据: " + exchanger.exchange("BBBB"));
                
                    }

在调用exchange方后 当前线程会等待其它线程调用同一个exchanger对象exchange方法 当另一个线程也调用之后 方法会返回对方线程传入的参数

可见功能还是比较简单的