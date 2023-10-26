package eu.europa.ec.sante.ehdsi.openncp.tm.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@SpringBootApplication(scanBasePackages = {"eu.europa.ec.sante.ehdsi.openncp.tm"})
@EnableSwagger2
public class TranslationsAndMappingsApplication extends SpringBootServletInitializer {

    private static final Logger logger = LoggerFactory.getLogger(TranslationsAndMappingsApplication.class);

    public static void main(String... args) {
        SpringApplication.run(TranslationsAndMappingsApplication.class, args);
    }
}
