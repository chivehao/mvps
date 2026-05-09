package run.ikaros.mvp.spring.example.service;

import run.ikaros.mvp.spring.Component;

@Component(beanName = "orderService")
public class DefaultOrderService implements OrderService{
    @Override
    public String getOrderIdByUsername(String username) {
        return username + ":2313043850";
    }
}
