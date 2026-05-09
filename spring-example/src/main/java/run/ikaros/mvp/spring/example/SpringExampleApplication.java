package run.ikaros.mvp.spring.example;

import run.ikaros.mvp.spring.ApplicationContext;
import run.ikaros.mvp.spring.example.service.UserService;

public class SpringExampleApplication {
    static void main(String[] args) {
        ApplicationContext applicationContext = new ApplicationContext(ApplicationConfig.class);

        UserService userService = (UserService) applicationContext.getBean("userService");
        // String username = userService.getUsername();
        // System.out.println("hello " + username);
        UserService userService2 = (UserService) applicationContext.getBean("userService");
        System.out.println(userService);
        System.out.println(userService2);
    }
}
