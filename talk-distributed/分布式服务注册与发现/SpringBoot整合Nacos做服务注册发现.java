Nacos对多种架构模式的支持，加上配置管理，让Nacos相比于Eureka、Zookeeper、Consul等其他组件有更多的优势，
现在已经是非常主流的服务注册与发现组件了。
像Eureka、Zookeeper、Consul这几种都只是提供了服务注册发现的功能，对于配置管理还需要另外引入比如SpringCloudConfig、Apollo之类的组件。
而Nacos融服务注册发现和配置管理为一体，还支持多种环境配置的隔离维护，是服务注册发现和配置中心落地的首选。
其他的Zookeeper可能用于强一致性的分布式锁或集群协调器等多，Consul则在健康检查上有一定优势，Eureka在SpringCloud项目中可能有更好的契合度。
之前Eureka有段时间都处于维护状态了，但在最近几年又活跃了不少。

一、Nacos服务
Nacos服务的版本要和客户端版本以及SpringBoot版本要兼容，如果是SpringBoot3.0以下版本，推荐使用Nacos2.4.1版本。
Nacos2.4.1默认使用内置数据库Derby进行配置存储，所以Nacos服务的conf目录下的application.properties文件中
并没有显示配置数据源有关的信息。
Derby数据库是一种轻量级嵌入式数据库，无需单独安装，适合测试或轻量级部署场景。
但如果需要高并发或大规模部署，通常要配置外置数据库，比如MySQL等，以保证生产的稳定性和性能。

Nacos使用MySQL做数据源配置步骤：
        1、先在Nacos安装包的conf目录下找到mysql相关的sql脚本，在MySQL中执行。
        2、在Nacos安装包的conf目录下找到application.properties文件，搜索datasource关键词，找到配置示例。
按照示例修改数据源为指定的MySQL。
检查users表中是否有默认用户nacos，如果没有，可执行以下sql插入记录：
INSERT INTO users (username, password, enabled) VALUES ('nacos', '$2a$10$EuWPZHzz32dJN7jexM34MOeYirDdFAZm2kuWj7VEOJhhZkDrxfvUu', 1);
密码为nacos的BCrypt加密值。

Nacos服务的默认端口是8848，控制台地址为：http://locahost:8848/nacos，登录用户名和密码默认为：nacos/nacos。

二、Maven依赖
1、服务注册发现
        <dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
    <version>2021.1</version>
</dependency>

        2、配置中心
        <dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
    <version>2021.1</version>
</dependency>

        3、其他基础依赖
spring-boot-starter-parent、
spring-cloud-alibaba-dependencies、
spring-boot-starter-web

三、SpringBoot配置
以yml为例。
        1、服务注册发现配置
spring:
application:
name: nacos-demo  # 服务名称（注册到Nacos的名称）
cloud:
nacos:
discovery:
server-addr: localhost:8848  # Nacos Server地址
namespace: public            # 命名空间（默认public）
group: DEFAULT_GROUP         # 分组（默认DEFAULT_GROUP）

注意：如果namespace要使用新建的，namespace属性的值要用命名空间的id值而不是名称，否则无法注册。

        2、配置中心配置
spring:
cloud:
nacos:
config:
server-addr: localhost:8848
namespace: public
group: DEFAULT_GROUP
file-extension: yaml         # 配置文件后缀（支持yaml/properties）
shared-configs:              # 共享配置
          - data-id: common-config.yaml
group: DEFAULT_GROUP
refresh: true
username: ${NACOS_USER:nacos} # 从环境变量读取，默认nacos
password: ${NACOS_PWD:nacos} # 从环境变量读取，默认nacos

如果namespace要使用新建的，namespace属性的值要用命名空间的id值而不是名称，否则无法匹配。
如果没有通过name属性指定nacos配置文件的dataId，默认找DataId为“${spring.application.name}.${file-extension}”的文件作为当前服务的配置文件。

Nacos控制台上的配置文件的DataId最好带上yaml后缀，避免匹配不上而无法正常读取配置。

四、关键注解
1、@EnableDiscoveryClient
开启服务注册与发现。这个是org.springframework.cloud.client.discovery包下的注解。
不加此注解SpringBoot服务可能无法正常注册到Nacos服务中心。
@EnableDiscoveryClient可以用在启动类上，也可以用在Configuration配置类上。

        2、@RefreshScope
支持配置动态刷新。这个是org.springframework.cloud.context.config.annotation包下的注解。

        3、@Value
可通过${}表达式引用SpringBoot服务的配置的属性值。
这个是org.springframework.beans.factory.annotation包下的注解。

五、Nacos做服务注册发现的原理机制
Nacos服务注册与发现，主要基于三部分实现：服务注册、健康检查、动态推送。
        1、服务注册
业务服务嵌入Nacos客户端，通过Nacos客户端将服务自身的相关信息注册在Nacos服务中，也就是把服务的元数据信息发送给Nacos服务。
Nacos服务维护已注册服务的关键信息。
注册的信息示例如下：
        {
        "serviceName": "nacos-demo",
        "ip": "192.168.0.103",
        "port": 8080,
        "clusterName": "DEFAULT",
        "metadata": {"version": "1.0"}
        }
两个独立部署的微服务A和B，A如果想要访问B中的接口资源，需要先和B服务建立HTTP或TCP连接，
而连接的关键就是四元组信息，即：源ip、源端口、目的ip、目的端口。
服务A自身的ip和端口肯定是知道的，而B的ip和端口需要知晓才能进行网络连接，
所以服务注册时需要将自己的ip和端口信息注册给Nacos，否则其他服务即便知道有nacos-demo这么个服务也没法进行连接和访问。
这一点是所有服务注册发现中心都需要提供的公共功能。

注册服务通过Nacos客户端向Nacos服务端发送注册请求，注册信息主要包含服务ip和端口等信息，
Nacos服务端基于Nacos不同的分布式架构模式进行相应处理，存储或更新注册服务实例列表。
如果是启用的AP架构，Nacos服务端将实例信息写入内存中；
如果是启用的CP架构，Nacos服务端将实例信息持久化到数据库中。

        2、健康检查
健康检查其实就是需要注册者与Nacos服务端建立长连接，定时发送自己的心跳数据给Nacos服务端，表示自己能继续提供正常服务。
当然并不是业务服务直接与Nacos服务端进行连接，而是通过Nacos客户端来做这个事情。
如果Nacos客户端超过一定时间没有发送心跳数据，Nacos服务端就认为其对应的注册服务已经有问题了，就会将注册踢出服务提供者列表。
踢出方法就是修改注册服务的健康状态为false。
Nacos客户端默认5秒发送一次心跳给Nacos服务端。
如果15秒内Nacos服务端没有接收到心跳，会将注册服务实例标记为不健康，30秒后自动删除。
心跳间隔时间、心跳超时时间、实例删除延迟时间等都可以进行配置。
以yml配置为例如下：
spring:
cloud:
nacos:
discovery:
heartbeat-interval: 5000    # 心跳间隔（ms）
heart-beat-timeout: 15000   # 心跳超时
ip-delete-timeout: 30000    # 实例删除延迟

上面是由Nacos客户端主动发送心跳维护实例健康状态的模式，还有一种是Nacos服务端主动探测注册服务实例健康状态的模式。
Nacos服务端主动探测模式需要在Naco服务端的application.properties文件中将
“nacos.naming.health.check.enabled”属性设置为true。
Nacos服务端主动发起健康检查（如调用 `/health` 端点）。


        3、动态推送
其实就是服务发现，需要Nacos客户端和Nacos服务端的相互配合。
当服务消费者启动时，通过嵌入的Nacos客户端从Nacos服务端全量拉取服务提供者列表，并进行本地缓存。
默认是每10秒拉取一次，这个轮询时间可以通过server-list-refresh-interval属性进行配置。例如：
spring:
cloud:
nacos:
discovery:
server-list-refresh-interval: 10000  # 刷新间隔（ms）
而Nacos服务端就负责推送可用的服务信息，在2.0及以上版本采用长轮询（Long Polling）机制实现准实时推送。

服务消费者通过本侧的Nacos客户端首次发起服务信息拉取请求，Nacos服务端会立即返回服务注册的全量数据，
同时与消费者侧的Nacos客户端建立长连接，这个长连接默认维持30秒，
在30秒内如果服务注册信息有变更会即时推送给服务消费者侧的Nacos客户端，
消费者侧的Nacos客户端更新本地的服务提供者实例信息缓存。
如果没有变更，在长连接超时后返回空或“304 Not Modified”，Nacos服务端关闭长连接。
下一次再由Nacos客户端发起请求，重新走首次请求流程。

首次请求服务注册信息可能会失败，因此Nacos支持重试机制，这部分配置示例如下：
spring:
cloud:
nacos:
discovery:
fail-fast: true           # 快速失败
retry:
max-attempts: 3         # 重试次数
initial-interval: 1000  # 重试间隔（ms）

Nacos使用长连接来配合客户端的轮询请求，减少了连接频繁建立关闭的开销，提高了服务注册信息拉取推送的效率。
Nacos采用推和拉两种信息获取模式，尤其是这个推送模式，只要服务信息一有变更就立马推给Nacos客户端，
确实大大加强了信息获取的实时性。

六、Nacos做配置中心的原理机制
配置中心其实也是Nacos客户端和服务端通力合作实现的。
Nacos服务端存储和管理所有配置，并提供HTTP/gRPC接口给Nacos客户端调用。
Nacos客户端发起配置拉取请求，监听配置变更，拉取到的配置保存在客户端所在机器的~/nacos/config目录中，当Nacos服务不可用时取本地目录的配置使用。
当然，配置拉取也有重试机制的支持，对应yml配置示例如下：
spring:
cloud:
nacos:
config:
max-retry: 5       # 最大重试次数
retry-time: 2000   # 重试间隔(ms)

Nacos服务端接收到客户端的首次拉取请求，会立即返回指定DataId的配置，同时将这次连接维护为30秒的长连接，
在这个长连接持续期间，如果配置有变更Nacos服务端会主动推送给客户端，如果没有变更也会维持连接直到超时。

如果Nacos客户端这边的服务没有指定配置的DataId，默认找寻“${spring.application.name}.${file-extension}”作为DataId进行匹配。

Nacos基于内存并结合数据库来存储配置，服务启动时数据库加载配置到内存中，客户端请求时优先从内存响应，先写数据库再更新内存。
通过定时任务来保证内存和数据库中配置的最终一致性。

Nacos在2.x版本默认内置了Derby数据库，在单机模式下默认用Derby作数据库，但在集群模式下需要使用外部数据库。

Nacos可通过namesapce来区分不同环境的配置，这在多环境下有很强的适用性。

七、bootstrap文件与Nacos配置关联性
Spring Boot 2.4+ 默认不加载 bootstrap.yml，如需要应用bootstrap.yml，需要添加spring-cloud-starter-bootstrap依赖，
例如：
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-bootstrap</artifactId>
    <version>3.1.5</version>
</dependency>

Nacos的配置如果是放在application.yml或application.properties中，
可能出现Nacos控制台上的配置在SpringBoot工程里无法正常读取的问题，
此时需要把Nacos相关配置移到bootstrap.yml文件中，因为bootstrap.yml的加载优先级更高，由父应用上下文加载，
会提前初始化Nacos客户端，包括服务发现和配置中心的功能，而application.yml的配置在应用主上下文初始化时才加载，
此时Nacos客户端可能已错过部分初始化流程。

Nacos客户端的双模式：
当配置放在 `bootstrap.yml` 时，Nacos 客户端会同时激活：
        1. 配置中心客户端（从 Nacos 读取配置）
        2. 服务发现客户端（注册服务并定时拉取服务列表）

八、Nacos灰度发布管理
Nacos 作为服务发现和配置管理中心，支持多种灰度发布策略。

一种是基于权重的灰度发布，yml配置示例如下：
spring:
cloud:
nacos:
discovery:
weight: 80 # 设置服务实例权重

一种是基于元数据的灰度发布，可通过服务实例的元数据标识灰度版本，例如：
@Bean
public NacosDiscoveryProperties nacosProperties() {
    NacosDiscoveryProperties properties = new NacosDiscoveryProperties();
    Map<String, String> metadata = new HashMap<>();
    metadata.put("version", "gray");
    properties.setMetadata(metadata);
    return properties;
}

还有一种是直接通过Nacos控制台的配置进行灰度发布管理，先创建灰度配置（比如namespace命名为gray），
让指定的服务应用灰度配置。

九、Nacos的AP和CP架构模式
Nacos在全局模式上并不支持AP和CP架构的切换。

Nacos服务注册中心是基于AP架构的，虽然在服务实例级别可以通过设置ephemeral=false持久化实例信息，
但这是服务注册信息存储的AP和CP模式的切换，整个注册中心始终是建立在Distro协议上的。

Nacos配置中心是基于CP架构的，默认协议是Raft，ephemeral配置对其无任何影响，始终强一致性。