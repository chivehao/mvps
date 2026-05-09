package run.ikaros.mvp.spring.example;

import run.ikaros.mvp.spring.ApplicationContext;
import run.ikaros.mvp.spring.example.service.OrderService;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SpringExampleApplication {
    static void main(String[] args) {
        ApplicationContext applicationContext = new ApplicationContext(ApplicationConfig.class);

        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(100, 200, 1, TimeUnit.SECONDS, new ArrayBlockingQueue<>(100));
        for (int i = 0; i < 200; i++) {
            threadPoolExecutor.execute(()->{
                // 并发量大的时候会出现NullPointerException 此时获取的是刚实例完未进行依赖注入的不完全对象，"this.userService" is null
                OrderService orderService = (OrderService) applicationContext.getBean("orderService");
                System.out.println("[" + Thread.currentThread().getName() + "] username " + orderService.getUsername());
            });
        }
        threadPoolExecutor.shutdownNow();


    }
}
