package run.ikaros.mvp.spring;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public class ApplicationContext {
    private final Class<?> clazz;

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
            System.out.println(component.beanName());
        }

    }

    public Object getBean(String beanName) {
        return null;
    }
}
