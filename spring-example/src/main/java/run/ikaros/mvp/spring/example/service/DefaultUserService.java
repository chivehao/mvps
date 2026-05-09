package run.ikaros.mvp.spring.example.service;

import run.ikaros.mvp.spring.Component;

@Component(beanName = "userService")
public class DefaultUserService implements UserService {
    @Override
    public String getUsername() {
        return "Tom";
    }
}
