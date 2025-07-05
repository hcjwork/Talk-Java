Hystrix在2.x版本之后就不再发布新版本而进入维护状态了，Spring官方推荐使用Resilience替代Hystrix。
一、Maven依赖
1、Resilience4j核心依赖
Maven依赖如下：
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot2</artifactId>
    <version>1.7.1</version>
</dependency>
如果是SpringBoot2.x版本，就需要使用resilience4j-spring-boot2的1.x版本；
如果是SpringBoot3.x版本，使用resilience4j-spring-boot2的2.x版本或使用resilience4j-spring-boot3。

resilience4j-spring-boot2已包含对注解的支持。
如果想要显示引入resilience4j-annotations以使用其他版本，就需要屏蔽resilience4j-spring-boot2的resilience4j-annotations，
否则会发生冲突而导致SpringBoot启动失败。
另外还有个坑点就是，resilience4j-spring-boot2的1.7.1版本中的有些子包是1.7.0版本，比如resilience4j-spring等，
如果引入的是resilience4j-spring-boot2的1.7.1版本，resilience4j-spring就会出现1.7.0和1.7.1两个版本，导致
io.github.resilience4j.spelresolver.configure.SpelResolverConfiguration和
io.github.resilience4j.spelresolver.autoconfigure.SpelResolverConfigurationOnMissingBean
的一些方法调用发生冲突，为了解决这个问题，需要将resilience4j-spring-boot2的版本修改为1.7.0，跟其内子包版本统一。

2、Actuator监控
Maven依赖如下：
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
    <version>2.7.18</version>
</dependency>
需要监控metrics、circuitbreakers端点，配合prometheus可视化面板监控Resilience4j熔断器。

Actuator监控默认访问地址：http://服务ip:服务port/actuator/监控端点
例如：http://localhost:8086/actuator/metrics

3、Prometheus面板
Maven依赖如下：
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
    <version>1.9.17</version>
</dependency>
Prometheus指标面板默认访问地址：http://服务ip:服务port/actuator/prometheus

4、其他基础依赖
spring-cloud-dependencies、
spring-boot-starter-parent、
spring-boot-starter-web

二、SpringBoot配置
yml格式示例：（resilience4j.circuitbreaker.instances配置熔断器的主要属性）
# 顶格写
resilience4j:
  # 熔断器配置
  circuitbreaker:
    instances:
      userService: # 实例名称
        registerHealthIndicator: true
        slidingWindowType: TIME_BASED # 滑动窗口类型
        slidingWindowSize: 10        # 统计窗口大小
        minimumNumberOfCalls: 5       # 最小调用次数
        failureRateThreshold: 50      # 失败率阈值(50%)
        waitDurationInOpenState: 5s   # 熔断持续时间
        permittedNumberOfCallsInHalfOpenState: 3 # 半开状态允许的调用数
        automaticTransitionFromOpenToHalfOpenEnabled: true # 自动切换到半开
  # 可选：重试配置
  retry:
    instances:
      userService:
        maxAttempts: 3      #最大重试次数
        waitDuration: 500ms #每次重试间隔时间

# Actuator 监控端点
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,circuitbreakers
  metrics:
    export:
      prometheus:
        enabled: true

三、关键注解
@CircuitBreaker：指定熔断器实例和降级方法。只包含name和fallbackMethod两个属性。

name属性：指定熔断器实例名称。
需要和SpringBoot配置文件中resilience4j.circuitbreaker.instances指的名称一致。
或者与ResilienceConfig中自定义的熔断器实例名称一致。

fallbackMethod属性：指定当前方法的降级方法。降级方法的方法参数与原方法一致，且需要多加一个Exception参数。
例如：
private String getUserFallback(String userId, Exception e) {
    return "[Annotation Fallback] User-" + userId;
}

@CircuitBreaker注解的完整示例：
@io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(
        name = "userService", fallbackMethod = "getUserFallback")
public String getUserWithAnnotation(String userId) {
    // 具体处理
}

四、原理机制
1、熔断
与Hystrix一样，也是基于熔断器状态机模式和滑动窗口统计来进行熔断器的打开和关闭处理。
熔断器有三种状态：CLOSED、OPEN、HALF_OPEN。
滑动窗口统计可以基于时间或计算来统计失败率，但至少要累计到指定阈值的请求数后才会开始计算，并不是一上来就计算。
这个请求数阈值可通过minimumNumberOfCalls属性设置。

2、熔断器三种状态的相互转换
1）CLOSED
熔断器默认是CLOSED关闭状态。
2）CLOSED -> OPEN
累计请求数达到指定数量，开始使用滑动窗口方式统计请求失败率，请求失败率达到阈值时（默认是50%），
熔断器打开，进入OPEN状态。OPEN状态持续一定时间，持续期内所有经过熔断器的请求都会快速失败。
OPEN状态的持续时间可由waitDurationInOpenState配置指定。
3）OPEN -> HALF_OPEN
熔断器OPEN状态保持指定时间后，转变为HALF_OPEN状态
4）HALF_OPEN -> CLOSED
熔断器在HALF_OPEN状态下，尝试放行请求，请求调用成功，关闭熔断器，熔断器回到CLOSED状态。
5）HALF_OPEN -> OPEN
熔断器在HALF_OPEN状态下，尝试放行请求，请求调用失败，重新打开熔断器，熔断器回到OPEN状态。

3、熔断状态和滑动窗口统计涉及的主要参数
1）minimumNumberOfCalls
最小调用次数，大于0的整数，默认为100。熔断服务开启后，通过熔断器的请求数累计达到此数值后，才开始计算请求调用的失败率。
2）failureRateThreshold
失败率阈值，百分数值，大于0的整数，默认为50。请求调用的失败率达到此阈值时，熔断器全开状态，所有通过熔断器的请求全都快速失败。
3）waitDurationInOpenState
熔断持续时间，单位秒，默认为60s。熔断器一旦全开，请求熔断持续此指定时间。过了这段时间后，熔断器转为半开状态，尝试放行请求。
4）permittedNumberOfCallsInHalfOpenState
半开状态允许的请求调用数，大于0的整数，默认为10。熔断器转为半开状态时，会放行这个数量的请求。
如果失败率达到阈值，熔断器再次转为全开状态；如果失败率没有到达阈值，熔断器转为关闭状态，所有请求正常放行。
5）slidingWindowType
滑动窗口类的边界类型，枚举类型，由CircuitBreakerConfig.SlidingWindowType枚举类提供。
支持TIME_BASED和COUNT_BASED两种类型，TIME_BASED基于时间边界进行统计，COUNT_BASED基于请求累计数量统计。
6）slidingWindowSize
统计窗口的大小（长度），大于0的整数。
窗口类型为TIME_BASED是，此属性的值为大于0的时间数，单位为秒。
窗口类型为COUNT_BASED是，此属性的值为大于0的请求数。
7）automaticTransitionFromOpenToHalfOpenEnabled
是否在熔断时间结束后自动由全开切换到半开状态，布尔类型值，默认为false。
8）registerHealthIndicator
是否注册监控指标，布尔类型的值，默认为false。如果想要结合actuator监控，此属性要设置为true。

完整详细的配置属性参考CircuitBreakerConfig类，通过CircuitBreakerConfig.ofDefaults()，
可定位到内部类Builder的构造方法，其有各种属性值的默认设置。

4、降级
用户自定义Fallback方法，当请求调用失败时执行。通过@CircuitBreaker注解的fallbackMethod属性指定。
降级方法需要与@CircuitBreaker注解的原方法参数一致，且需要多加一个Exception参数。
例如：
private String getUserFallback(String userId, Exception e) {
    return "[Annotation Fallback] User-" + userId;
}

除了@CircuitBreaker注解的方式配置熔断降级，还可以通过代码方式配置熔断降级处理，但这种硬编码方式更复杂灵活性也更差。

降级方法可以采用多层降级策略：
第一层降级去查缓存数据，查不到再进行第二层降级；
第二层降级中返回默认值，默认值返回异常则进入第三层降级；
第三层降级包装异常为友好提示。

为每个熔断监控方法编写降级方法是比较繁琐的，每个原方法参数也不相同，如果想要提高效率，可以考虑使用全局异常处理器进行降级兜底。
例如：
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(CallNotPermittedException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public String handleCircuitBreakerOpen(CallNotPermittedException e) {
        return "Service temporarily unavailable. Circuit breaker is open";
    }
}
需要注意的是处理的是CallNotPermittedException异常。


五、Resilience4j与Hystrix对比
Resilience4j处理熔断降级是基于调用线程而不是线程池，线程相关的性能开销低；
Resilience4j所需的依赖也更轻量，支持反应式编程；
Resilience4j可以通过配置文件集合注解实现熔断降级，也可以用代码方式实现；
Resilience4j可以配置滑动窗口的大小，而不是固定长度；
Resilience4j是Spring官方推荐的熔断降级组件，有更好的社区活跃度。

六、Prometheus+Grafana实现Resilience4j可视化面板
https://prometheus.io/download/
https://grafana.com/grafana/download

http://localhost:8086/actuator/prometheus
http://localhost:8086/actuator/metrics
http://localhost:8086/actuator/circuitbreakers

七、关键源码
1、CircuitBreakerConfig的Builder类的无参构造方法，包含了各项属性的默认值。
public Builder() {
    this.currentTimestampFunction = CircuitBreakerConfig.DEFAULT_TIMESTAMP_FUNCTION;
    this.timestampUnit = CircuitBreakerConfig.DEFAULT_TIMESTAMP_UNIT;
    this.recordExceptions = new Class[0];
    this.ignoreExceptions = new Class[0];
    this.failureRateThreshold = 50.0F;
    this.minimumNumberOfCalls = 100;
    this.writableStackTraceEnabled = true;
    this.permittedNumberOfCallsInHalfOpenState = 10;
    this.slidingWindowSize = 100;
    this.recordResultPredicate = CircuitBreakerConfig.DEFAULT_RECORD_RESULT_PREDICATE;
    this.waitIntervalFunctionInOpenState = IntervalFunction.of(Duration.ofSeconds(60L));
    this.automaticTransitionFromOpenToHalfOpenEnabled = false;
    this.slidingWindowType = CircuitBreakerConfig.DEFAULT_SLIDING_WINDOW_TYPE;
    this.slowCallRateThreshold = 100.0F;
    this.slowCallDurationThreshold = Duration.ofSeconds(60L);
    this.maxWaitDurationInHalfOpenState = Duration.ofSeconds(0L);
    this.createWaitIntervalFunctionCounter = 0;
}