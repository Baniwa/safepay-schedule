package com.safepay.schedule.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI safePayOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SafePay Schedule API")
                        .description("RESTful API for scheduling payment transfers with dynamic tax computation.")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("SafePay Engineering")
                                .email("engineering@safepay.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}
