RocketMQ是阿里开源的高性能消息队列，支持事务消息，有较高的吞吐量。


一、RocketMQ服务
在启动RocketMQ客户端侧的服务前，需要先启动RocketMQ服务端。
兼容java8的RocketMQ最高版本是4.9.8的，注意版本选择，如果不兼容会导致无法正常创建消息主题。
安装好RocketMQ服务后，要配置好环境变量，RocketMQ相关的启动脚本中有使用ROCKETMQ_HOME环境变量信息。
RocketMQ启动分两部分，先执行mqnamesrv脚本启动RocketMQ命名服务，再执行mqbroker脚本启动Broker服务。

mqnamesrv脚本正常执行后，RocketMQ命名服务启动成功，启动日志会显示“The Name Server boot success”日志，
表示启动成功，命名服务默认以9876为启动端口，命名地址默认为：127.0.0.1:9876。
启动端口可以在namesrv相关jar包的配置文件中修改。

mqbroker脚本的执行需要指定命名地址，不然无法创建主题，这是一个坑点，启动时尤其要注意。
以windows中的mqbroker.cmd脚本执行为例，正确的执行命令如下（在%ROCKETMQ_HOME%\bin路径下打开cmd窗口执行）：
mqbroker.cmd -n 127.0.0.1:9876
通过-n指定命名服务连接地址。

主题创建可以用mqadmin脚本程序，以windows中的mqadmin.cmd脚本执行为例，执行命令如下：
mqadmin.cmd updateTopic -n 127.0.0.1:9876 -c DefaultCluster -t my-test-topic
“mqadmin.cmd updateTopic”是固定写法，“-n”后面接命名服务地址，“-c”后面接集群名称，“-t”后面接自定义主题名称。
更完整的格式是：
mqadmin.cmd updateTopic -n <NameServer地址> -c <Cluster名称> -t <Topic名称> -w <写队列数> -r <读队列数>
最关键的是命名服务地址、集群名称、主题名称。

查看RocketMQ当前已创建的主题，也可以用mqadmin，示例如下：
mqadmin.cmd topicList -n 127.0.0.1:9876
“mqadmin.cmd topicList”是固定写法。

如果想要RocketMQ在生产者发送消息时自动创建未存在的新主题，需要在启动Broker时指定autoCreateTopicEnable为true，例如：
mqbroker.cmd -n 127.0.0.1:9876 autoCreateTopicEnable=true
当然也可以在Broker服务的配置文件中指定。

RocketMQ的相关日志默认在${user_home}\logs\rocketmqlogs目录下。

二、RocketMQ-Dashboard
RocketMQ控制台，java8需要使用rocketmq-dashboard-2.0.0.jar即之下版本，2.0.1及之上的需要java11及之上版本。
控制台默认访问地址：http://127.0.0.1:8021/。
可以在jar包中的配置文件中修改启动端口。

三、Maven依赖
1、rocketmq依赖
要引入的是RocketMQ的客户端依赖，可以使用原生的或starter依赖。
原生依赖示例如下：
<dependency>
    <groupId>org.apache.rocketmq</groupId>
    <artifactId>rocketmq-client</artifactId>
    <version>4.9.8</version>
</dependency>

starter依赖示例如下：
<dependency>
    <groupId>org.apache.rocketmq</groupId>
    <artifactId>rocketmq-spring-boot-starter</artifactId>
     <version>2.2.3</version>
</dependency>

2、其他基础依赖
spring-boot-starter-parent、
spring-boot-starter-web

四、SpringBoot配置
1、rocketmq命名服务地址
通过rocketmq.namesrv属性配置，yml示例如下：
rocketmq:
  name-server: 192.168.1.100:9876;192.168.1.101:9876  # NameServer集群地址，多个用分号分隔

2、rocketmq生产者
通过rocketmq.producer属性配置，yml示例如下：
rocketmq:
  producer:
    group: my-producer-group  # 生产者组名（必须全局唯一）
    send-message-timeout: 3000  # 发送超时(ms)，默认3000
    retry-times-when-send-failed: 2  # 同步发送失败重试次数
    retry-times-when-send-async-failed: 2  # 异步发送失败重试次数
    compress-message-body-threshold: 4096  # 消息压缩阈值(字节)
    max-message-size: 4194304  # 最大消息大小(4MB)

这里最关键的是rocketmq.producer.group属性，全局唯一（在rocketmq实例层面）的生产者组名称。

3、rocketmq
通过rocketmq.consumer属性配置，yml示例如下：
rocketmq:
  consumer:
    group: my-consumer-group  # 消费者组名
    consume-thread-min: 5  # 最小消费线程数
    consume-thread-max: 32  # 最大消费线程数
    pull-batch-size: 32  # 每次拉取消息数
    consume-message-batch-max-size: 1  # 批量消费最大消息数
    message-model: CLUSTERING  # 消费模式：BROADCASTING(广播) 或 CLUSTERING(集群)
五、关键注解
1、@RocketMQMessageListener
消息监听注解，用在消息消费者的类声明上。
主要的属性包含有：topic、consumerGroup、selectorExpression、nameServer。
topic：消息主题，消息生产者在发送消息前需要先创建消息主题，可以通过配置让RocketMQ服务自动创建主题，但实际生产还是建议手动创建。
consumerGroup：消费者组名称，每一个消费者都必须要指定消费者组。
selectorExpression：过滤表达式，默认为*，即不过滤。可以指定tag过滤。
nameServer：RocketMQ命名服务地址，默认取${rocketmq.name-server:}，所以如果没有在application配置文件配置，
就需要在注解中通过name-server属性显示指定。其他的常用属性也是一样。

使用示例：（其中OrderMessage是自定义的消息实体类）
@Slf4j
@Service
@RocketMQMessageListener(
        topic = "order-topic",
        consumerGroup = "order-consumer-group",
        selectorExpression = "*"  // 可以指定tag过滤
)
public class OrderMessageConsumer implements RocketMQListener<OrderMessage> {
    @Override
    public void onMessage(OrderMessage orderMessage) {
        log.info("收到订单消息: {}", orderMessage);
        // 处理业务逻辑
    }
}

2、@RocketMQTransactionListener
事务消息监听。事务消息是通过半确认机制和两阶段提交来实现的，生产者发送给RocketMQ的消息先暂时存在Broker的内部主题中，
然后再由Broker提交到用户主题中。一旦生产者发送消息到RocketMQ失败，会直接回滚这部分消息，而不会透传到用户主题中。

这个注解在早期版本中通过txProducerGroup属性来显式指定事务消息生产者组，
而在starter2.2.3版本中，是通过rocketMQTemplateBeanName属性关联的RocketMQTemplate实例自动推断生产者组。
默认关联的是名称为“rocketMQTemplate”的RocketMQTemplate实例。
源码示例：
String rocketMQTemplateBeanName() default "rocketMQTemplate";

3、@ExtRocketMQTemplateConfiguration
扩展多个Producer配置，需要连接不同RocketMQ集群时使用。

六、机制原理
namesrv，命名服务；broker，协调服务。
既然是消息队列组件，那么至少就包含三种角色：消息生产者、消息消费者、消息队列本身。
这是每一种消息队列都有的基础架构，而对于RocketMQ来说，更关键的个性化区别是在消息队列这部分。

从RocketMQ服务的启动过程就可以知道RocketMQ包含namesrv和broker两种服务，而且必须是先启动namesrv，
然后启动broker关联namesrv，才能正常创建主题和发送消息。

消息生产者和消息消费者发送和消费消息，也是通过name-server进行，name-server就是RocketMQ服务的门户，
所有消息的出入都基于name-server，然后由name-server将消息的操作请求路由到broker。
所以name-server只是作为服务发现和路由的中心，实际执行消息的具体操作的是broker。
broker，原意为协调者，我更愿意称之为operator，即操作者。
消息的读写都是由broker来操行，为了提高消息处理效率，通常采用读写分离架构，即broker-master负责写，broker-slaver负责读，
broker-master将最新的消息同步给broker-slaver。
而对于broker-master和broker-slaver也可以采用多节点部署以提高可用性。

生产者通过name-server获取指定topic的broker列表，将消息发送到具体的broker服务，
如果broker服务采用的主从架构部署，会基于rocketmq客户端的轮询或哈希算法路由到其中一个broker-master。

消费者同样也是通过name-server获取指定topic的broker列表，然后从具体的broker服务拉取消息进行消费，消息消费成功后，
提交消息消费位点到broker服务。

rocketmq-dashboard提供了rocketmq的消息监控与运维的可视化平台，默认监控地址为127.0.0.1:9876的命名服务，
当然也可以在控制台页面新增要监控的命名服务。

七、官方文档
https://rocketmq.apache.org/zh/docs/