MQ事务消息来实现分布式事务，这种方案要依赖于MQ本身支持事务消息，例如Kafka、RocketMQ等。
其实就是利用MQ的半消息确认机制来进行消息的两阶段提交。

MQ的事务消息基于半消息确认机制，是为了配合分布式事务的ACID特性而设计的，可以进行配置。
如果配置了消息的事务特性，生产者发送给MQ的消息在提交到指定的主题前会先暂存MQ的内部主题中，
这些消息对于消费者不可见，只有MQ从内部主题提交到消费主题中时，消费者才能读取。
所以MQ的消息其实是有两个提交阶段，第一个阶段是提交到MQ的内部主题中，第二阶段是提交到MQ的消费主题中。
开启消息事务特性时，如果第一阶段也就是生产者发送消息给MQ这一步失败，就会回滚消息，不会把消息提交消费主题中。

如果不想要事务特性，在MQ配置中取消或不配置事务特性即可，这样的话生产者发送给MQ的消息会直接提交到消费主题中，消费者直接可见。

为了让生产者的业务操作和消息发送操作组合起来具有原子性，通常需要@Transactional和MQ的事务消息配置结合起来实现。
例如使用Kafka事务消息时，就需要通过@EnableKafka开启Kafka配置，并在Kafka配置文件中定义KafkaTransactionManager，
并在生产者中通过@Transactional的transactionManager属性应用KafkaTransactionManager实例，
这样只要生产者的业务操作方法抛出异常，业务操作的数据库事务和Kafka的消息事务就都会回滚。
这样可以解决生产者业务操作回滚而Kafka消息却依旧发送成功的问题。

示例：
// kafka配置中开启kafka事务管理；定义Kafka事务管理器
@Configuration
@EnableKafka
public class KafkaConfig {
    // Kafka事务管理器
    @Bean
    public KafkaTransactionManager<String, Object> kafkaTransactionManager() {
        return new KafkaTransactionManager<>(producerFactory());
    }
}
// 生产者发送消息和业务操作在同一个方法中，通过@Transactional的transactionManager引用Kafka事务管理器
// 给Kafka消息发送（生产者发送给MQ）添加回调方法，发送失败时抛出异常让Kafka事务管理器捕捉到，回滚Kafka事务
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    @Transactional(transactionManager = "kafkaTransactionManager")
    public void createOrder(OrderRequest request) {
        kafkaTemplate.send("inventory-topic", inventoryEvent)
        .addCallback(
            success -> log.info("库存事件发送成功"),
            ex -> {
                log.error("库存事件发送失败", ex);
                throw new RuntimeException("消息发送失败", ex);
            }
        );
    }
}