package run.ikaros.mvp.spring.example.service;

import run.ikaros.mvp.spring.BeanNameAware;
import run.ikaros.mvp.spring.Component;

@Component(beanName = "orderService")
public class DefaultOrderService implements OrderService, BeanNameAware {
    private String beanName;

    @Override
    public String getBeanName() {
        return this.beanName;
    }

    @Override
    public String getOrderIdByUsername(String username) {
        return username + ":2313043850";
    }

    @Override
    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

}
