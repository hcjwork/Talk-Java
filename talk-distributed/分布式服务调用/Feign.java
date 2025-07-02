Feign的远程调用基于JDK动态代理实现，通过定义Feign客户端接口类，利用JDK动态代理实现该接口，结合服务的地址、方法url，
用HTTP方法调到目标服务的目标方法。 

FeignClient接口定义在服务提供方，实际工程中通常命名为XXXFeignClient，一般是在服务提供方的api子模块内，
api子模块只定义非业务性的接口或实体类。
FeignClient接口使用@FeignClient注解进行标识。

Feign基于JDK进行代理而不是Cglib，是因为Feign是不清楚的目标服务的具体实现的，而且Feign也不应该与业务类绑定太紧密，
因此选择接口的方式，即便如此FeignClient接口依然要与业务服务进行关联，并无法完全解耦。

JDK动态代理帮助Feign根据FeignClient接口实现其子类，并重写FeignClient接口的所有方法，在重写的方法中，
以HTTP的方式调用目标方法。
但JDK动态代码无法指导目标服务的地址和目标方法的url，所以还需要@FeignClient注解和@RequestMapping注解来配合。

@FeignClient注解的属性有：
value、serviceId、contextId、name、qualifier、url、decode404、
configuration、fallback、fallbackFactory、path、primary。
其中关键的属性作用如下：
1）value、name
value和name属性等效，都是用来指定要远程调用的服务的名称，
此名称与服务在注册中心的名称一致，也就是spring.application.name配置的自定义服务名称。
Feign通过服务注册中心获取要调用的服务的ip和port信息。

2）contextId
contextId用于指定@FeignClient所注解的接口的实例名称，调用方通过@Autowired注入FeignClient接口实例时根据
contextId定义的名称寻找匹配。

3）url
url用于指定要远程调用的服务的url地址，格式为：协议+服务ip+服务端口，例如：http://localhost:8082。
指定了url，就不需要再指定要调用的服务名称了。

4）fallback
fallback用于指定远程服务调用失败时的回调方法，不管哪一个方法调用失败都回调此方法做降级处理。

5）fallbackFactory
fallbackFactory用于指定@FeignClient所注解的接口的所有方法的回调，是为回调工厂，
常与熔断降级结合，例如Hystrix、Sentinel等。

@RequestMapping注解用于要目标方法上，其表示的完整路径与目标服务的Controller层的对应方法一致，
否则无法正常调用到目标方法。

如果调用FeignClient的一方无法正常通过@Autowired注入FeignClient接口的实例，
说明@FeignClient没有被正常扫描并实例化，此时需要在启动类所加的@EnableFeignClients注解中，
通过basePackages或basePackageClasses属性指定要扫描的@FeignClient所注解的类或包。
例如：
@EnableFeignClients(basePackages = {"com.hcj.example"})
或
@EnableFeignClients(basePackageClasses = {com.hcj.example.feign.UserFeignClient.class})

@FeignClient注解必须要声明value或name属性，这两个属性都是标识目标服务的用户名，任选其一即可，
另外还有个url属性可以直接指定目标服务的ip和端口信息，可以与value或name属性同时存在，但如果同时指定了url，
Feign会将所有的远程调用请求都分发到这个url地址对应的服务上去。
如果目标服务有多个实例，想要Feign将请求通过负载均衡算法分发，就需要去掉url属性的设置。

Feign的负载均衡是怎么实现的？
Feign根据服务名称找到多个服务实例，默认采用轮询的方式分发远程调用请求，使用的是Ribbon作为负载均衡组件。

Feign的熔断降级是怎么结合的？
@FeignClient注解有一个fallback和fallbackFactory属性，
前者可指定一个通用的回调方法作为降级处理，后者可以指定一个实现了Feign客户端接口的回调类。
Feign基于Hystrix实现熔断降级。

从远程调用方法，到服务注册与发现，到请求负载均衡，到服务熔断降级。
