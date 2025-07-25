为了增加消息处理的并行度，提高吞吐量，实现消息的局部顺序性，消息中间件通常在消息的存储单元有队列或分区的概念。
为了保证高可用性，MQ服务往往也是以多实例方式部署运行。
而对于消息生产者来说，它其实不知道MQ服务有多个实例或有多队列或多分区概念，它也不关心这里面的细节，它只需要MQ服务能正常
接收它发出的消息就行。
所以MQ客户端就需要保证消息能够发送到其中一个MQ服务实例并在某个队列或分区中保存，
这个工作就是消息的路由，或者说是消息的负载均衡。

消息的路由有两个阶段要考虑，一是有多个MQ服务实例具体选择哪一个进行连接和消息交互，
二是MQ服务在一个主题下有多个队列或分区具体选择哪一个进行消息存储。
1）MQ服务实例的选择
MQ服务实例的选择，需要知道MQ服务实例信息列表，最关键的信息是服务名、服务ip、服务port。
那么当MQ服务多个实例启动时，就需要有个地方维护MQ服务实例的关键信息。
在主流的消息中间件中一般用一个注册中心或元数据管理模块来维护MQ服务实例的信息。

Kafka在旧版本使用Zookeeper做MQ服务的注册与发现中心，在新版本使用Raft共识算法管理MQ服务的元数据。
Raft共识算法具体是由Broker节点（MQ服务实例节点）选举出Controller并管理集群元数据如分区Leader、Broker列表）。

RocketMQ则通过轻量级注册中心NameServer来维护MQ服务实例的元数据信息。
MQ服务实例与NameServer保持心跳连接，MQ客户端从NameServer拉取MQ服务可用实例列表。
NameServer本身无状态且可集群部署。

RabbitMQ没有使用注册中心，而是采用去中心化设计，利用Erlang的epmd进程和net_kernel模块维护MQ服务实例信息，
因此RabbitMQ客户端一侧（消息生产者或消费者服务）必须显示指定一个RabbitMQ服务端的连接地址（通过配置文件或配置代码）。
MQ客户端必须配置至少一个入口MQ服务节点，MQ服务节点间通过Erlang协议同步状态，但对MQ客户端透明。

获取到了MQ服务可用实例列表，MQ客户端就可以执行不同的选择策略了，也就是负载均衡策略。
通常要实现轮询、随机、一致性哈希这三种负载均衡策略，有些MQ客户端没有内置这些负载混合策略，就需要使用消息的业务自己实现，
比如本地维护一个MQ服务连接地址集合，根据地址集合实现所需的选择策略。
在主流的消息中间件里，Kafka和RocketMQ在客户端里是内置了负载均衡策略的，一般默认是采用轮询策略。
而RabbitMQ没有提供这几种负载均衡策略，使用x-consistent-hash插件可以实现现一致性哈希的策略，轮询和随机则依赖于
引用RabbitMQ客户端的业务服务自己实现。

2）MQ队列或分区的选择
当选定了具体的MQ服务实例进行连接和消息发送后，还需要对主题具体队列或分区进行选择以决定存储在哪个位置。
队列或分区的选择，对于消息生产者来说，也是需要负载均衡的。主题下有多个队列或多个分区，都处于一个MQ服务实例中，
具体存储不知道选择哪一个，因此生产者在发送消息时应当要支持对队列或分区的选择或路由。

Kafka支持在发送消息时直接指定分区，也可以不指定分区，不指定分区则默认以轮询方式选择分区存储消息。

RocketMQ中是队列的概念，但不支持直接指定队列，而是支持设置自定义队列选择器，
比如使用RocketMQTemplate通过setMessageQueueSelector方法 指定自定义的队列选择器，
这个自定义的队列选择器需要实现MessageQueueSelector接口，实现其select方法。
通过自定义可以实现轮询、随机、一致性哈希等多种选择策略。

RabbitMQ也是队列的概念，在RabbitMQ中队列在创建时需要指定消息主题，创建交换机时指定队列和路由键，
消息生产者发送消息时直接指定具体的队列和路由键。


