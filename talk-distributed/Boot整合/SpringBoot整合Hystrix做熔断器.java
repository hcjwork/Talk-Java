Hystrix是SpringCloud管理维护的一种服务熔断组件，但目前已经不再发布新的版本，官方推荐
使用Resilience4j替代Hystrix，但依然有很多项目使用的是Hystrix，所以关于Hystrix的整合使用，
还是有必要再梳理一下的。

一、原理
Hystrix的熔断机制是基于状态机模式和滑动窗口统计实现的。
所谓的状态机，就是用不同的状态标识关键处理节点，根据这些状态来决定具体的处理逻辑。

Hystrix熔断器主要有三种状态：CLOSED、OPEN、HALF_OPEN。
CLOSED，关闭状态，此状态表示Hystrix熔断器没有打开，所有请求正常通行。
OPEN，打开状态，此状态表示Hystrix熔断器已经打开，所有的请求直接熔断，不允许继续往下游调用。
HALF_OPEN，半开状态，此状态表示Hystrix熔断器半开半闭，尝试放行请求，如果成功熔断器进入CLOSED状态，如果失败熔断器进入OPEN状态。

三种状态的转换情形：
CLOSED -> OPEN  => 窗口期内服务调用错误率达到阈值，直接熔断请求。默认阈值为50%。
OPEN -> HALF_OPEN => 熔断持续时间已过，进入半开状态，尝试放行一个请求，看请求是否成功。默认熔断持续时间为5秒。
HALF_OPEN -> CLOSED => 放行的请求处理成功，熔断器关闭。
HALF_OPEN -> OPEN => 放行的请求处理失败，直接熔断请求，熔断器进入全开状态。

关于Hystrix熔断器的这三种状态，在低版本时是以枚举的形式定义的，在高版本中则是通过Atomic原子计数来表示。

Hystrix熔断器的滑动窗口统计支持两种模式，一种是以时间为边界，一种是以请求数量为边界。
默认是以时间为边界，如果想要以请求数量为边界，需要通过“metrics.rollingPercentile.enabled”显示开启。
以时间为边界的滑动窗口模式，默认以10秒时间为统计窗口长度，统计时间窗口内的请求错误率；
以请求数量为边界的滑动窗口模式，统计指定数量的请求错误率。
无论是哪种滑动窗口模式，都是基于Bucket数组实现，统计的时间复杂度是O(1)。

当请求的数量累计到指定值才会开始计算请求的错误率，如果请求的错误率达到阈值，Hystrix熔断器进入OPEN状态，直接熔断请求，
熔断时间默认持续5秒（可配置），在熔断器OPEN期间，所有的请求调用会快速失败。

Hystrix通过线程池方式来实现限流，代替传统的限流算法，线程池满时会直接抛出HystrixRuntimeException，快速失败。
使用GlobalExceptionHandler全局异常处理器，兜底处理HystrixRuntimeException。
例如：
@RestControllerAdvice
public class GlobalFallbackHandler {
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    @ExceptionHandler(HystrixRuntimeException.class)
    public String handleHystrixException(HystrixRuntimeException e) {
        return "Global Fallback: Service unavailable. Reason: " + e.getFailureType();
    }
}

二、核心依赖（以Maven为例）
1、Hystrix依赖
Hystrix的核心依赖是spring-cloud-starter-netflix-hystrix或spring-cloud-netflix-hystrix，
例如其Maven依赖为：
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-hystrix</artifactId>
    <version>2.2.10.RELEASE</version>
</dependency>
2.2.10.RELEASE是兼容SpringBoot3.0以下的最高Hystrix版本。
在启动类上需要配置@EnableHystrix注解已开启Hystrix熔断器，不然不会生效。

spring-cloud-netflix-hystrix的Maven依赖为：
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-netflix-hystrix</artifactId>
    <version>2.2.10.RELEASE</version>
</dependency>

2、Actuator监控
Maven依赖如下：
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
    <version>2.7.18</version>
</dependency>
actuator监控hystrix.stream的数据状态，配合hystrix的dashboard可视化界面进行展示。
注意application文件中的“management.endpoints.web.exposure.include”属性要暴露hystrix.stream监控端点。
例如（以yml文件为例）：
management:
  endpoints:
    web:
      exposure:
        include: health,hystrix.stream  # 暴露监控端点

actuator监控信息默认访问地址是：http://服务ip:服务port/actuator/hystrix.stream。

3、Hystrix可视化面板
Maven依赖如下：
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-hystrix-dashboard</artifactId>
    <version>2.2.10.RELEASE</version>
</dependency>
Hystrix可视化面板的默认访问地址是：http://服务ip:服务port/hystrix。


三、核心配置（以.yml格式为例）
Hystrix的配置项全路径为：hystrix.command.[commandName].[execution|circuitBreaker].[commandKey]
示例：
# 顶格写
hystrix:
  command:
    default:
      execution:
        isolation:
          thread:
            timeoutInMilliseconds: 2000  # 默认超时2秒
      circuitBreaker:
        requestVolumeThreshold: 5       # 触发熔断的最小累计请求数
        errorThresholdPercentage: 5    # 错误率阈值(默认是50%)
        sleepWindowInMilliseconds: 10000 # 熔断持续时间(默认是10秒)

可以通过配置文件配置hystrix的command信息，也可以通过@HystrixCommand配置。
如果都配置了，采用就近原则，即使用@HystrixCommand的配置。

四、核心注解
1、@EnableHystrix或@EnableCircuitBreaker
@EnableHystrix或@EnableCircuitBreaker（这个注解在新版本被废弃）：开启Hystrix熔断器。配置在启动类或配置类上。
例如：
@SpringBootApplication
@EnableHystrix
public class HystrixDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(HystrixDemoApplication.class, args);
    }
}

2、@HystrixCommand
@HystrixCommand：Hystrix熔断器命令配置。
支持多个属性，主要包含：fallbackMethod、commandKey、threadPoolKey、commandProperties、threadPoolProperties。
fallbackMethod：指定一个降级方法。降级方法必须与@HystrixCommand注解的方法的参数一致，降级方法可以多添加一个Throwable参数。
例如：
public String getUserFallback(Long userId, Throwable t) {
    return "[Fallback] Cached User-" + userId;
}
commandKey：自定义的HystrixCommand名称。
threadPoolKey：自定义的Hystrix线程池名称。
commandProperties：HystrixCommand的属性，通过@HystrixProperty进行设置，比如请求错误率阈值。
threadPoolProperties：Hystrix线程池的属性，也通过@HystrixProperty进行设置，比如核心线程数。

@HystrixCommand完整示例：
@HystrixCommand(
    fallbackMethod = "getUserFallback",
    commandKey = "getUserCommand",
    threadPoolKey = "userThreadPool",
    commandProperties = {
            @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "1000"),
            @HystrixProperty(name = "metrics.rollingStats.timeInMilliseconds", value = "20000"),
            @HystrixProperty(name = "metrics.rollingStats.numBuckets", value = "20"),
    },
    threadPoolProperties = {
            @HystrixProperty(name = "coreSize", value = "10"),
            @HystrixProperty(name = "maxQueueSize", value = "20")
    }
)

3、@HystrixProperty
@HystrixProperty：Hystrix配置项，包含name和value两个属性，name为配置名称，value为配置值。
@HystrixProperty支持的配置可参考Hystrix的配置类“HystrixCommandProperties”。

五、缺陷
1、线程池隔离带来很大的性能开销。
每个依赖服务创建独立的线程池，导致高并发场景下线程切换开销大，线程堆栈内存占用高。
2、线程池资源浪费
线程池大小固定，无法动态扩缩容。线程空闲时仍然占用资源。
3、功能扩展性差
熔断器的规则配置不支持动态规则和细粒度的流量控制。只有一种线程池的机制来控制流量，比较粗疏。
4、监控能力薄弱
需要结合actuator监控hystrix.stream端点，dashboard面板功能简陋单一，时常出现监控不及时或断联等问题。
5、社区活跃度低
spring-cloud-starter-netflix-hystrix的版本只更新到2.2.10.RELEASE版本，目前已经入维护状态不再发布新的版本，
最多只能兼容支持到SpringBoot的2.7.x版本，不支持SpringBoot3.0及之上版本。
6、强依赖netflix
与Archaius/Ribbon等组件深度耦合，难以独立使用。
7、缺乏云原生支持
不支持Kubernetes服务发现、Istio等服务网格特性。

六、迁移方案
常规Spring应用 → Resilience4j（轻量级，函数式API）
阿里云/高并发场景 → Sentinel（生产级控制台）
服务网格环境 → Istio原生熔断