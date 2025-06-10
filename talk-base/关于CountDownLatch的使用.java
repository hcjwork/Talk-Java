CountDownLatch，倒计门闩，是juc包下的并发工具类，基于AQS（AbstractQueuedSynchronizer）实现。
CountDownLatch创建时需指定一个整数作为门闩的数量，这个整数须是非负数，否则将抛出异常IllegalArgumentException。

CountDownLatch设置了几道门闩，就最多支持几个线程并发访问。主要包含await()、countDown()等方法。

线程通过调用CountDownLatch对象的await()方法可以主动阻塞自己，此方法有一个重载方法支持设置一个超时时间。
调用await()方法后，AQS将调用线程包装成Node对象加入阻塞队列，并通过LockSupport.park(Object blocker)方法阻塞调用线程。

调用CountDownLatch对象的countDown()方法可以释放一道门闩，且只能释放一道门闩，如果有多道门闩，需要释放多次。
countDown()方法中会唤醒阻塞队列中队首节点里的线程。

CountDownLatch的门闩不可复用，如果想要重置门闩可以通过类包装下CountDownLatch属性，
通过创建新的CountDownLatch对象赋值给CountDownLatch属性，已达到门闩的重置效果。

其实CountDownLatch一般用于任务需要分批处理的场景，比如一个大的文件，分批解析入库，
要分成3个线程处理，将CountDownLatch的门闩数设置为3，每个线程执行完任务后调用countDown()方法释放一道门闩，
而主线程调用await()方法阻塞等待，直到3个线程全都执行完任务释放了3道门闩，主线程就可以继续运行。
例如：
private static final CountDownLatch downLatch = new CountDownLatch(3);
private static final ThreadPoolExecutor threadPool = new ThreadPoolExecutor(
        3, 3, 0, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(30)
);
private static String handleBatch() throws Exception {
    List<DataEntity> dataList = getDataList();
    if (!dataList.isEmpty()) {
        int splices = dataList.size() / 3;
        AtomicInteger counter = new AtomicInteger();

        threadPool.execute(() -> {
            resolveAndTransferDB(dataList.subList(counter.get() * splices, counter.incrementAndGet() * splices));
            // 任务完成后，释放1道门闩
            downLatch.countDown();
        });
        // 主线程阻塞等待3道门闩全部释放
        downLatch.await();
    }
    return "SUCCESS";
}

如果想要控制多线程互斥访问，则需要给每个线程配置一个CountDownLatch，门闩数都设置为1，
通过CountDownLatch的await()方法主动阻塞，通过CountDownLatch的countDownLatch()方法唤醒指定线程。
下面通过一个场景来熟悉这种实现：使用CountDownLatch实现三个线程交替打印1~100
示例伪代码：
private static volatile int counter = 1; // 计数器从1开始，使用volatile保证内存可见性
// 创建3个倒计门闩分给3个线程
private static CountDownLatchWrapper latch1 = new CountDownLatchWrapper(new CountDownLatch(1));
private static CountDownLatchWrapper latch2 = new CountDownLatchWrapper(new CountDownLatch(1));
private static CountDownLatchWrapper latch3 = new CountDownLatchWrapper(new CountDownLatch(1));
// 因为CountDownLatch的门闩不可复用，若要重置门闩则需要新建，但Java方法是值传递，只能通过属性方式来进行修改
@Data // lombok的注解
static class CountDownLatchWrapper {
    CountDownLatch latch;
    CountDownLatchWrapper(CountDownLatch latch) {
        this.latch = latch;
    }
}
// 创建3个线程，并调用打印方法
public static void doPrintWork() {
    new Thread(() -> print(latch1, latch2), "线程1").start();
    new Thread(() -> print(latch2, latch3), "线程2").start();
    new Thread(() -> print(latch3, latch1), "线程3").start();

    // 唤醒线程1
    latch1.getLatch().countDown();
}
// 打印1~100
private static void print(CountDownLatchWrapper cur, CountDownLatchWrapper next) {
    try {
        // 循环直到超过100
        while (counter <= 100) {
            // 进来先阻塞，等待唤醒
            cur.getLatch().await();
            // 被唤醒时，先检查在阻塞期间，计时器有没有累计到超过100
            if (counter > 100) {
                // 唤醒下一个线程
                next.getLatch().countDown();
                // 退出方法
                return;
            }
            // 执行打印操作，递增计数器
            System.out.println(Thread.currentThread().getName() + "：" + counter++);
            // 重置门闩
            cur.setLatch(new CountDownLatch(1));
            // 唤醒下一个线程
            next.getLatch().countDown();
        }
    } catch (InterruptedException e) {
        throw new RuntimeException(e);
    }
}

