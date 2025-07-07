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

六、Nacos做配置中心的原理机制

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

九、Nacos的AP和CP架构模式
