package run.ikaros.mvp.spring.example.service;

import run.ikaros.mvp.spring.Autowired;
import run.ikaros.mvp.spring.BeanNameAware;
import run.ikaros.mvp.spring.Component;

@Component(beanName = "orderService")
public class DefaultOrderService implements OrderService, BeanNameAware {
    private String beanName;
    @Autowired
    private UserService userService;

    @Override
    public String getBeanName() {
        return this.beanName;
    }

    @Override
    public String getOrderIdByUsername(String username) {
        return username + ":2313043850";
    }

    @Override
    public String getUsername() {
        if (this.userService == null) return null;
        return this.userService.getUsername();
    }

    @Override
    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

}
