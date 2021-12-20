package load.test.app.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({ GeneratorsConfig.class, PriceThrottllerConfig.class })
public class LoadTestAppConfig {

}
