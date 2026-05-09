package run.ikaros.mvp.spring.example.service;

import run.ikaros.mvp.spring.BeanPostProcessor;
import run.ikaros.mvp.spring.Component;

@Component(beanName = "commonBeanPostProcessor")
public class CommonBeanPostProcessor implements BeanPostProcessor {
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        System.out.println("Exec CommonBeanPostProcessor postProcessBeforeInitialization.");
        return BeanPostProcessor.super.postProcessBeforeInitialization(bean, beanName);
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        System.out.println("Exec CommonBeanPostProcessor postProcessAfterInitialization.");
        return BeanPostProcessor.super.postProcessAfterInitialization(bean, beanName);
    }
}
