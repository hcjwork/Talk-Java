
一、Maven依赖
1、gateway依赖
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-gateway</artifactId>
    <version>3.1.9</version>
</dependency>

2、负载均衡依赖
gateway使用低版本时可以用ribbon做负载均衡，gateway使用高版本时推荐使用loadbalancer。
spring-cloud-gateway在3.x默认集成了loadbalancer做负载均衡。
loadbalancer依赖示例：
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-loadbalancer</artifactId>
    <version>3.1.9</version>
</dependency>

3、服务注册发现依赖
服务注册发现可以选Eureka或Nacos或Zookeeper。
eureka依赖如下：
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
    <version>3.1.9</version>
</dependency>

nacos依赖如下：
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
    <version>2021.1</version>
</dependency>

4、其他关键依赖
spring-cloud-dependencies：spring cloud核心依赖。spring-cloud先关的starter可根据这个基础自动选择匹配版本。
spring-boot-starter-parent
spring-boot-starter-web
spring-boot-starter-actuator
spring-boot-starter-data-redis-reactive

可以结合Redis做限流。

二、SpringBoot配置
1、gateway配置
SpringCloudGateway把符合规则的请求路由发送给具体的服务，可以直接指定具体连接地址，也可以通过服务发现以负载均衡的方式
选择请求路由的目标服务。
当然对于要路由的服务实例列表，可以由服务注册中心来提供，也可以通过配置文件直接设置。
比如：
spring.cloud.discovery.client.simple.instances.service-provider[0].uri=http://localhost:8081
spring.cloud.discovery.client.simple.instances.service-provider[1].uri=http://localhost:8082
但这种硬编码的方式没有灵活性，且需要自己维护，如果服务实例或减少或不能正常提供服务时，没法第一时间调整可用服务实例列表，
还得修改代码或配置，重新发布才行。
所以通常还是通过服务注册发现组件来维护服务实例列表，在结合负载均衡组件选择请求路由的具体服务实例。
服务注册发现组件推荐使用Eureka或Nacos，负载均衡组件则推荐使用Ribbon或Loadbalancer。

yml示例：
# Spring Cloud Gateway配置
spring:
  cloud:
    gateway:
      discovery:
        locator:
          enabled: true  # 开启通过服务发现自动创建路由
          lower-case-service-id: true  # 服务ID小写
      routes:
        - id: user-service
          uri: lb://user-service  # lb表示负载均衡
          predicates:
            - Path=/api/users/**
          filters:
            - StripPrefix=1  # 去掉前缀/api/users

关键属性解析

2、eureka配置（如果使用Eureka做服务注册与发现）
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka  # Eureka服务器地址

3、nacos配置（如果使用Nacos做服务注册与发现）
spring:
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848  # Nacos Server地址
        namespace: public        # 命名空间（默认public，如果是用新建的命名空间，要用ID值）
        group: DEFAULT_GROUP         # 分组（默认DEFAULT_GROUP）

4、限流Redis配置
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: lb://user-service  # lb表示负载均衡
          predicates:
            - Path=/api/users/**
          filters:
            - StripPrefix=1  # 去掉前缀/api/users
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 10  # 每秒允许的请求数
                redis-rate-limiter.burstCapacity: 20  # 最大突发请求数
                redis-rate-limiter.requestedTokens: 1  # 每个请求消耗的令牌数

三、核心注解
1、@EnableDiscoveryClient
如果Gateway使用服务注册发现组件，需要在启动类或配置类上添加这个注解，以开启服务注册发现。

四、使用示例

五、核心原理