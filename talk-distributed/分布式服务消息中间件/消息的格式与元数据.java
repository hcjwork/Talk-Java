一条消息所包含的信息，除了消息内容本身外，应该还有传输协议、序列化方式、加解密方式、版本号等元数据信息。

一、Kafka消息格式与元数据信息
Kafka的消息是以二进制方式传输的。
1、消息数据格式
1）Header（可选）
键值对，用于扩展（如traceId、压缩算法）。
2）Key
分区路由键，可为null。如果为null默认以轮询的方式选择分区。
3）Value
实际消息内容，是序列化后的字节数据。
4）Timestamp
消息生成时间或追加时间。
5）Offset
分区内唯一递增序号，由Broker分配。
2、消息元数据
1）集群层面
Broker列表：节点地址、角色（Controller/Worker）等信息。
Topic配置：分区数、副本数、ISR（In-Sync Replicas）列表。
2）分区层面
Leader/Follower：读写责任节点。
Offset范围：当前分区的消息区间（logStartOffset到highWatermark）。

Kafka的消息以紧凑性的二进制数据进行传输，结合零拷贝技术，能具有非常高效的性能。
其实Kafka的消息也是经过了好几个版本的演进的，在低版本时只是基础格式并不包含时间戳，
在高版本中逐渐引入更多的关键信息，比如时间戳、协议等，
新版本中还支持传递压缩批处理（Record Batch）属性，可以节省带宽。

二、RocketMQ消息格式与元数据信息
1、消息数据格式
Topic：消息所属主题（字符串）。
Body：消息内容（二进制，支持任意序列化格式）。
Tags：消息标签（用于消费者过滤，如"order_pay"）。
Keys：业务标识（如订单ID，用于消息追踪）。
BornTimestamp：消息生成时间（生产者设置）。
StoreTimestamp：消息存储时间（Broker记录）。
QueueId：所属队列ID（自动分配或自定义）。
2、元数据
1）Broker集群
NameServer注册信息：Broker地址、Topic配置、队列分布。
CommitLog：所有消息的物理存储文件（顺序写入）。
2）Topic与队列
MessageQueue：逻辑队列（绑定到Broker的CommitLog）。
消费位点（Offset）：消费者组独立维护进度。

RocketMQ的消息结构没有复杂嵌套，解析起来更高效。同时也支持自定义属性。
消息结构中有一个Tag属性，可以实现消息过滤。
但是RocketMQ用CommitLog统一存储所有消息，CommitLog文件的大小会持续增长。
RocketMQ通过分片+异步清理机制解决CommitLog文件太大的问题，
CommitLog文件按照固定大小进行切割，默认以1GB进行切分，支持配置。
旧文件可通过配置过期删除或归档冷备，默认保留72小时。
消息先写入内存页缓存，再异步刷盘，以平衡性能与持久化需求。

三、RabbitMQ消息格式与元数据信息
1、消息格式
基于AMQP协议，核心属性包含：
Headers：扩展属性（键值对，如优先级、延迟设置）。
Body：消息内容（二进制，支持JSON/Protobuf等格式）。
Properties：元数据（如delivery_mode（持久化标记）、message_id、timestamp）。
Routing Key：路由键（决定消息投递到哪个队列）。
2、元数据
交换机（Exchange）：类型（Direct/Topic/Fanout等）、绑定规则。
队列（Queue）：名称、持久化标志、绑定关系。
虚拟主机（VHost）：逻辑隔离的租户空间。

RabbitMQ的消息可以通过交换机和路由键支持复杂路由规则，消息头可扩展，但没有嵌套结构，解析起来比较高效。
RabbitMQ的消息元数据可以动态调整，队列和交换机可以随时声明或删除，无需预分配，
而Kafka和RocketMQ的分区和队列需要预先创建。
