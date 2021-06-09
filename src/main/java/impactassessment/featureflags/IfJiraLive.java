package impactassessment.featureflags;

import org.springframework.context.annotation.Conditional;

import java.lang.annotation.*;

@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(ConditionJiraLive.class)
public @interface IfJiraLive {
}
