### ABA问题及解决方案
我们来想象一下这种场景:

<img src="https://fast.itbaima.net/2023/03/06/KQjEvX1ZxohMT3l.png">

线程1和线程2同时开始对a的值进行CAS修改 但是线程1的速度比较快 将a的值修改为2之后紧接着又修改回1 这时线程2才开始进行判断 发现a的值是1 所以CAS操作成功

很明显 这里的1已经不是一开始的那个1了 而是被重新赋值的1 这也是CAS操作存在的问题(无锁虽好 但是问题多多)
它只会机械地比较当前值是不是预期值 但是并不会关心当前值是否被修改过 这种问题称之为ABA问题

那么如何解决这种ABA问题呢 JUC提供了带版本号的引用类型 只要每次操作都记录一下版本号 并且版本号不会重复 那么就可以解决ABA问题了:

                    static void test() {

                        String a = "Hello"; String b = "World";
                        AtomicStampedReference<String> reference = new AtomicStampedReference<>(a, 1); // 在构造时需要指定初始值和对应的版本号
                        reference.attemptStamp(a, 2); // 可以中途对版本号进行修改 注意要填写当前的引用对象
                        System.out.println(reference.compareAndSet(a, b, 2, 3)); // CAS操作时不仅需要提供预期值和修改值 还要提供预期版本号和新的版本号
                
                    }

至此 有关原子类的讲解就到这里