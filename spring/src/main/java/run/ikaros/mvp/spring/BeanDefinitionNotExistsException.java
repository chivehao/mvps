package run.ikaros.mvp.spring;

public class BeanDefinitionNotExistsException extends RuntimeException {
    public BeanDefinitionNotExistsException(String message) {
        super(message);
    }
}
