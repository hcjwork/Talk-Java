1、设计目标
Kafka：高吞吐量，流处理。
RocketMQ：金融级可靠性与低延迟。
RabbitMQ：复杂路由支持，极低延迟。

2、存储模型
Kafka：Broker+Topic+Partition+Replicas。
RocketMQ：Broker+Topic+MessageQueue。
RabbitMQ：Topic+Queue+Exchange。

3、存储方式
Kafka：磁盘持久化。
RocketMQ：磁盘持久化。
RabbitMQ：内存，磁盘持久化。

4、事务支持
Kafka：仅生产者确认模式为Exactly-Once精确消费一次时支持。
RocketMQ：完整两阶段事务支持，金融级事务性。
RabbitMQ：基本事务支持，性能差

5、控制台
Kafka：无内置控制台，需外部实现。
RocketMQ：有RocketMQ-Dashboard控制台，但不在RocketMQ服务中内置。
RabbitMQ：RabbitMQ服务包内置有控制台Web服务，启动RabbitMQ服务时控制台服务一同启动。

6、核心优势
Kafka：超高吞吐量，非常适合海量数据场景。支持流处理，适用于日志、事件等流处理场景。
RocketMQ：为高并发的金融交易设计，具有高可靠性和低延迟特性，事务性强，顺序性强。
适用于金融业务、强事务性、顺序性要求高的场景。
RabbitMQ：路由配置可动态调整，灵活性高，可支持复杂路由；支持内存队列，读写极低延迟；支持多种协议。
适用于对实时性要求高但容忍消息丢失的场景，适用于需要复杂路由的场景。