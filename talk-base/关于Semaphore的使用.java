Semaphore是juc包下的一个并发工具类，基于AQS（AbstractQueuedSynchronizer，抽象队列同步器）实现。
通过Permit许可证的发送与回收，实现线程的并发访问控制。
创建Semaphore对象时需要传递许可证的数量，这个数量是整数，可以为负。
另外还支持传递一个布尔值以设置公平性，如果不传这个参数默认是非公平模式。
非公平即允许线程插队获取许可证，而不是按照线程申请的顺序来的。

只有获取到许可证时线程才能继续实现，否则会被阻塞。
通过Semaphore对象的acquire()方法可以获取一个许可证，也可以通过tryAcquire()方法来获取。
tryAcquire()方法会尝试获取一个许可证，获取到了会返回true，获取不到会返回false。
通过tryAcquire()方法获取不到许可证时，调用此方法的线程也会被阻塞。

而Semaphore对象的release()方法用于发放一个许可证。
该方法可以调用多次。也支持传递一个整数，表示一次性发放多个许可证。
一个许可证只能由一个线程持有，但多个线程可以在同一时刻各自持有许可证。
所以如果想要保证多个线程访问的互斥性，比如在同一时刻只能一个线程运行，就需要每个线程分配一个专属的信号量来控制，
信号量的最大许可证数量不能超过1，超过1就意味着允许其他线程同时访问了。

下面从一个场景实现来熟悉Semaphore的应用：使用Semaphore实现三个线程交替打印1~100
示例伪代码：
private static final Semaphore semaphore1 = new Semaphore(0); // 分配给线程1的信号量，初始可用的许可证为0
private static final Semaphore semaphore2 = new Semaphore(0); // 分配给线程2的信号量，初始可用的许可证为0
private static final Semaphore semaphore3 = new Semaphore(0); // 分配给线程3的信号量，初始可用的许可证为0
private static volatile int counter = 1; // 计数器，从1开始，使用volatile保证内存可见性

public static void doPrintWork() {
    // 创建三个线程，调用打印方法，传递信号量对象
    new Thread(() -> print(semaphore1, semaphore2), "线程1").start();
    new Thread(() -> print(semaphore2, semaphore3), "线程2").start();
    new Thread(() -> print(semaphore3, semaphore1), "线程3").start();

    // 给线程1发一个许可证，让线程1先打印
    semaphore1.release();
}
// 打印方法，传递当前线程对应的信号量和下一个要打印的线程的信号量
private static void print(Semaphore cur, Semaphore next) {
    while (counter <= 100) {
        try {
            // 进来先尝试获取许可证，获取不到则阻塞等待
            cur.acquire();
            // 获取到了许可证，先判断计数器是否已超过100，因为阻塞等待期间可能计时器已经被累加到超过100了
            if (counter > 100) {
                // 给下一个线程发放一个许可证
                next.release();
                // 退出方法
                return;
            }
            // 打印计数后累加计数器
            System.out.println(Thread.currentThread().getName() + "：" + counter++);
            // 给下一个线程发放一个许可证
            next.release();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}

一个信号量可以支持多个许可证，这意味着允许持有同一信号量的许可证的多个线程同时访问资源。
Semaphore对象的release方法，支持传递要发放的许可证数量，但不能小于0，如果小于0则抛出异常IllegalArgumentException。
如果该方法不指定许可证数量，默认发放一个许可证。
其许可证的数量由AQS中定义的state字段保存，发放许可证时，基于当前可用的许可证数量，加上要发放的数量，
通过compareAndSetState()方法进行CAS操作，不断尝试直到state修改成功为止。
许可证数量修改成功后，利用LockSupport.unpark()方法唤醒阻塞队列中的其中一个线程。
至于唤醒哪一个线程，由Semaphore对象创建时所指定的公平性决定。

而Semaphore对象的acquire()方法，获取许可证失败时，将调用线程包装成Node加入阻塞队列的队尾，使用LockSupport.park()
阻塞调用线程。

阻塞队列的维护、线程的阻塞和唤醒、还包括唤醒的公平性，都是在AQS中实现的。
