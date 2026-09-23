package com.itheima.config;

import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Locale;

@Configuration
public class WindowsTomcatConfig {

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> windowsNio2Connector() {
        return factory -> {
            if (System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("windows")) {
                factory.setProtocol("org.apache.coyote.http11.Http11Nio2Protocol");
            }
        };
    }
}
