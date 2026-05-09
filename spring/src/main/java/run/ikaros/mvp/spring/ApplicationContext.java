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

    public ApplicationContext(Class<?> clazz) {
        this.clazz = clazz;

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
        // 实例化
        Object instance = createBeanInstanceWithReflect(beanDefinition);

        // 依赖注入
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

        // 特定的接口
        for (Class<?> clsInterface : cls.getInterfaces()) {
            if (clsInterface == BeanNameAware.class) {
                Method setBeanNameMethod;
                try {
                    setBeanNameMethod = clsInterface.getDeclaredMethod("setBeanName", String.class);
                    setBeanNameMethod.setAccessible(true);
                    setBeanNameMethod.invoke(instance, beanName);
                } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
            if (clsInterface == InitializingBean.class) {
                Method afterPropertiesSet;
                try {
                    afterPropertiesSet = clsInterface.getDeclaredMethod("afterPropertiesSet");
                    afterPropertiesSet.setAccessible(true);
                    afterPropertiesSet.invoke(instance);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
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
