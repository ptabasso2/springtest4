package com.datadoghq.pej;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.Scope;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BasicController {

    private static final Logger logger = LoggerFactory.getLogger(BasicController.class);

    @Autowired
    private Tracer tracer;

    @RequestMapping("/Callme")
    public String service() throws InterruptedException {

        Span span = tracer.buildSpan("Service").start();
        try (Scope scope = tracer.activateSpan(span)) {
            span.setTag("customer_id", "45678");
            doSomeStuff(span, "Hello");
            Thread.sleep(2000L);
            doSomeOtherStuff(span, "World!");
            logger.info("In Service");
        } finally {
            span.finish();
        }
        return "Ok\n";
    }

    private String doSomeStuff(Span parentSpan, String somestring) throws InterruptedException {

        String astring;
        Span span = tracer.buildSpan("doSomeStuff").asChildOf(parentSpan).start();
        try (Scope scope1 = tracer.activateSpan(span)) {
            astring = String.format("Hello, %s!", somestring);
            Thread.sleep(250L);
            logger.info("In doSomeStuff()");
        } finally {
            span.finish();
        }
        return astring;

    }

    private void doSomeOtherStuff(Span parentSpan, String somestring) throws InterruptedException {
        Span span = tracer.buildSpan("doSomeOtherStuff").asChildOf(parentSpan).start();
        try (Scope scope1 = tracer.activateSpan(span)) {
            Thread.sleep(180L);
            logger.info("In doSomeOtherStuff()");
        } finally {
            span.finish();
        }
        System.out.println(somestring);
        Thread.sleep(320L);
    }

}