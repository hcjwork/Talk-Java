消息被MQ服务端接收后保存下来，消费者得通过某种机制拿到消息，才能根据消息进行下一步的业务处理。
当然消费者获取消息也是要依靠MQ客户端来实现的。
MQ客户端和MQ服务端连接后，MQ客户端获取消息的方式理论上有两种：MQ客户端主动拉取消息，或由MQ服务端推送消息。
但实际上MQ服务端推送消息的方式一般不会被采用，因为MQ服务端不知道消费者的消费能力和消费节奏，
如果推送过于频繁或量过大就有可能压垮消费者。所以消息中间件都是以客户端主动拉取的方式来提供消息获取功能。
由引入了MQ客户端的消费者服务自行决定和调控消费节奏和消费能力，因此MQ客户端的配置通常支持有一次拉取消息数量、拉取时间间隔、
消费者并发线程数等多种辅助调控消费节奏和能力的关键参数。
像一些消息中间件通过注解方式监听消息变更，其本质其实也是拉取模式。
MQ客户端与MQ服务端建立超时长连接，MQ客户端定时向MQ服务端拉取最新消息，如果没有新消息则挂起请求直到超时关闭，
这样以长连接轮询方式进行优化，能提高消息拉取效率，同时以超时机制减少空轮询带来的资源开销。
MQ客户端主动拉取的模式，不仅可以按需控制消息的消费节奏，还可以主动提交消费偏移量，确保消息不丢失消费。

RabbitMQ的Channel的basicConsumer注册回调，实际是对拉取模式的封装，只是API的设计区别。

主流消息中间件的消费者获取消息示例。
1、Kafka
Kafka的消息消费者通过@KafkaListener注解获取消息，通过Acknowledgment（ack）可手动提交消费偏移量（offset）。
示例：
@Component
public class OrderConsumer {
    @KafkaListener(topics = "orders", groupId = "my-group")
    public void consume(String message, Acknowledgment ack) {
        try {
            // 模拟业务处理
            System.out.println("Received message: " + message);

            // 业务处理成功后手动提交offset
            ack.acknowledge();
        } catch (Exception e) {
            // 处理失败时不提交offset，消息会重新消费
            System.err.println("Process failed: " + e.getMessage());
        }
    }
}

2、RocketMQ
RocketMQ的消息消费者通过实现RocketMQListener接口，结合@RocketMQMessageListener注解实现消息的获取和消费。
示例：
@Slf4j
@Service
@RocketMQMessageListener(
        topic = "order-topic",
        consumerGroup = "order-consumer-group",
        selectorExpression = "*",  // 可以指定tag过滤
        consumeThreadNumber = 3
)
public class OrderMessageConsumer implements RocketMQListener<OrderMessage> {
    @Override
    public void onMessage(OrderMessage orderMessage) {
        log.info("收到订单消息: {}", orderMessage);
        // 处理业务逻辑
    }
}

3、RabbitMQ
RabbitMQ的消息消费者通过@RabbitListener注解获取消息，通过Channel提交消息消费的确认信息。
示例：
@Component
public class RabbitMQConsumer {
    @RabbitListener(queues = RabbitMQConfig.ORDER_QUEUE)
    public void processOrderMessage(OrderMessage orderMessage, Message message, Channel channel) throws IOException {
        try {
            System.out.println("收到订单消息: " + orderMessage);

            // 业务处理逻辑...

            // 手动确认消息
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            // 处理失败，拒绝消息并重新入队
            // channel.basicReject(message.getMessageProperties().getDeliveryTag(), true);

            // 处理失败，拒绝消息并进入死信队列
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), false);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.DEAD_LETTER_QUEUE)
    public void processDeadLetterMessage(OrderMessage orderMessage, Message message, Channel channel) throws IOException {
        System.out.println("收到死信消息: " + orderMessage);
        // 处理死信消息...
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }
}


