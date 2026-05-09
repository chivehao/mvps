package run.ikaros.mvp.spring.example.service;

import run.ikaros.mvp.spring.Autowired;
import run.ikaros.mvp.spring.Component;
import run.ikaros.mvp.spring.InitializingBean;

@Component(beanName = "userService")
public class DefaultUserService implements UserService, InitializingBean {
    @Autowired
    private OrderService orderService;

    @Override
    public String getUsername() {
        return "Tom";
    }

    @Override
    public String getOrderId() {
        return orderService.getOrderIdByUsername(getUsername());
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("UserService afterPropertiesSet called.");
    }
}
