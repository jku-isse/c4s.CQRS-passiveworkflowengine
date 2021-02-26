package impactassessment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ApplicationContext;

@Slf4j
@SpringBootApplication
public class SpringApp extends SpringBootServletInitializer {

    private static ApplicationContext ctx;

    public static void main(String[] args) {
        ctx = SpringApplication.run(SpringApp.class, args);
    }

    public static void shutdown() {
        int exitCode = SpringApplication.exit(ctx, () -> 0);
        log.info("Spring Application stopped with exit code {}", exitCode);
        System.exit(exitCode);
    }
}
