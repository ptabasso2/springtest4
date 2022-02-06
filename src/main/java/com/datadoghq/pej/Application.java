package com.datadoghq.pej;

import datadog.opentracing.DDTracer;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.io.IOException;

@Slf4j
@SpringBootApplication
public class Application {

    public static void main(String[] args) throws IOException, InterruptedException {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public Tracer ddtracer() {
        Tracer tracer = new DDTracer.DDTracerBuilder().build();
        GlobalTracer.registerIfAbsent(tracer);
        return tracer;
    }
}
