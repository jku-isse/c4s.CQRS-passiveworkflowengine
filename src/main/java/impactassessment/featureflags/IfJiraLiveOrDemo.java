package impactassessment.featureflags;

import org.springframework.context.annotation.Conditional;

import java.lang.annotation.*;

@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(ConditionJiraLiveOrDemo.class)
public @interface IfJiraLiveOrDemo {
}