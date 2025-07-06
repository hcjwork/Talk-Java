Sentinel是阿里开源的熔断降级组件，有非常高的社区活跃度，熔断、降级、限流、系统保护等处理支持灵活配置和更细粒度的控制，
在高并发场景下的熔断降级性能和监控性能很可观，可视化界面也比较成熟，对云原生和网格服务也有较好支持，

一、Maven依赖
1、Sentinel核心依赖
Maven依赖示例如下：
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-sentinel</artifactId>
    <version>2021.1</version>
</dependency>

2、Actuator监控
Maven依赖如下：
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
    <version>2.7.18</version>
</dependency>
需要监控sentinel端点。

Actuator监控默认访问地址：http://服务ip:服务port/actuator/监控端点
例如：http://localhost:8087/actuator/sentinel


4、其他基础依赖
spring-cloud-alibaba-dependencies、
spring-cloud-dependencies、
spring-boot-starter-parent、
spring-boot-starter-web

二、SpringBoot配置
1、熔断规则配置
基于spring.cloud.sentinel.degrade.rules属性进行配置，支持多个熔断规则定义，实际应用时选择其中一个即可。
以.yml格式为例：
spring:
  cloud:
    sentinel:
      degrade:
        rules:
          - resource: degradeRule1 # 资源名
            grade: EXCEPTION_RATIO # 熔断策略：EXCEPTION_RATIO(异常比例)/RT(慢调用)/EXCEPTION_COUNT(异常数)
            count: 0.5 # 阈值：异常比例阈值(0.5=50%) 或 慢调用RT(ms)
            timeWindow: 10 # 熔断时长(秒)
            minRequestAmount: 5 # 触发熔断的最小请求数
            statIntervalMs: 10000 # 统计时间窗口(ms)
          - resource: degradeRule2 # 资源名
            grade: EXCEPTION_RATIO # 熔断策略：EXCEPTION_RATIO(异常比例)/RT(慢调用)/EXCEPTION_COUNT(异常数)
            count: 0.5 # 阈值：异常比例阈值(0.5=50%) 或 慢调用RT(ms)
            timeWindow: 10 # 熔断时长(秒)
            minRequestAmount: 5 # 触发熔断的最小请求数
            statIntervalMs: 10000 # 统计时间窗口(ms)

参数解析
resource：自定义资源名称。
grade：熔断策略，支持三种策略，EXCEPTION_RATIO(异常比例)、RT(慢调用)、EXCEPTION_COUNT(异常数)。
count：阈值，异常比例阈值(0.5=50%) 或 慢调用RT(ms)
timeWindow：熔断时长，单位秒
minRequestAmount：触发熔断的最小请求数
statIntervalMs：统计时间窗口，单位毫秒

2、降级规则配置
yml示例：
spring:
  cloud:
    sentinel:
      # 降级规则（通过@SentinelResource配置）
      scg:
        fallback:
          enabled: true
这个配置不加也没关系，关键是要通过@SentinelResource注解的fallback属性指定降级方法。
降级方法的参数与@SentinelResource注解的原方法一致，此外还要多加一个Throwable参数。

3、限流规则配置
基于spring.cloud.sentinel.flow.rules属性进行配置，支持多个限流规则定义，实际应用时选择其中一个即可。
yml示例：
spring:
  cloud:
    sentinel:
      # 限流规则
      flow:
        rules:
          - resource: flowRule1  # 资源名
            limitApp: default      # 来源应用(default表示所有)
            grade: QPS             # 限流类型(QPS/线程数)
            count: 100             # 阈值
            strategy: DIRECT       # 流控模式(DIRECT/ASSOCIATE/CHAIN)
            controlBehavior: RATE_LIMITER  # 控制效果(直接拒绝/WARM_UP/匀速排队)
            burst: 10              # 突发流量额外允许的QPS
            maxQueueingTimeMs: 500 # 排队等待时间(ms)

关键参数
resource：自定义资源名。
limitApp：请求来源应用(default表示所有)，请求来源白名单。
grade：限流类型，支持两种类型：QPS、线程数。
count：阈值。
strategy：流控模式，支持三种模式：DIRECT、ASSOCIATE、CHAIN。
controlBehavior：控制效果，有三种选型：直接拒绝、WARM_UP、匀速排队。
burst：突发流量额外允许的QPS。
maxQueueingTimeMs：排队等待时间，单位毫秒。

4、热点参数限流配置
yml示例：
spring:
  cloud:
    sentinel:
      # 热点参数规则
      param-flow:
        rules:
          - resource: getUserById  # 资源名
            grade: QPS              # 限流类型
            paramIdx: 0             # 参数索引(0表示第一个参数)
            count: 10               # 单参数阈值
            durationInSec: 1        # 统计窗口(秒)
            burstCount: 5           # 突发流量额外配额
        # 特殊参数单独配置
            paramFlowItemList:
              - object: "101"       # 参数值
                count: 50          # 该参数值的独立阈值
              - object: "102"
                count: 30

4、系统保护规则配置
yml示例：
spring:
  cloud:
    sentinel:
      # 系统保护规则
      system:
        rules:
          - highestSystemLoad: 2.0   # 最大系统Load
            highestCpuUsage: 0.8      # CPU使用率阈值(0.0~1.0)
            avgRt: 500                # 平均响应时间(ms)
            maxThread: 50             # 最大并发线程数
            qps: 1000                # 入口总QPS


5、sentinel控制台配置
sentinel-dashboard的jar包集成了3种不同的服务角色，对应不同的端口和功能。
控制台Web服务：提供可视化界面，端口默认8080，可以修改，也可以在“java -jar”命令执行指定启动端口。
HTTP API 服务：提供RESTful API（规则配置、监控数据获取），端口默认8080，其实就是控制台Web服务提供的功能。
Transport Command Center：与客户端应用通信的心跳检测和规则推送服务（基于Netty），端口默认8719，
配置在sentinel客户端这边，比如application.yml中。
例如：
spring:
  cloud:
    sentinel:
      transport:
        dashboard: localhost:8080 # Sentinel控制台地址，与sentinel-dashboard包的启动ip和port一致
        port: 8719 # 客户端与sentinel心跳监测和规则推送服务的通信端口

windows中cmd命令查看端口是否占用：netstat -ano | findstr 端口号

在客户端所属服务的application.yml中将spring.cloud.sentinel.transport.port属性的值
指定为sentinel规则推送服务的启动端口，就可以将sentinel控制台配置的规则推到客户端。

但sentinel-dashboard和sentinel客户端之间，只能是由dashboard单向推送规则到客户端，客户端配置的规则并不会在dashboard页面展示。
dashboard控制台页面配置的规则在客户端重启后会清除，所以如果想要dashboard配置的规则持久性存在，需要结合Nacos做持久化存储。

Dashboard是操作入口，Nacos是持久化中间层（写数据库的），客户端是执行终端。
对应配置的动态调整应始终在Dashboard上操作，避免在Nacos中修改。

6、nacos同步sentinel配置
application.yml中配置的规则是本地静态配置，如果想要在sentinel-dashboard页面同步显示，就需要结合nacos进行同步。
yml示例：
spring:
  application:
    name: sentinel-demo
  cloud:
    sentinel:
      datasource:
        # 自定义名称，流控规则数据源
        flow:
          nacos:
            server-addr: localhost:8848
            data-id: ${spring.application.name}-flow-rules
            username: nacos
            password: nacos
            namespace: SENTINEL_GROUP
            group-id: SENTINEL_GROUP
            rule-type: flow
            data-type: json

7、actuator监控配置
# 顶格写
management:
  endpoints:
    web:
      exposure:
        include: health,sentinel

8、Nacos动态规则配置
spring:
  cloud:
    sentinel:
      datasource:
        # Nacos动态规则配置
        flow-nacos:
          nacos:
            server-addr: localhost:8848
            dataId: sentinel-flow-rules
            groupId: SENTINEL_GROUP
            rule-type: flow
        degrade-nacos:
          nacos:
            server-addr: localhost:8848
            dataId: sentinel-degrade-rules
            rule-type: degrade

三、关键注解
1、@SentinelResource
Sentinel的核心注解，可以指定规则资源名称、熔断和限流时的降级处理、业务异常时的降级处理等。
可以用在类声明或方法声明上，用在类上，类中所有的方法都会应用到同一种规则，用在方法上则针对具体方法进行规则应用。
关键属性：value、blockHandler、fallback、exceptionsToIgnore。

value：指定资源的唯一标识，自定义的名称，如果指定的是一个未配置的规则资源，则规则采用默认值。
如果想要指定已配置好的资源，需要与Config配置类或SpringBoot配置中的资源名称保持一致，否则不会按预期生效。

blockHandler：指定熔断和限流时的降级方法。当处于熔断状态或限流状态时，未被允许通信的请求会抛出BlockException。
熔断状态下的请求调用会抛出DegradeException，限流状态下的请求调用会抛出FlowException，
DegradeException和FlowException都是BlockException的子类。
blockHandler方法与原方法参数一致，还要多加一个BlockException参数。例如：
public String blockHandler(String userId, BlockException ex) {
    return "[Blocked] User-" + userId + ", reason: " + ex.getClass().getSimpleName();
}

fallback：请求出现业务异常时执行的降级方法。fallback方法与原方法参数一致，此外还要多加一个Throwable参数，例如：
public String fallbackHandler(String userId, Throwable t) {
    return "[Fallback] User-" + userId + ", error: " + t.getMessage();
}

exceptionsToIgnore：指定要忽略的业务异常，当出现指定的异常时，不会执行降级方法，会被Sentinel忽略。

2、@SentinelRestTemplate
对RestTemplate的调用自动启用Sentinel保护。适用于HTTP接口调用的熔断和降级场景。

四、原理机制
1、熔断
基于熔断状态机+滑动窗口统计实现熔断控制。
熔断状态包含三种状态：CLOSED、OPEN、HALF_OPEN。
Closed（关闭状态）：正常状态，所有请求都允许通过。
Open（打开状态）：熔断状态，所有请求被快速拒绝。
Half-Open（半开状态）：试探恢复状态，允许部分请求通过。

熔断触发条件有三种：慢调用比例、异常比例、异常数。
慢调用比例（Slow Request Ratio）：当请求响应时间超过设定阈值且慢调用比例达到阈值时触发。
异常比例（Error Ratio）：当请求异常比例超过阈值时触发。
异常数（Error Count）：当请求异常数超过阈值时触发。

熔断状态三种状态转换情况：
- 熔断触发后会进入熔断开启状态，持续一个设定的熔断时长。熔断时长通过timeWindow参数配置。
- 熔断时长结束后进入半开状态
- 半开状态下如果接下来的一个请求调用成功则关闭熔断器，否则继续保持熔断重新进入打开状态。

2、降级
Sentinel提供多种降级规则：平均响应时间、异常比例、异常数。
RT（平均响应时间）：当1s内持续进入5个请求，且平均响应时间超过阈值，触发降级。
异常比例：当每秒异常比例超过阈值，触发降级。
异常数：当每分钟异常数超过阈值，触发降级。

Sentinel的降级支持两种模式的降级处理：熔断限流、业务异常。
通过@SentinelResource注解的blockHandler和fallback属性分别可指定。
（当然blockHandlerClass和fallbackClass属性也可以）
blockHandler属性指定熔断或限流时的降级方法，fallback属性指定出现业务异常时的降级方法。
熔断限流降级方法参数与原方法一致，此外还需要多加一个BlockException参数；
业务异常降级方法参数与原方法一致，此外还需要多加一个Throwable参数。

3、限流
支持两种限流模式：基于QPS、基于并发线程数。
限流策略有三种：直接限流、关联资源限流、链路限流。
DIRECT：直接限流，是默认限流策略。
ASSOCIATE：关联资源限流。
CHAIN：链路限流。

流控行为：直接拒绝、预热启动、匀速排队。
DEFAULT：直接是直接拒绝请求，直接抛出FlowException异常。
WARM_UP：预热启动。
RATE_LIMITER：匀速排队，不抛异常，而是进入等待队列，等待放行。

五、面板集成
先通过“java -jar”命令运行sentinel-dashboard-x.y.z.jar包，启动sentinel控制台和sentinel规则推送服务。
可以在“java -jar”命令中指定sentinel的控制台Web服务和规则推送服务的启动端口。
控制台Web服务端口由参数“-Dserver.port”指定，例如：-Dserver.port=8080。
规则推送服务端口由参数“-Dsentinel.transport.server.port”指定，例如：-Dsentinel.transport.server.port=8723。
控制台Web服务端口默认是8080，规则推送服务端口默认是8719。
如果Sentinel客户端的配置中没有指定transport.port属性的值，默认与推送服务的通信端口为8719，如果端口被占用，则递增端口。
为了让Sentinel控制台上配置的规则能正常推送到Sentinel客户端（比如SpringBoot服务），
需要transport.port属性配置的端口与推送服务的启动端口一致。

完整“java -jar”命令示例如下：
java -Dserver.port=8080 -Dsentinel.transport.server.port=8723 -jar sentinel-dashboard-1.8.8.jar

如果要结合Nacos做规则的持久化存储，启动命令可能要添加Nacos相关的命令参数，例如：
java -Dserver.port=8080 \
     -Dnacos.addr=localhost:8848 \          # Nacos 地址
     -Dnacos.namespace=public \             # 命名空间ID
     -Dsentinel.dashboard.auth.username=nacos \  # Nacos 用户名（若开启认证）
     -Dsentinel.dashboard.auth.password=nacos \
     -jar sentinel-dashboard-1.8.8.jar


六、关键源码
1、com.alibaba.csp.sentinel.slots.statistic.base.LeapArray
滑动窗口基本结构。
2、StatisticSlot
慢调用统计。
3、DegradeRuleManager
异常统计。
4、ErrorCounterLeapArray
异常数统计。