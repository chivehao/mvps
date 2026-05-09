package run.ikaros.mvp.spring;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class ApplicationContext {
    private final Class<?> clazz;
    /**
     * Bean定义对象的存储Map.
     */
    private final static Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>();
    /**
     * 单例Bean缓存，也称为一级缓存，作用是存储单例Bean对象，和有效解决循环依赖的问题。
     */
    private final static Map<String, Object> singletonBeanMap = new ConcurrentHashMap<>();

    /**
     * Bean的后置处理器列表。
     */
    private final static List<BeanPostProcessor> beanPostProcessorList = new ArrayList<>();

    public ApplicationContext(Class<?> clazz) {
        this.clazz = clazz;

        scan(clazz);
    }

    private static void scan(Class<?> clazz) {
        // 解析配置，创建 Bean定义对象
        // 获取配置类的 @ComponentScan 信息
        if (!clazz.isAnnotationPresent(ComponentScan.class)) {
            throw new IllegalArgumentException("Config class should add @ComponentScan.");
        }
        String basePackageName = clazz.getDeclaredAnnotation(ComponentScan.class).packageName();
        // 扫描包下的类，拿到 @Component 的对象
        ClassLoader classLoader = clazz.getClassLoader();
        URL resource = classLoader.getResource(basePackageName.replace('.', '/'));
        URL classPathResource = classLoader.getResource("");
        Path dirPath;
        Path classPathDirPath;
        try {
            assert resource != null;
            assert classPathResource != null;
            dirPath = Paths.get(resource.toURI());
            classPathDirPath = Paths.get(classPathResource.toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        if (dirPath.toFile().isFile()) {
            return;
        }
        for (File file : Objects.requireNonNull(dirPath.toFile().listFiles())) {
            String absolutePath = file.getAbsolutePath();
            if (!absolutePath.endsWith(".class")) {
                continue;
            }
            String basePath = classPathDirPath.toFile().getAbsolutePath();
            String clazzName = absolutePath.substring(
                            absolutePath.indexOf(basePath)
                                    + basePath.length() + 1,
                            absolutePath.lastIndexOf('.'))
                    .replace(File.separatorChar, '.');
            Class<?> loadClass;
            try {
                loadClass = classLoader.loadClass(clazzName);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }

            if (!loadClass.isAnnotationPresent(Component.class)) {
               continue;
            }

            // 后置处理器直接反射实例化放到列表里去，
            // 需要注意的是Spring里面BeanPostProcessor并不是反射直接创建，也是交给ioc容器管理的。
            if (BeanPostProcessor.class.isAssignableFrom(loadClass)) {
                try {
                    Constructor<?> constructor = loadClass.getDeclaredConstructor();
                    BeanPostProcessor instance = (BeanPostProcessor) constructor.newInstance();
                    beanPostProcessorList.add(instance);
                } catch (NoSuchMethodException | InvocationTargetException | InstantiationException |
                         IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }

            Component component = loadClass.getDeclaredAnnotation(Component.class);
            boolean isPrototype = false;
            if (loadClass.isAnnotationPresent(Scope.class)) {
                Scope scope = loadClass.getDeclaredAnnotation(Scope.class);
                if ("prototype".equalsIgnoreCase(scope.value())) {
                    isPrototype = true;
                }
            }
            BeanDefinition beanDefinition = new BeanDefinition();
            beanDefinition.setClazz(loadClass);
            beanDefinition.setScope(isPrototype ? "prototype" : "singleton");
            beanDefinitionMap.putIfAbsent(component.beanName(), beanDefinition);
        }
    }

    public Object getBean(String beanName) {
        if (!beanDefinitionMap.containsKey(beanName)) {
            throw new BeanDefinitionNotExistsException(beanName);
        }
        final BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);

        // 原型模式，直接新建Bean返回
        if ("prototype".equalsIgnoreCase(beanDefinition.getScope())) {
            return createBean(beanName, beanDefinition);
        }

        // 双重校验锁，平衡单例模式并发性能
        if (singletonBeanMap.containsKey(beanName)) {
            return singletonBeanMap.get(beanName);
        }
        synchronized (singletonBeanMap) {
            if (singletonBeanMap.containsKey(beanName)) {
                return singletonBeanMap.get(beanName);
            }
            Object beanObj = createBean(beanName, beanDefinition);
            singletonBeanMap.putIfAbsent(beanName, beanObj);
            return beanObj;
        }
    }

    private Object createBean(String beanName, BeanDefinition beanDefinition) {
        // 实例化 Bean
        Object instance = createBeanInstanceWithReflect(beanDefinition);

        // 设置属性，IOC相关，依赖注入
        Class<?> cls = beanDefinition.getClazz();
        for (Field field : cls.getDeclaredFields()) {
            if (!field.isAnnotationPresent(Autowired.class)) {
                continue;
            }
            try {
                field.setAccessible(true);
                String fieldName = field.getName();
                Object fieldValue = getBean(fieldName);
                field.set(instance, fieldValue);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } finally {
                field.setAccessible(false);
            }
        }

        // Aware 相关接口
        if (BeanNameAware.class.isAssignableFrom(cls)) {
            Method setBeanNameMethod;
            try {
                setBeanNameMethod = cls.getDeclaredMethod("setBeanName", String.class);
                setBeanNameMethod.setAccessible(true);
                setBeanNameMethod.invoke(instance, beanName);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        //实例化之后，初始化之前操作，BeanPostProcessor 前置处理。
        for (BeanPostProcessor beanPostProcessor : beanPostProcessorList) {
            beanPostProcessor.postProcessBeforeInitialization(instance, beanName);
        }

        //初始化，如 InitializingBean 等特定的接口
        if (InitializingBean.class.isAssignableFrom(cls)) {
            Method afterPropertiesSet;
            try {
                afterPropertiesSet = cls.getDeclaredMethod("afterPropertiesSet");
                afterPropertiesSet.setAccessible(true);
                afterPropertiesSet.invoke(instance);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }



        //实例化之后，初始化之后操作，BeanPostProcessor 后置处理。
        for (BeanPostProcessor beanPostProcessor : beanPostProcessorList) {
            beanPostProcessor.postProcessAfterInitialization(instance, beanName);
        }

        return instance;
    }

    private static Object createBeanInstanceWithReflect(BeanDefinition beanDefinition) {
        // 不存在，则根据Bean定义，反射创建Bean对象，并放入单例对象池。
        Class<?> clazz = beanDefinition.getClazz();
        Constructor<?> constructor;
        Object beanObj;
        try {
            constructor = clazz.getDeclaredConstructor(null);
            beanObj = constructor.newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        return beanObj;
    }
}
