package run.ikaros.mvp.spring.example.service;

import run.ikaros.mvp.spring.Autowired;
import run.ikaros.mvp.spring.Component;

@Component(beanName = "userService")
public class DefaultUserService implements UserService {
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
}
