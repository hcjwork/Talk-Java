SpringBoot整合使用Zookeeper可以用spring-cloud-stater-zookeeper包，也可以用curator包。
以Maven依赖为例，
如果是用spring-cloud-stater-zookeeper，引入依赖如下：
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-zookeeper</artifactId>
</dependency>
版本从spring-cloud继承。
如果是用curator，引入依赖如下：
<dependency>
    <groupId>org.apache.curator</groupId>
    <artifactId>curator-recipes</artifactId>
    <version>5.5.0</version>
</dependency>

ZookeeperConfig配置，主要关注：服务端连接地址、会话超时时间。
服务端连接地址支持传递Zookeeper服务集群地址，不同服务节点的地址用英文逗号分隔。
会话超时时间的设置需要考虑业务最大可能处理时间，避免要额外处理锁续期问题。

创建节点：
通过ZooKeeper实例的create()方法可以创建新的节点，该方法可传递节点路径，节点数据，还可以指定节点类型。
ZooKeeper支持的节点类型有：持久节点、临时节点、临时顺序节点。
在3.5.0版本开始又引入了持久顺序节点、持久过期节点、持久顺序过期节点。过期节点支持传递一个过期时间，时间到了会自动删除该节点。

虽然过期节点支持指定一个过期时间，但是过期时间不可刷新，也就是说如果以此来实现分布式锁，没法为锁延迟时间。
其次持久节点感知到不到客户端的断联，即使客户端崩溃，节点仍然存活直到TTL结束。
实际生产中还是用的临时顺序节点来实现分布式锁。

如果创建节点是指定节点具有顺序性，会在节点名称后自动拼接递增序列串。
create()方法中还需要传递一个ACL集合，可以从ZooDefs.Ids中选择类型。
本地测试可以使用OPEN_ACL_UNSAFE，因为它允许所有用户对数据进行读写操作，
但在生产中，应该定义更精细的访问控制列表（ACL）。例如，使用ZooDefs.Ids.CREATOR_ALL_ACL或其他更安全的ACL。

删除节点：
Zookeeper不允许直接删除包含有子节点的节点，如果想要一次性删除则需要进行递归处理。
例如：
private void deleteRecursive(String path) throws Exception {
    List<String> children = zooKeeper.getChildren(path, false);
    for (String child : children) {
        deleteRecursive(path + "/" + child); // 递归删除每个子节点
    }
    zooKeeper.delete(path, -1); // 最后删除父节点
}

zk可视化工具和对应下载地址：
zkui    https://github.com/DeemOpen/zkui
ZooInspector   https://issues.apache.org/jira/secure/attachment/12436620/ZooInspector.zip
zkdash  https://github.com/ireaderlab/zkdash
prettyZoo   https://github.com/vran-dev/PrettyZoo/releases