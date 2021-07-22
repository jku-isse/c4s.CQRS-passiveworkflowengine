package impactassessment;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SpringUtil implements ApplicationContextAware {
    private static ApplicationContext context;

    public static <T extends Object> Optional<T> getBean(Class<T> beanClass) {
        try {
            return Optional.of(context.getBean(beanClass));
        } catch (BeansException e) {
            return Optional.empty();
        }
    }

    public static <T extends Object> Optional<T> getBean(Class<T> beanClass, String beanName) {
        try {
            return Optional.of(context.getBean(beanName, beanClass));
        } catch (BeansException e) {
            return Optional.empty();
        }
    }

    public static ApplicationContext getApplicationContext() {
        return context;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }
}