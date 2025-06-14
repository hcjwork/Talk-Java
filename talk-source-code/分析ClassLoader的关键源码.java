Java的类加载有一种双亲委派机制，即当一个类加载器接收到类加载请求时，会先委派给其父类加载器，由父类加载器尝试加载，
如果所有的父类加载器都无法加载，则最终由自身来加载。

类加载的关键类就是：ClassLoader。
最关键的方式是loadClass方法，具体如下：
private final ClassLoader parent;
protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    synchronized (getClassLoadingLock(name)) {
        // First, check if the class has already been loaded
        Class<?> c = findLoadedClass(name);
        if (c == null) {
            long t0 = System.nanoTime();
            try {
                if (parent != null) {
                    c = parent.loadClass(name, false);
                } else {
                    c = findBootstrapClassOrNull(name);
                }
            } catch (ClassNotFoundException e) {
                // ClassNotFoundException thrown if class not found
                // from the non-null parent class loader
            }

            if (c == null) {
                // If still not found, then invoke findClass in order
                // to find the class.
                long t1 = System.nanoTime();
                c = findClass(name);

                // this is the defining class loader; record the stats
                sun.misc.PerfCounter.getParentDelegationTime().addTime(t1 - t0);
                sun.misc.PerfCounter.getFindClassTime().addElapsedTimeFrom(t1);
                sun.misc.PerfCounter.getFindClasses().increment();
            }
        }
        if (resolve) {
            resolveClass(c);
        }
        return c;
    }
}

loadClass的主要流程是：
先检查要加载的类是否已加载过，如果已经加载过则直接返回Class对象，避免了重复加载。
如果当前类没有被加载过，则委派给父类加载器加载，如果没有父类加载器则由启动类加载器尝试加载。
父类加载器同样是先检查是否已加载过，没加载过则委派给自己的父类加载器尝试加载。
就这样从最开始接收到类加载请求的类加载器不断向上委派与尝试，都无法加载时，再由最开始接到类加载请求的类加载器进行加载。

拓展问题：为什么自定义类加载器时，不建议重写loadClass方法？
从loadClass源码可以看到，类加载的双亲委派机制主要就体现在这里，包括父加载器的加载尝试。
如果重写loadClass方法很可能会破坏java自带的安全的类加载机制。

如果想要打破双亲委派机制，就可以重新loadClass方法自定义处理逻辑。
