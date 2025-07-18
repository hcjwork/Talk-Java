在消息中间件系统中，消息的生产者生产消息后通过MQ客户端发送给MQ服务端，MQ服务端接收并保存消息，消息的消费者接收到
MQ服务端推送过来的消息，进行响应的处理后，主动提交消息消费位或通知MQ服务端已消费消息。

这是消息交互的基本流程，只是一种笼统的描述，在实际工程实践里，消息的业务类型多样，对消息的消费处理模式也有多种需求，
需要在消息的生产、发送、存储、消费等多个流程节点上提供更灵活或更细化的功能支持。
比如消息的存储，在订单业务这个大模块下有N个具体订单，每个订单的业务ID不同，通常需要按照具体订单来隔离独立存储相关消息，
即一个业务ID所关联的所有订单消息都存入一个独立区域，不同业务ID的订单消息存储相互隔离，这样不同业务ID的消息可以并行处理。
这不仅提高了消息的吞吐量，在独立区域这一层面还能更好地保证消息的逻辑顺序性。

这个独立隔离区域就是在消息主题下的子概念，通常被称为队列或分区。消息被存储在不同的队列或不同的分区中。
所以消息的元数据部分一般涵盖所属主题、所属队列或分区等信息。
因此主题和队列或分区就往往有映射关系，某个队列或分区存储的就是某个主题的消息。

在消息的生产消费方面有两种模式，一种是点对点（Point to Point）模式，一种是发布-订阅（Publish-Subscribe）模式。
看着这些名词有时候会有些懵，其实就是一对一和一对多，说白了就是消息生产和消费消费的对应关系。
点对点是指：一条消息只被唯一的消费者所消费；
发布-订阅是指：一条消息可以被多个消费者所消费。
所以说是点对点模式还是发布-订阅模式，与主题和队列或分区的数量关系是无关的，这两种模式说的是消息生产与消费的模式，
或者更直白点，说的消息和消费者数量的关系。

点对点模式不用说消息中间件肯定要支持，一条消息至少也要被一个消费者所消费，这是消息系统的最基本需求。
至于为什么要提供发布-订阅模式，因为有些业务场景需要对消息做多种不同的处理，比如对于一批消息要进行实时计算、统计分析、
日志留存等多种处理，这几种工作是需要独立进行的，但又是基于同样的消息进行处理，如果是点对点模式，只能支撑其中一个工作完成，
这自然是不够的，因此发布-订阅模式不可或缺。

主流的消息中间件对于点对点模式和发布-订阅模式是都支持，通过消费者的配置可调整消费者的数量，
例如Kafka可以通过设置消费组及消费者的唯一性，使得消息只会被唯一的消费者所消费。

