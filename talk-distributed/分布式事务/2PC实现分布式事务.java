无论是2PC方案还是3PC方案，其基础都是数据库的XA协议，对于不支持XA协议的数据库来说，2PC或3PC是无法保证分布式事务的ACID特性的。

2PC，Two-Phase-Commit，意为两阶段提交。
整个事务控制分为准备阶段（preparing phase）和提交阶段（committing phase）。
所以参与者（也就是各个子事务）全部都准备好可以提交了，整个分布式事务才开始提交。
只要有一个参与者没有准备好，整个分布式事务就不进行提交。

每个参与者都负责自己的事务的准备和提交，那么整体的分布式事务是谁来提交呢？
可见除了参与者（Participant）外，应该还有协调者（Coordinator）的角色。
这个协调者负责检查所有子事务是否准备妥当，根据子事务的准备情况抉择整体事务是否提交。

2PC是一种强一致性的分布式事务实现方案，所有的子事务要么全部提交、要么全部回滚，适合对一致性要求高的业务，比如金融交易。

每个子事务在准备阶段只是记录XA事务日志和执行业务SQL，但此时不提交也不回滚，
等到协调者检查子事务准备状态时，如果发现有子事务准备失败的，就将所有子事务全部回滚，
如果全都准备成功了，则让所有子事务都进行提交。

在准备阶段，是所有子事务做好XA日志记录、执行完SQL语句，但不提交也不回滚，如果顺利完成就表示准备成功，如果有异常就表示准备失败。
在提交阶段，其实是分提交和回滚两种情况的，一旦有子事务准备失败就全部回滚，一旦所有子事务都准备成功就全部提交。

在2PC方案里，参与者就是对每个子事务处理的包装器，需要封装自己的prepare、commit、rollback方法。
协调者先检查每个参与者的准备状态，也就是挨个调所有参与者的prepare方法，一旦出现准备失败的，就逐个调所有参与者的rollback方法，
如果都准备成功了，则逐个调所有参与者的commit方法提交事务。

实际生产中如果要使用2PC方案实现分布式事务，通常采用高效的工具库来操作落地，比如Atomikos、Narayana等。
如果采用Atomikos库，Maven依赖如下：
<dependency>
    <groupId>com.atomikos</groupId>
    <artifactId>transactions-jta</artifactId>
    <version>6.0.0</version>
</dependency>
如果采用Narayana库，Maven依赖如下：
dependency>
    <groupId>org.jboss.narayana.jta</groupId>
    <artifactId>narayana-jta</artifactId>
    <version>7.2.2.Final</version>
    <scope>compile</scope>
</dependency>




