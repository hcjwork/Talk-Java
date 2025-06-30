TCC，Try-Confirm-Cancel，尝试、确认或取消，这是一种补偿型的分布式事务实现方案。
涉及三个阶段：
Try阶段，尝试执行业务处理，做好必要的检查，预留出必要的资源。
Confirm阶段，确认执行业务同时提交。
Cancel阶段，取消执行业务，释放Try阶段预留的资源。

思考如下问题：
Try阶段检查些什么？
检查是否有足够的资源可以使用，如果不够则结束业务处理。
这里的资源都是些什么，库存、余额、余量等。
也可以是用户权限、api调用剩余次数、证书有效时间、截止日期等。
反正就是一些前置检查要放到这个阶段来做。

Try阶段资源是怎么个预留法？

Confirm阶段的真正提交是提交什么？

Cancel阶段取消执行业务是怎么取消的？

Cancel阶段释放预留资源是怎么释放的？


资源预留能增加可供调整的灵活性，比如订单和支付可能会间隔比较长的时间，下了订单后支持用户立即支付，也支持用户在一定时间做订单取消调整。
但TCC方案不仅对业务代码的侵入性比较大，对数据库表也有侵入，需要类似于冻结量这种字段来提供预留和回退的基础。
如果不想对数据库表有入侵，可以用一张日志表记录TCC操作日志，尤其是Try阶段时，记录操作日志，但是不对业务数据做修改，
等到Confirm阶段，将操作日志和业务修改一起提交；而对于Cancel阶段，只需要删除或修改操作日志即可，因为Try阶段没有修改数据。
那么Try阶段的检查就要有足够的健壮性，得保证检查无误后Confirm阶段能顺利执行。

假设有三个方法为try()、confirm()、cancel()分别对应Try、Confirm、Cancel阶段的处理，
为了保证三个阶段处理的是一个TCC事务，需要提前生成好一个全局唯一的事务id即xid。
@Transactional
public void try() {
    // 充分的检查，比如是否有权限、是否有足量库存、是否有余额等，所有可能会导致Confirm阶段无法进行的业务因素都要检查
    // 检查没问题了，准备TCC操作日志，先根据xid查一下，如果查到了且操作状态为"Try"，说明已经执行过了，不必重复执行
    // 如果没查到，或是操作状态为"Confirm"或"Cancel"，说明上一套流程已经结束
    // 可以重新记录TCC操作日志，将业务类型和关键的业务数据字段体现出来，方便后续回溯；记录xid；记录操作状态为"Try"
    // 记住不在Try阶段修改业务数据
}
@Transactional
public void confirm() {
    // 根据xid查找对应的TCC操作日志，如果没查到说明还没有执行Try阶段，抛出异常或做其他结束处理
    // 如果根据xid查到了TCC操作日志，先检查操作状态是不是"Try"，如果不是，表示Try阶段还没执行，结束处理
    // 如果根据xid查到了TCC操作日志，查操作状态是"Try"，表示接下来可以执行Confirm阶段的处理
    // 这个时候再修改业务数据，同时修改TCC操作状态为"Confirm"。将两个修改放在同一本地事务中提交。
}
@Transactional
public void Cancel() {
    // 根据xid查找对应的TCC操作日志，如果没查到说明还没有执行Try阶段，可以直接结束
    // 如果根据xid查到了TCC操作日志，先检查操作状态是什么
    // 如果操作状态是"Confirm"，可以直接结束
    // 如果操作状态是"Try"，修改操作状态为"Cancel"或是删除掉TCC操作日志。
    // 而对于业务数据，因为Try阶段没有修改，Cancel阶段不做任何改变
}

处理的逻辑要统一，统一出入口，还要给分布式事务的关键步骤加锁，保证这一套流程的结果符合预期。
分布式事务中的每个参与者都需要提供好try()、confirm()、cancel()方法以供协调者调用。
这就注定了要对业务代码进行侵入式设计，原本一个本地事务做了必要的检查和业务数据变更就行，
现在为了与其他本地事务协作，就需要都实现某种共同特性，这种特性是跨本地事务的，然后由
协调者来统一调配这种共同特性，使所有本地事务得以通过执行各自职责部分而实现跨越本地事务的分布式事务。

实际工程中，不同业务一般会有自己独立的微服务，比如订单服务、支付服务等。
这样跨库的分布式事务同样也是跨JVM的，如果采用TCC方案实现分布式事务，协调者可能会定义在前置流程的服务里，
比如先下订单后支付，协调者就定义在订单服务中，这种分布式事务涉及订单服务、支付服务，更细点的支付服务还能还会拆出库存服务等。
假设一个分布式事务只涉及到订单服务和支付服务，采用TCC方案实现，订单服务要体现订单这边的参与者，还要体现协调者，
而对于支付服务的参与者，可能就是通过远程过程调用方式去调支付服务了，例如Feign接口、Dubbo调用等。

为什么不定义一个包含try/confirm/cancel方法的接口，让所有的参与者都实现？
假设OrderService和PayService都实现了包含有try/confirm/cancel方法的接口TCCPhaseWork，
如果又有一个订单业务也涉及到分布式事务，那这个业务的TCC的try/confirm/cancel方法写在哪里，
肯定也是要放在OrderService里的，但已经实现了TCCPhaseWork接口，已经有了一套try/confirm/cancel方法，
TCC的协调者只能调用到这一套try/confirm/cancel方法，无法进行业务区分了。
所以TCCPhaseWork不强制业务Service实现，就是要让业务Service保持灵活性，
业务A的可以定义为tryA/confirmA/cancelA方法，业务B的可以定义tryB/confirmB/cancelB方法，
而协调者在收集TCCPhaseWork时，可以通过直接匿名函数实现，按需调用tryA或tryB即可。
如果想保持TCC协调者的职责的单一性，还可以把TCCPhaseWork的匿名实现工作抽离出来用专门的类负责。

例如：
TCCManager专门做TCCPhaseWork实例的try、confirm、cancel执行；
TCCPlaceService专门做订单服务、支付服务的TCCPhaseWork实例创建；
OrderService包含订单相关分布式业务的try/confirm/cancel方法实现；
PayService包含支付相关分布式业务的try/confirm/cancel方法实现；
然后TCCPlaceService中可能会通过远程调用PayService的try/confirm/cancel方法，以组装支付服务的TCCPhaseWork实例。

TCC方案比2PC方案更加复杂，对代码的侵入性也更高，对一致性的保证不如2PC，如果实际生产需要使用TCC方案，
通常都是采用成熟的TCC框架如Seata、ByteTCC、Hmily等，推荐使用Seata，Seata支持HTTP/RPC/Dubbo等多种协议，
有比较完善的可视化界面，支持AT/TCC/Saga等多种分布式事务模式。
当然了，如果对性能有高要求可采用Hmily，想与SpringCloud直接配套可采用ByteTCC。
ByteTCC是基于SpringCloud的分布式事务补偿方案实现，自动生成TCC接口，且支持嵌套事务。
Hmily在高并发场景有更好的性能，支持事务日志异步存储，SPI扩展接口也很丰富。

