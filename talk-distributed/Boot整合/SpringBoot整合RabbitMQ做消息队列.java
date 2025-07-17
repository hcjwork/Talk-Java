RabbitMQ是Apache开源的消息队列组件。

一、RabbitMQ服务启动
版本选择需要考虑与java和spring-boot的兼容性，兼容java8和springboot2.x的RabbitMQ服务端至多能
选用到3.8.35版本，3.9及以上版本至少需要java11或springboot3。

RabbitMQ服务端3.8.35版本下载地址：
https://github.com/rabbitmq/rabbitmq-server/releases?expanded=true&page=3&q=3.8

另一个需要注意的点是，RabbitMQ是使用Erlang语言开发的，需要安装Erlang语言环境，且需要与所选的RabbitMQ服务段版本兼容才行。
兼容RabbitMQ3.8.35的Erlang版本范围是23.2~24.2，其他版本与Erlang版本匹配关系可查看以下链接：
https://www.rabbitmq.com/docs/which-erlang。

我这里选择Erlang24.0版本，下载地址为：https://www.erlang.org/patches/otp-24.0。
可选zip包或安装包，解压或安装后，配置ERLANG_HOME环境变量，配置好通过执行以下erl命令可以测试erl安装是否成功。

RabbitMQ服务安装好后，添加环境变量RABBITMQ_SERVER

进入RabbitMQ/sbin目录下，运行rabbitmq-service.bat。
启动报错“服务名无效”，出现这个错误一般是因为RabbitMQ服务没有注册或没有注册成功。

通过执行“sc query”命令可以查看已经注册的服务，例如：
sc query | findstr RabbitMQ
可以查看是否有RabbitMQ服务。

如果是开发测试环境，暂时搞不定，可以执行rabbitmq-server.bat跳过服务直接启动。

RabbitMQ服务端启动时其实会启动AMQP和控制台两个服务，
控制台的默认访问地址是：http://localhost:15672/，默认用户名和密码是：guest/guest。
而AMQP服务的默认端口是5672。
所以在启动RabbitMQ前最好先检查下5672和15672两个端口是否被占用，当然可以修改对应的jar包中的配置文件来调整启动端口。

AMQP就是具体提供消息功能的服务。

二、Maven依赖
1、rabbitmq核心依赖
rabbitmq依赖需要引入的就是rabbitmq客户端的依赖，通常有两种选择，一种是与spring-boot整合的starter依赖，
一种是rabbitmq客户端的独立依赖。

starter依赖示例如下：
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
    <version>2.7.18/version>
</dependency>

rabbitmq-client依赖示例如下：
<dependency>
    <groupId>com.rabbitmq</groupId>
    <artifactId>amqp-client</artifactId>
    <version>5.15.0</version>
</dependency>
注意：与springboot2兼容的得在5.16版本以下，最高可选到5.15.0版本。

2、其他基础依赖
spring-boot-starter-parent、
spring-boot-starter-web

三、SpringBoot配置
通过spring.rabbitmq属性进行配置，yml示例如下：
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
    virtual-host: /
    # 开启发送方确认机制
    publisher-confirm-type: correlated
    # 开启发送方回退机制
    publisher-returns: true
    listener:
      simple:
        # 消费者最小数量
        concurrency: 1
        # 消费者最大数量
        max-concurrency: 10
        # 每次从队列中获取的消息数量
        prefetch: 1
        # 消费失败自动重新入队
        default-requeue-rejected: false
        # 手动确认消息
        acknowledge-mode: manual

四、关键注解
1、@EnableRabbit
开启RabbitMQ消息队列功能。用在启动类或配置类上。

2、@RabbitListener
消息监听。用在消息消费者类中。例如：
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
    }
}

关键属性：queues
queues：指定队列名称。

五、机制原理
1、消息生产和发送
消息生产者通过RabbitTemplate实例发送消息。具体是调用RabbitTemplate实例的convertAndSend方法。
例如：（OrderMessage是自定义的消息实体）
public void sendOrderMessage(OrderMessage orderMessage) {
    CorrelationData correlationData = new CorrelationData(UUID.randomUUID().toString());
    rabbitTemplate.convertAndSend(
            RabbitMQConfig.ORDER_EXCHANGE,
            RabbitMQConfig.ORDER_ROUTING_KEY,
            orderMessage,
            correlationData
    );
}
convertAndSend主要有4个关键参数：交换机名称、绑定路由键、具体的消息、消息的全局ID。
convertAndSend方法没有提供“队列名称”参数，这是因为RabbitMQ的队列、交换机需要在RabbitMQConfig类中通过Bean的方式先创建好，
并且通过路由键绑定队列和交换机，这样只需要交换机名称和绑定路由键就能定位到唯一的队列。
例如：
@Bean
public Queue orderQueue() {
    return QueueBuilder.durable(ORDER_QUEUE)
            .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE) // 死信交换机
            .withArgument("x-dead-letter-routing-key", DEAD_LETTER_ROUTING_KEY) // 死信路由键
            .build();
}
// 创建直连交换机
@Bean
public DirectExchange orderExchange() {
    return new DirectExchange(ORDER_EXCHANGE);
}
// 绑定队列和交换机
@Bean
public Binding bindingOrder() {
    return BindingBuilder.bind(orderQueue()).to(orderExchange()).with(ORDER_ROUTING_KEY);
}

2、消息的消费和拉取
RabbitMQ的消息消费者通过@RabbitListener注解以及注解的queues属性，监听指定队列的消息。

3、消息的提交和确认

六、核心组件
Queue：队列；
Exchange：交换机；
Channel：通道。

一个队列通过唯一的路由键绑定一个交换机，一个交换机对应一个通道。

