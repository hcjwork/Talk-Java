消息的生产与发送时消息交互流程的第一个关键阶段，生产消息的业务服务通过MQ客户端发送消息给MQ服务端。
消息的生产工作主要包含消息内容实体的定义、消息内容数据准备，这个工作其实就是业务处理和准备工作。
消息的发送则需要依赖于MQ客户端，所以需要使用消息的业务服务内要先引入MQ客户端依赖。
MQ客户端与MQ服务实例进行连接，消息生产者以MQ客户端工具发送具体消息给MQ服务进行保存。

MQ的客户端创建通常有配置和代码两种方式，比如SpringBoot配置和Config类。当然也可以两者结合，依实际需求灵活组合即可。
如果是SpringBoot项目，一般引入的是Spring与具体MQ客户端的整合依赖，例如：
对于Kafka客户端引入的是spring-kafka依赖；
对于RocketMQ客户端引入的是rocketmq-spring-boot-starter依赖；
对于RabbitMQ客户端引入的是spring-boot-starter-amqp依赖。
如果不想引入与Spring的整合依赖，也可以使用MQ客户端的原生依赖，例如：
对于Kafka客户端引入的是kafka-clients依赖；
对于RocketMQ客户端引入的是rocketmq-client依赖；
对于RabbitMQ客户端引入的是amqp-client依赖。

如果是引入的Spring整合依赖，消息生产者或消息消费者可以使用Spring封装好的Template工具来操作MQ客户端，例如：
对于Kafka可以使用KafkaTemplate操作客户端；
对于RocketMQ可以使用RocketMQTemplate操作客户端；
对于RabbitMQ可以使用RabbitMQTemplate操作客户端。
如果是引入的MQ客户端原生依赖，则需要手动创建MQ客户端。

消息的生产其实没什么需要讨论的，根据实际需求定义消息内容实体类并准备好数据即可，这部分也是通用的，并不与具体MQ进行绑定。
但是消息的发送则因具体的MQ客户端有所差异。
有些MQ客户端支持直接指定队列或分区，有些只能指定队列选择器。
以Kafka、RocketMQ、RabbitMQ三种主流的MQ为例，对比其在消息发送时的具体区别。
1、Kafka
// 指定消息内容，不指定主题，不指定分区，不指定路由键，
kafkaTemplate.send(MessageBuilder.withPayload(message).build());
// 指定消息内容，指定主题，不指定分区，不指定路由键
kafkaTemplate.send("my-topic", message);
// 指定消息内容，指定主题，指定路由键，不指定分区
kafkaTemplate.send("my-topic", "route-key", message);
// 指定消息内容，指定主题，指定路由键，指定分区
kafkaTemplate.send("my-topic", 1, "route-key", message);
// 指定消息内容，指定主题，指定路由键，指定分区，指定发送时间
kafkaTemplate.send("my-topic", 1, System.currentTimeMillis(), "route-key", message);

Kafka的分区编号是从0开始，比如有10个分区，分区编号为0~9。
工程实践通常优先用路由键进行消息路由，避免硬编码分区号。

2、RocketMQ
// 指定消息内容，指定主题
rocketMQTemplate.syncSend("my-topic", message);
rocketMQTemplate.syncSend("my-topic", MessageBuilder.withPayload(message).build());
// 指定消息内容，指定主题，指定路由键
rocketMQTemplate.syncSendOrderly("my-topic", message, "route-key");
// 不能指定队列，但可以指定队列选择器
rocketMQTemplate.setMessageQueueSelector((queues, msg, arg) -> {
    int index = Math.abs(arg.hashCode()) % queues.size();
    return queues.get(index);
});

3、RabbitMQ
// 指定消息内容
rabbitTemplate.convertAndSend(message);
// 指定消息内容，指定路由键
rabbitTemplate.convertAndSend("my-key", message);
// 指定消息内容，指定路由键，指定队列
rabbitTemplate.convertAndSend("my-exchange", "my-key", message);

RabbitMQ不支持指定消息主题，因为消息主题已在队列创建时绑定，指定队列就相当于指定主题。


