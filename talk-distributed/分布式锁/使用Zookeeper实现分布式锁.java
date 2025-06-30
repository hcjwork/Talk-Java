Zookeeper实现分布式锁的关键在于其提供的临时节点和顺序节点。
临时节点会在客户端断开或会话过期时删除，顺序节点在创建时会自动增加序列号。
使用临时节点可避免死锁，使用顺序节点则是为了让最小节点先获取锁以保证公平性。

当一个线程获取锁的时候，先创建一个临时顺序节点（当然所有竞争锁的线程都应该是在同一路径下创建临时顺序节点），
创建好临时顺序节点后，对该路径下的所有临时顺序节点进行从小到大的排序。
例如：
// 创建临时顺序节点
myNode = zk.create(lockPath + "/lock-", null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
// 获取该路径下的所有子节点
List<String> nodes = zk.getChildren(lockPath, false);
// 对节点从小到大排序
Collections.sort(nodes);

如果此时最小的节点就是自己刚创建的，表示锁获取成功；
如果最小节点不是自己刚创建的，说明暂时轮不到自己获取锁，这种情况下就监听自己的前一个节点，一旦前一节点被删除了，
那就说明自己的节点就会是最先的最小节点了，也就表示锁轮到自己持有了。

这个获取锁的过程里，最难就是对前一节点的删除监听，需要一直监听直到前一节点的删除事件被触发，
其实这里就需要利用并发工具比如CountDownLatch倒计时门闩，利用主线程阻塞等待子线程任务完成后再往下执行的特性，
监听前先设置门闩数为1，当监听到前一节点被删除后，门闩数减1，主线程由阻塞状态恢复运行，此时就代表当前线程已经获取到锁了。
例如：
// 找到前一个节点
String prevNode = nodes.get(nodes.indexOf(myNode.substring(myNode.lastIndexOf("/") + 1)) - 1);
// 只设置一道门闩
CountDownLatch latch = new CountDownLatch(1);
// 监听前一个节点的节点删除事件
zk.exists(lockPath + "/" + prevNode, event -> {
    if (event.getType() == Watcher.Event.EventType.NodeDeleted) {
        // 监听到前一节点被删除了，放开门闩
        latch.countDown();
    }
});

// 阻塞等待直到门闩被全部打开
latch.await(); 
// 表示获取到锁了
return true;

虽然临时节点在zk客户端断开连接时自动删除，但这是zk客户端的主动行为，
如果是zk客户端这边的任务还未完成就已经到了会话时间限制了（通常默认是40s），zk服务端也会删除掉临时节点。
这就可能导致其他线程提前获取到锁去执行任务，最终出现结果不合预期的问题。
所以为了使长时间任务能够顺利执行完毕，就需要维持一种锁续期的机制，而在Zookeeper中并没有像Redis那样可以直接设置锁存续时间的功能，
但可以定期发送心跳数据给zk服务端，告知zk服务端此客户端还想继续保持连接，那么zk服务端会重置会话有效时间，进而达到锁续期的目的。
例如：
ourPath = client.create().withMode(CreateMode.EPHEMERAL_SEQUENTIAL).forPath(lockPath + "/lock-");
// 启动续期线程
executor = Executors.newSingleThreadScheduledExecutor();
executor.scheduleAtFixedRate(() -> {
    try {
        // 通过getData操作维持会话活跃
        client.getData().forPath(ourPath); 
    } catch (Exception e) {
        // 处理异常
    }
}, 0, client.getZookeeperClient().getConnectionTimeoutMs() / 3, TimeUnit.MILLISECONDS);

其实现在已经有很多成熟高效的zk客户端框架，比如整合了zk的Spring Cloud Starter、Curator框架等，
这些框架已经处理好zookeeper做分布式锁的各类问题，也有更好的性能，更丰富便捷的API，实际生产中我们一般直接引用这些成熟框架，
而不是手动做zk实现分布式锁的开发，毕竟手动实现更复杂需要考虑平衡的东西也更多，学习、开发、测试成本也更高。
推荐使用Curator框架。
Curator主要用InterProcessMutex来创建分布式锁。
InterProcessMutex的构造方法传递一个Zookeeper客户端和指定节点根路径。
通过InterProcessMutex实例的acquire()方法获取锁，支持超时等待。
InterProcessMutex实例的release()方法用于释放锁，释放锁时删除该线程创建的临时顺序节点。

Zookeeper通过会话机制天然解决了锁续期问题，相比Redis的实现更加可靠，
这也是Zookeeper在强一致性场景下被优先选择的原因之一。
通常我们会将会话有效时间设置成大于业务最大可能处理时间以减少维持心跳的开销，
但对于长时间的任务应该要尽量拆分多个短小的任务，以降低其他线程等待锁的开销，
也能提高系统的并发性能。

