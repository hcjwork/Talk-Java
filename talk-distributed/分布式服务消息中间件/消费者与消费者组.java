消息由某个服务通过MQ客户端发送给MQ服务端，而另一个服务获取消息然后进行相应处理，处理完成后通过MQ客户端提交消息的消费位置，
或者通知MQ服务端消息已消费，由MQ服务端自行修改消息的消费状态。
在这个流程里，消息最新发送的服务称为消息的生产者，最后根据消息进行业务处理的服务称为消息的消费者。

一条消息由一个生产者创建后，发给一个MQ服务，一个消费者获取这条消息（被动接收或主动拉取）进行下一步处理，
处理完后由MQ服务标识消息已被消费。
这条消息的交互链路是以消费者只对消息做一种处理为前提，假如消费者需要根据这条消息独立隔离执行多种不同的处理，
那么对于这条消息来说，在逻辑上有不同组的消费者都想来消费，这就是消费者组的概念。
有时候的需求是需要对消息做独立的不同的处理，比如对于一批消息要进行实时计算、统计分析、日志留存等多种操作，
这些操作是不同维度的业务需求。
消费者组其实就是发布-订阅这种生产消费模式，多个消费者位于不同的消费者分组，消费同一主题下的消息，
每个消费者组都独立消费全量消息。

像Kafka和RocketMQ都有消费者组的概念，消费者监听消息主题时可以不指定消费者组ID，MQ服务会自动创建临时消费者组，
但这种操作不建议，因为临时消费者组无法保存消费进度，也可能导致消息重复消费或丢失。
所以工程实践中应当要指定唯一的消息费者组ID。

通过消费者组和消费者并发线程的设置，可以实现点对点模式和发布-订阅模式的切换。