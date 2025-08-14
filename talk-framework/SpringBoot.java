1、约定大于配置。
将原来的一个通用性的或者基础性的可运行的Spring应用，抽取出普遍性或必要的配置组件，将这些必要的配置内置化为默认约定，
由SpringBoot在启动时自行加载应用，而不需要开发者再显示地冗杂配置和测验。
将原来的xml配置方式改善为.properties或.yml方式配置，语法更灵活，结构更清晰。
按需加载，只有引入相关依赖才会激活对应配置。

2、SpringBoot默认加载的配置是存放在哪的，加载了哪些配置？
SpringBoot默认加载的配置是在spring-boot-autoconfigure模块的META-INF目录下，SpringBoot2.7前默认是在
META-INF/spring.factories文件中，SpringBoot2.7开始默认在
META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports文件中。
但SpringBoot2.7+版本依旧保留着META-INF/spring.factories文件，以兼容低版本，如果两个文件都在，则以imports文件优先。

主要加载一些Web容器、JDBC连接等默认配置，可以使用.properties或.yml或配置类进行覆盖。
第三方库的Starter中也会在对应META-INF/spring.factories文件或
META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports文件中添加自动配置类信息。

3、自动配置在SpringBoot中扮演了什么角色？   
为了简化开发流程、提高开发效率。很多基础的通用性的配置，直接由SpringBoot选用默认值，减少程序开发者的代码工作量。
比如负责与前端交互的Web容器，在spring-boot-starter-web模块中引入，在SpringBoot服务启动时自动加载和配置。

4、自定义SpringBoot的Starter依赖
一个Starter是专门为了实现某种功能或职责的，所以单一职责要保证，只解决特定的问题。
像SpringBoot一样也采用约定优于配置的方式，提供合理的默认配置，在META-INF/目录下提供spring.factories或imports文件。
要保持Starter的独立性和解耦性，避免强依赖其他Starter。
提供条件化装配能力，避免重复Bean定义或与用户自定义Bean发生冲突。
保证开箱即用、易扩展、运维友好等特性。

5、SpringBoot配置的加载优先级
SpringBoot服务的配置根据来源可以分为：命令行配置、环境变量配置、项目外配置文件、项目内配置文件、配置类。
加载的优先级从高到低位：命令行配置 > 环境变量配置 > 项目外配置文件 > 项目内配置文件 > 配置类。
相同的属性会采用优先级更高的配置，低优先级中对应的属性会全部失效。

配置的加载优先级和加载顺序是相反的，优先级越高的越靠后加载，后加载的会覆盖先加载的同名配置，也就说最后加载的同名配置才是最终生效的。
实际加载顺序为：
配置类、（项目内部java文件）
resources目录下的application*.yml、（项目内部配置文件）
resources目录下的application*.properties、（项目内部配置文件）
resources目录下的bootstrap.yml、（项目内部配置文件）
resources/config目录下的application*.yml、（项目内部配置文件）
resources/config目录下的application*.properties、（项目内部配置文件）
resources/config目录下的bootstrap.yml、（项目内部配置文件）
jar包同目录的application*.yml、（项目外部配置文件）
jar包同目录的application*.properties、（项目外部配置文件）
jar包同目录/config下的application*.yml、（项目外部配置文件）
jar包同目录/config下的application*.properties、（项目外部配置文件）
操作系统环境变量、（操作系统环境变量）
`java -jar`命令-D选项配置、（java-jar命令行配置）
SPRING_APPLICATION_JSON专属环境变量、（操作系统环境变量）
`java -jar`命令行属性（java-jar命令行配置）。

配置加载的这种优先级特性，让应用服务的部署运维有了更强的灵活性，比如服务端口冲突，但又不想重新修改源码重新打包，就可以
在使用`java -jar`命令启动jar包时指定新的端口。

注意SpringBoot会默认加载与运行的jar包同目录或同目录的/config子目录下的application.yml/.properties中的配置，
且优先级高于SpringBoot项目内部（即resources目录）的application.yml/.properties的配置。
相同目录下的application.yml先于application.properties文件加载。
所以一个项目或模块应该统一使用.properties配置文件或.yml配置文件，避免混用导致配置加载混乱。

bootstrap.yml和application.yml的加载优先级，bootstrap.yml的加载优先级更高，会覆盖application.yml中的
同名属性，但bootstrap.yml在SpringBoot2.4+版本默认不加载，如果要加载则需要引入spring-cloud-starter-bootstrap依赖。
也就是说纯SpringBoot项目其实是不需要bootstrap.yml的，因为bootstrap.yml是SpringCloud用于引导应用获取外部配置的。

生产实践：
之前有一次发布兼容GoldenDB的2.4.1版本xxl-job的最新jar包，发现端口被占用导致xxl-job服务启动失败，
通过`netstat -tulnp`命令检查端口占用情况后，选用一个没有被占用的新端口重新启动xxl-job服务，
修改了项目内部application.properties文件中的端口号重新打包发布，但xxl-job服务依旧启动失败，
查看启动日志发现报错信息没有改变，也就是说依旧使用了之前被占用的端口，项目内部的配置文件对于`server.port`属性未生效，
一开始还以为是打包的问题，清理后重新打包发布，服务启动还是失败，于是开始排查是否有项目外部的因素干扰，检查了启动脚本、
系统变量、环境变量等都没有问题。最后通过修改xxl-job的jar包所在目录的application.properties配置，
在`java -jar`命令里通过`--spring.config.location`属性指定使用此配置文件，发现还是使用的之前的端口，
再次仔细检查后发现在xxl-job的jar包所在目录中，还有个config子目录，该目录中还有一个application.properties文件，
推测可能是这个文件引起的，修改文件内使用的端口号为未占用的新端口，再指定使用/config/application.properties为配置文件，
xxl-job最终成功启动并正常运行。
经过这次生产问题发现了SpringBoot对于jar包所处目录的配置文件和/config目录的配置文件的加载优先级，
加载优先级由高到低为：./config/application.properties > ./application.properties > 项目内的配置文件。

`java -jar`命令中指定的配置和环境变量配置的加载优先级是容易被识别的，
但对于jar包所处目录的配置文件和/config目录的配置文件的加载优先级则往往被忽视或模糊不清，尤其这种项目外的配置文件因为
灵活性更高而被使用更广泛，就更容易产生一些低级的细节问题。


1）命令行配置
这里的命令行指的就是`java -jar`命令，这是由java自身提供的java.exe程序所支持的。
`java -jar`命令的详情在Linux系统或Git Bash中可通过`java -jar -help`查看。
完整命令：java [-options] -jar jarfile [args...]
options：所是支持的操作选项，比较常用的是`-D<name>=<value>`选项，用来指定java系统属性，例如：-Dserver.port=8080。
jarfile：指定可运行的jar包文件。
args：命令行参数，格式为`--name=value`，例如：--server.port=8080。

命令行配置其实包含两种方式：命令行配置和操作选项配置。
由`-D<name>=<value>`操作选项指定的配置称为java系统配置，由`--name=value`指定的配置称为命令行配置，
其中name是属性名称value是属性值，两种配置的优先级：命令行配置 > 操作选项配置（java系统配置）。

使用命令行方式配置时需要注意，`-D<name>=<value>`选项必须要在jar包之前，`--name=value`必须要在jar包之后，例如：
`java -jar -Dserver.port=8999 nacos-server.jar`，
`java -jar nacos-server.jar --server.port=8999`。
`-D<name>=<value>`在`-jar`前后都可以，只要在具体的jar包名称前就行；而`--name=value`必须要在`-jar`和jar包名称后才行。

2）环境变量配置
环境变量配置也包含两种方式，但其实都是操作系统的环境变量。
一种是通过`SPRING_APPLICATION_JSON`这个专有属性名称来设置，需要提供标准的JSON格式，比如：
`SPRING_APPLICATION_JSON='{"server":{"port":9090}}'`。
另一种就是普通的操作系统环境变量设置，比如Linux系统中在环境变量文件`~/.bashrc`或`~/.bash_profile`通过`export`关键字
设置环境变量，例如：export server.port=8999。

环境变量配置和命令行配置的四种配置方式，加载优先级为：
命令行配置 > java系统配置 > `SPRING_APPLICATION_JSON`配置 > 操作系统环境变量配置。

3）项目外部配置
项目外部配置可以说是SpringBoot服务外的配置，也可以说就是待运行的jar包外的配置。
比如一个可运行的jar包为`nacos-server.jar`，与该jar包同目录的以及同目录/config子目录下的application.properties
或application.yml文件，其中的配置会被SpringBoot默认加载，这是由SpringBoot内部默认实现决定的。
先加载application.yml文件，再加载application.properties文件，所以application.yml中的同名属性会被覆盖。
最终以application.properties的同名属性生效。

./目录的和./config目录的，则是先加载./目录的，后加载./config目录的，也就是说./config目录中application.properties
或application.yml的同名属性会覆盖./目录下的配置文件。

假设jar包所在目录和所在目录的/[application.properties](..%2F..%2F..%2F..%2F..%2F..%2F..%2F..%2F..%2F..%2F..%2FTemp%2Fconfig%2Fapplication.properties)config子目录都有application.properties和application.yml文件，加载顺序为：
./application.yml > ./application.properties > ./config/application.yml > ./config/application.properties。
后加载的配置的同名属性会覆盖先加载的。

jar包外的配置文件提供了更大的灵活性，尤其是在通过传统shell脚本方式启动SpringBoot服务时，外置配置文件修改起来更方便，
但实际应用时需要注意，jar包外的配置会覆盖jar包内（resources目录的）配置的同名属性，./config目录的又会覆盖./目录的，
因此如果真要使用jar包外的配置，./config和./目录的配置只保留一份即可，两个目录都存在时容易误导问题定位。

jar包外的配置可通过在`java -jar`命令里的`--spring.config.location`属性进行指定，可使用绝对路径也可使用相对jar包的路径，
例如：
`java -jar xyz.jar --spring.config.location=file:./application.properties`，
`java -jar xyz.jar --spring.config.location=file:/data/config/application.yml`。

4）项目内部配置
可以在ConfigFileApplicationListener类中看到：
DEFAULT_SEARCH_LOCATIONS = "classpath:/,classpath:/config/,file:./,file:./config/*/,file:./config/"。
`classpath:`开头的就是指的项目内的配置，/表示resources目录，resources/config目录的配置优先级比resources目录的更高。
`file:`开头的是用户指定的项目外的配置，可以指定相对路径（相对jar包的路径），也可以指定绝对路径。如果不指定，则默认加载
jar包所在目录的配置、./config目录下的配置。如果./config目录下还有子目录有配置文件，则先加载子目录的最后再加载./config根目录的。
从SpringBoot的默认加载顺序来看，想要引用外部配置文件，属性值要以`file:`开头，比如：
`java -jar xyz.jar --spring.config.location=file:/data/config/application.yml`。

对于同一目录的application.properties和application.yml文件，application.yml文件先加载，
application.properties后加载，因为旧版本中是以application.properties为默认配置文件的，需要进行兼容。

resources目录下的配置文件可能会根据不同的环境进行命名和划分，比如开发环境、测试环境、灰度环境、生产环境等，
保持application前缀，但实际命名区分为：application-dev、application-test、application-gray、
application-pro。以yml文件为例则分别为：application-dev.yml、application-test.yml、application-gray.yml、
application-pro.yml。
这些不同环境的配置文件，通过在application.yml或bootstrap.yml中用`spring.profile.active`属性进行选择，例如：
`spring.profile.active=pro`，SpringBoot会加载名称为application开头且以pro结尾的.yml或.properties的配置文件，
如果都有，则先加载.yml文件再加载.properties文件，不同属性会被合并，相同属性会被.properties文件覆盖。


5）配置代码类
用@Configuration或@ConfigurationProperties注解声明的类，是SpringBoot要加载的配置类，配置类里的配置最先加载，
因此优先级也最低，所以可以通过resources目录下的.properties或.yml文件进行配置覆盖，也是SpringBoot约定大于配置的
一种体现，一开始按照默认约定来，但用户配置可以覆盖约定配置。

6、SpringBoot的启动过程
初始化阶段：
创建SpringApplication实例。在此过程中确定应用的具体类型，决定是否需要Wen容。
通过spring.factories或imports文件加载应用程序上下文初始化器和监听器。

运行阶段：
发布应用程序启动中事件，准备环境，加载配置，创建应用上下文；
准备上下文，比如注册主类、调用初始化器；
刷新上下文，解析配置类，扫描组件并注册Bean，初始化Web服务器，加载和执行自动配置，发布应用程序启动完成事件；
调用Runner，按`@Order`顺序执行所有`ApplicationRunner`和`CommandLineRunner`的实现类的run方法；
发布应用程序准备就绪事件。

Bean实例创建后存入Spring容器中，也就是内存中，延迟加载能够降低内存资源占用。
注解扫描是在Runner调用前，所以`ApplicationRunner`和`CommandLineRunner`的实现类要加上@Component注解才行，
不加则在Runner调用时不会执行，因为没有实例化，也就是不会执行实例的run方法。

@Component、@Controller、@Service、@Repository、@Bean都是实例创建注解，本身不是没有顺序区分的，
但可以通过@Order或@DependsOn调整创建次序。
比如@DependsOn("userService")，表示在UserService实例化后再进行创建。

@SpringBootApplication由@SpringBootConfiguration、@ComponentScan、@EnableAutoConfiguration三个注解组合而成。

7、SpringBoot服务启动时预加载数据
我们可以利用SpringBoot服务的启动过程，在SpringBoot服务真正提供功能前，做一些全局性的操作，比如数据预加载、全局配置等。
通过实现CommandLineRunner或ApplicationRunner接口，在服务启动时进行数据预加载。
注意要用@Component注解标注，否则覆写的run方法不会被执行。

另外@EnableEurekaServer注解使用后，Eureka默认会使用/eureka路径前缀提供固定管理端点，
如果Controller中也使用/eureka*路径则可能会出现路径冲突而导致Controller接口出现404问题。

8、注解的不同实现方式对性能影响
Java的注解可以分为编译时注解、类加载时注解、运行时注解。

编译时注解：
通过javac的注解处理器在编译时扫描和处理注解，将注解转化为对应代码。
不依赖反射，性能较高，但仅能读取源码中的注解，无法获取运行时信息。
比如Lombok的@Data注解，会在编译时生成属性的getter/setter方法。

类加载时注解：
通过字节码增强工具（如ASM、Java Agent）在类加载时修改字节码，将注解替换为字节码信息。
无需反射，性能较高。比如Spring的@Transactional注解。

运行时注解：
通过反射（如Class.getAnnotation()）动态获取注解信息。
灵活性更高，但反射调用有性能开销，且需要配置包扫描（如Spring的@ComponentScan）。
比如SpringMVC的@RequestMapping路由映射。

注解信息保存在类的元数据区（方法区的`RuntimeVisibleAnnotations`属性）中，
通过JVM的`getAnnotation`相关Native方法读取。

尽量将自定义注解实现为编译时注解，以减少运行时的开销。但其实业务注解更多的是在运行时，因为只有服务运行中业务才能正常请求，
此时业务注解目标就是要在请求处理时进行校验或增强，比如权限校验、日志记录、统一参数传递。

9、Bean实例创建与延迟加载
Bean实例默认是单例模式创建，以减少重复创建对象的开销和不必要的多例资源占用。这里的单例是Spring容器级别的。
单例：singleton
多例：prototype
请求：request
会话：session
应用程序：application

10、AOP原理
动态代理+反射实现。动态代理本身就需要反射技术来调用原始方式，如method.invoke()。

11、自定义注解
自定义注解时需声明为@interface类型。比如：
public @interface MyAnnotation {}

关键属性：
@Target：指定注解可作用的位置，由枚举ElementType定义，包含类声明、字段、方法、参数等类型。
@Retention：指定注解的生命周期或者作用时间，由枚举RetentionPolicy定义，包含源码期、类加载期、运行期。默认是类加载期。
例如：
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
