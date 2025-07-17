Kafka是一种高吞吐、支持事务消息的消息队列组件。

一、Kafka服务启动
Kafka是基于Zookeeper做节点管理器的，启动Kafka之前要先启动Zookeeper。
在Kafka的旧版本中，需要独立安装部署Zookeeper，而在新版本中Kafka安装包里内置了Zookeeper和对应的启动脚本。

二、Kafka控制台
Kafka官网并没有提供控制台工具，常见的非官方工具有：Kafka-Manager、Kafka-Tool等。

三、Maven依赖
1、kafka
可以使用spring与kafka的整合依赖spring-kafka，
例如：
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>

也可以使用kafka客户端原生依赖，
例如：
<dependency>
    <groupId>org.apache.kafka</groupId>
    <artifactId>kafka-clients</artifactId>
    <version>3.2.3</version>
</dependency>

注意：兼容Java8和SpringBoot2.x版本的Kafka客户端，最高到3.2.3版本。

2、其他基础依赖
spring-boot-starter-parent、
spring-boot-starter-web

四、核心注解
1、@EnableKafka
开启Kafka功能。用在启动类或配置类的类声明上。

2、@KafkaListener
消息监听。用于消费者类中。关键属性包含：topics、groupId、containerFactory、concurrency。
topics：指定要监听的Topic名称，支持数组。
groupId：消费者组ID，全局唯一。
containerFactory：指定自定义的监听器容器工厂。
concurrency：消费者并发数，默认为1。

3、@EnableKafkaStreams
启用 Kafka Streams 功能。

4、@Transactional
这是Spring的声明式事务注解。Kafka的事务边界，也需要结合这个注解来体现。常用于消息队列事务消息+本地事务的分布式事务解决方案中。

五、核心组件与原理
Kafka作为一个消息队列，自身肯定要有消息存储和管控功能部分的。
消息生产者和消息消费者则依赖于Kafka的客户端与Kafka服务端进行交互。

消息存储和管控功能部分的子组件称为代理协调器，即Broker，对于消息的接收、提交、记录、分发等工作负责。
简单来说，Broker就是指的Kafka服务，有一个Kafka服务就有一个概念上的Broker。

如果是集群模式下，有多个Kafka服务，即有多个Broker，这些Broker哪些是可用的，这部分信息需要维护。
因为对于消息的生产者和消费者来说，不关心Kafka服务端有几个，只关心服务能否正常提供，
所以在Kafka服务端这一侧确实需要管理Broker的注册与发现功能，而Kafka是选择用Zookeeper来实现。
当然Zookeeper除了管理Broker的注册与发现之外，还负责副本replicas的主本节点选举。

消息生产者通过Kafka客户端与Kafka服务端连接上了，生产的消息发送到哪呢？主题中。
Kafka基于主题的发布和订阅来支持消息的生产与消费。
消息生产者通过KafkaTemplate发送消息时，必须指定消息的主题，即Topic，主题的存在让消息在业务层面可以有所区分。

Kafka为每一个主题都维护独立的消息的相关文件，包括消息、日志等。
每一个Topic包含多个Partition分区，每一个分区有多个Replication副本。

Broker的主要功能就是持久化消息以及将消息队列中的消息从发送端传输到消费端。

Kafka利用Zookeeper临时节点来管理broker生命周期的。
broker启动时在Zookeeper中创建对应的临时节点，同时还会创建一个监听器监听该临时节点的状态。
一旦broker启动后，监听器会自动同步整个集群信息到该broker上；
而broker一旦崩溃，其与Zookeeper的会话就会失效，Zookeeper会删除其之前创建的临时节点，节点监听器被触发，
然后处理broker崩溃的后续事宜。
（这个监听器是谁创建的？broker崩溃后，谁来执行监听器的处理？）

一个Kafka分区本质就是一个备份日志，利用多份相同的备份共同提供冗余机制来保持系统的高可用性。这些备份被称为副本（replica）。
Kafka把分区的所有副本均匀地分配到所有的broker上，并从这些副本中挑选一个作为leader副本对外提供服务，
而其他副本被称为follower副本，follower副本只能被动地向leader副本请求数据，从而保持与leader副本的同步。

如果leader副本所在的broker宕机了，就要从follower副本选一个作为新的leader副本，但各个follower副本与原leader副本的
数据差异不相同，有些落后太多的follower副本是不适合被选作leader的，不然会丢失很多数据。
因此Kafka引入了ISR，ISR就是Kafka集群动态维护的一组同步副本集合（in-sync replicas），leader副本本身也在ISR列表内。

每个topic分区都有自己的ISR列表，ISR中的所有副本都与leader保持同步状态，只有ISR中的副本才有资格被选举为leader。
如果ISR中的副本落后leader太多，就会被踢出ISR列表。
producer写入的一条Kafka消息只有被ISR中的所有副本都接收到，才被视为“已提交”状态。
所以如果一个分区的ISR中有N个副本，那么该分区最多允许忍受N-1个副本崩溃而不丢失已提交消息。






