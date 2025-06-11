CyclicBarrier，循环屏障，是juc包下的一个并发工具类，基于ReentrantLock和Condition实现的，
而ReentrantLock和Condition基于AQS实现。

CyclicBarrier通过屏障点（parties）来控制线程相互等待直到到达屏障的线程数符合预期。

CyclicBarrier相比于CountDownLatch有所区别，CountDownLatch的门闩不可重置，而CyclicBarrier的屏障点可以重置。
另外CountDownLatch是主线程等待子线程，而CyclicBarrier是子线程相互等待。

CyclicBarrier也可用于大任务分片后并行处理，子线程处理完自己的任务后，调用CyclicBarrier对象的await()方法，
阻塞等待，每调一次await()方法所需到达屏障的线程数减1，直到预期数量的线程到达屏障时，所有调用await()方法的线程都被唤醒。
如果CyclicBarrier的屏障点数设置为N，调用await()方法的次数是M，如果M<N则所有调用await()方法线程一直阻塞。

CyclicBarrier的构造方法支持传递一个方法，这个方法由最后一个到达屏障的线程执行。
但CyclicBarrier不确定也不保证最后到达屏障的是哪个线程，而是类似于门的作用，这个门上有N把锁，锁的数量即屏障点数，
每个调用await()方法的线程都获取到其中一把锁的钥匙，只有拿到所有锁的钥匙才能打开这个门继续通行。
（也可以想象是田径赛道的终点线，所有线程不管什么时候起跑，从哪个赛道起跑，最后都要到终点汇合，
只要到达终点的线程数未达到预期，所有其他线程无法往下执行其他任务。）

CyclicBarrier创建示例：
private static CyclicBarrier barrier = new CyclicBarrier(2, () -> {});

CyclicBarrier的屏障点数被消耗到0后，会自动重置屏障点数，CyclicBarrier可继续使用。
利用这一点可以实现多个线程的交替执行，例如：使用CyclicBarrier实现三个线程交替打印1~100
示例伪代码：
private static CyclicBarrier barrier = new CyclicBarrier(3, () -> {}); // 3个屏障点数，对应3个线程
private static volatile int counter = 1; // 计时器从1开始，使用volatile保证内存可见性
private static volatile int printId = 1; // 当前应该做打印工作的线程的编号，从1开始，使用volatile保证内存可见性
// 创建三个线程，调用打印方法，传递自己的编号和下一个准备打印工作的线程编号
public static void doPrintWork() {
    new Thread(() -> print(1, 2), "线程1").start();
    new Thread(() -> print(2, 3), "线程2").start();
    new Thread(() -> print(3, 1), "线程3").start();
}
// 传递当前要打印的线程的编号和下一个要打印的线程的编号，以此来控制线程交替打印
private static void print(int curId, int nextId) {
    // 一直循环直到计数器大于100就退出方法
    for (;;) {
        try {
            // 等待其他线程到达屏障。屏障点数是3，每次用完屏障点数会自动重置
            barrier.await();
            // 所有线程都到了屏障点，先检查当前计数是否已超过100
            if (counter > 100) {
                // 超过100退出方法
                return;
            }
            // 如果是自己该打印的，则打印计数后，递增计数器
            if (printId == curId) {
                System.out.println(Thread.currentThread().getName() + "：" + counter++);
                // 把打印权传递给下一个线程
                printId = nextId;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

CyclicBarrier有些特殊，设置了屏障点数为3，就必须有三个线程调用await()方法后，三个线程才能往下执行。
主线程调用await()方法不算在内，因为CyclicBarrier有一个分代（generation）概念，也就是分层。
主线程是第一层，主线程开辟的子线程是第二层，子线程开启的子线程是第三层，以此类推。
只有同一层到达屏障的线程数与屏障点数一致了这一层的线程才会执行，否则全部阻塞。




