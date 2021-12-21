package com.price.processor.test.app.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.price.processor.test.app.services.App;
import com.price.processor.test.app.services.Generators;

@Configuration
@Import(GeneratorsConfig.class)
public class AppConfig {

  @Autowired
  private Generators generators;

  @Value("${duration:PT20S}")
  private String durationString;

  @Bean
  public Duration duration() {
    return Duration.parse(durationString);
  };

  @Bean
  public App app() {
    return new App(generators, duration());
  }
}
