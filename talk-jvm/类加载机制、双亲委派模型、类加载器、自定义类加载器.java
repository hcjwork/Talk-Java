类加载机制：
加载 - 加载途径、加载方式、加载源数据、加载目的地
验证 - 字节码文件格式、类基本结构
准备 - 静态变量内存分配，赋予默认初始值
解析 - 符号引用替换为直接引用
初始化 - 静态变量赋值、静态代码块执行。

类加载总的来说，就是将Java的.class文件转化为二进制数据读入内存，存放在JVM的方法区区域，并在堆中
创建一个Class对象用以封装在方法区内的数据结构。

类加载的基本过程：
1、加载（Loading）
将Java字节码文件从各种渠道加载到JVM内存中。
2、验证（Verification）
验证字节码文件是否符合JVM规范；验证类元数据是否有效是否缺失。
3、准备（Preparation）
为静态变量分配内存空间，设置默认初始值如0、null、false等。
4、解析（Resolution）
将类、接口、字段、方法等的字符串字面量替换为内存的直接引用。
5、初始化（Initialization）
执行静态变量的赋值语句，执行静态代码块，执行类的构造方法。

其中的验证、准备、解析三个部分又合称为链接过程。

类加载过程详解：
1、加载过程
字节码数据来源有磁盘文件、网络等多种渠道，但最终都会以二进制的形式加载到内存中。
这里的内存指的是JVM内存，主要是方法区，类的元数据如类名、接口名、包名、字段、方法等信息都存储在方法区。
方法区的落地实现在jdk1.8前后有区别，jdk1.8之前称作永久代（permanent generation），jdk1.8开始称作元空间（metaspace）。

方法区是Java运行时数据区（Runtime Data Area）的一部分，而字节码是从java源文件编译而成的静态数据，
所以加载到内存时，需要将字节码的静态数据结构转变为方法区中的运行时数据结构。

JVM在堆中生成一个java.lang.Class对象，作为方法区中运行时数据的访问入口。

数组类本身不通过类加载器加载，而是由JVM直接在堆中创建（数组实例）。

2、验证过程
加载到JVM内存的字节码二进制数据，在解析执行前得先检查有没有问题。
首先就是检查是不是符合Java规范的字节码格式，是不是以魔数0xCAFEBABE开头的，版本号是不是当前JVM能处理的。
其次就是检查类的元数据，基本结构是不是完整的，依赖的类是否正常找到。

3、准备过程
字节码数据验证没有了，准备解析执行。但在解析执行前，需要先给静态变量分配内存空间，并赋予默认初始值比如0、null、false等。
为什么要先进行内存分配，因为有些静态变量是引用类型，解析执行时需要替换为内存地址去找对应的实例，如果不先进行内存分配，
就没有内存地址可以替换了。
除了静态变量，还有静态常量需要分配内存空间并赋值，但静态常量是final修饰的，会在准备阶段直接赋值为代码中所制定的值。

4、解析过程
验证过了，准备也好了，就开始解析字节码指令并执行。
但字节码指令里面包含很多信息，例如全限定类名、实现的接口、继承的类、访问的其他类的字段、调用的其他类的方法等，
此时都是一些字符串字面量，并非是直接可使用的，需要将这些符号引用替换为实际可用的直接引用，也就是内存引用，
这样就能找到当前类所依赖的其他元数据。
如果解析失败，可能会抛出java.lang.NoSuchMethodError等错误。
另外部分解析可能在初始化之后进行，因为有运行时绑定机制，只能在实际运行时才能确定具体调哪个类的方法或字段。
例如类A中有一个字段name，类B继承了类A但没有定义name字段，但在方法中使用了name字段，
这种情况就只能在运行时才能确定最终使用的是父类A的name字段的值。

5、初始化过程
验证无误，准备妥当，解析完毕，开始着手字节码执行。
这一过程主要执行静态变量的赋值语句代码，以及执行静态代码块。

双亲委派模型：
双亲委派模型或机制，指的是JVM的类加载器接到类加载的请求时，会先把类加载请求委派给自己的父类加载器去加载，
父类加载器无法加载时继续往上委派，如果所有的父类加载器都无法加载，最终还是由发起委派的子加载器进行加载。

Java的类加载器分为：自定义类加载器、应用程序类加载器（也称为系统类加载器）、扩展类加载器、启动类加载器。层次从低到高。

在双亲委派模型中，父类加载器接收到类加载委派时，先检查是否已经加载过此类，如果已经加载过则直接返回Class对象，没有加载过就尝试加载。

双亲委派机制的作用：
主要有三个作用，一是保证类只被加载一次，因为加载时都会先检查是否已经加载过了，加过的会直接返回Class对象。
二是保证类加载的安全性，避免同名的类破坏核心类的加载，通过向上委派由java内置的类加载器加载保证安全。
三是进行责任分离，不同的类加载器负责加载不同位置或不同层级的类，避免类加载器任务繁重或混乱失序。

打破双亲委派的情形：
在某些特殊情况可以需要打破双亲委派模型，例如SPI机制、OSGI、热部署等。
具体的有如JDBC驱动加载、模块热部署、Tomcat对Web应用的支持。

类加载器：
启动类加载器 - 核心类库
扩展类加载器 - 扩展类库
应用程序类加载器 - 用户类路径上的类库

java的内置的类加载器有三种，分别为：启动类加载器（Bootstrap ClassLoader）、扩展类加载器（Extension ClassLoader）、
应用程序类加载器（Application ClassLoader）。
其中应用程序类加载器又称作：系统类加载（System ClassLoader）。

这三种类加载器各司其职，负责不同位置的类库的类加载工作。

启动类加载器是顶级父类加载器，其本身没有父类加载器了；扩展类加载器的父类加载器是启动类加载器；应用程序类加载器的父类加载器是扩展类加载器。

启动类加载器由C++语言实现，是JVM内的一部分，负责加载<JAVA_HOME>/lib根目录下的类库；
扩展类加载器由Java语言实现，继承自java.net.URLClassLoader，其父类加载器是启动类加载器，负责加载<JAVA_HOME>/lib/ext目录下的类库；
应用程序类加载器由Java语言实现，父类加载器是扩展类加载器，负责加载用户类路径（ClasPath）上的类库。

自定义类加载器
开发者可以继承java.lang.ClassLoader抽象类，自定义类加载器，但注意不要重写loadClass方法以免破坏双亲委派机制。
重点是重写findClass方法，在此方法中，读取类文件的字节码数据，然后调用defineClass()将字节数组转化为Class对象。
例如：
// 继承ClassLoader抽象类
public class MyClassLoader extends ClassLoader {
    // 类的包路径
    private String classPath;
    public MyClassLoader(String classPath) {
        this.classPath = classPath;
    }
    // 重新findClass方法
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        try {
            // 加载目标类文件的字节数据
            byte[] classData = loadClassData(name);
            // 创建目标类的Class对象。此方法继承自ClassLoader，最终调到native方法，由JVM在堆中生成Class对象
            return defineClass(name, classData, 0, classData.length);
        } catch (IOException e) {
            throw new ClassNotFoundException(name, e);
        }
    }
    private byte[] loadClassData(String className) throws IOException {
        // 类的全路径
        String path = classPath + File.separatorChar +
                className.replace('.', File.separatorChar) + ".class";
        // 把类文件转化为字节数组
        try (FileInputStream is = new FileInputStream(path);
             ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            return os.toByteArray();
        }
    }
}

