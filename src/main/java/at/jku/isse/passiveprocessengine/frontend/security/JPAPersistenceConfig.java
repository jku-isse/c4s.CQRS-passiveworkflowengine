package at.jku.isse.passiveprocessengine.frontend.security;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "at.jku.isse.passiveprocessengine.frontend.security.persistence")
@PropertySource("classpath:security.datasource.properties")
@EntityScan(basePackages={ "at.jku.isse.passiveprocessengine.frontend.security.persistence" })
public class JPAPersistenceConfig {
    
}
