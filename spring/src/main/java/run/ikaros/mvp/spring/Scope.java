package run.ikaros.mvp.spring;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Bean的类型，默认不加该注解是单例bean，加该注解则是原型Bean。
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Scope {
    /**
     * singleton-单例Bean
     * prototype-原型Bean
     * .
     */
    String value() default "prototype";
}
