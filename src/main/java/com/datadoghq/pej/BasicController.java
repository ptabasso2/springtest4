package com.datadoghq.pej;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;


@RestController
public class BasicController {

    private static final Logger logger = LoggerFactory.getLogger(BasicController.class);

    @Autowired
    private Tracer tracer;

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private RestTemplate restTemplate;


    @RequestMapping("/Upstream")
    public String service() throws InterruptedException {

        Map<String,String> mapinject=new HashMap<>();
        HttpHeaders header = new HttpHeaders();

        Span span = tracer.buildSpan("Upstream").start();
        tracer.inject(span.context(), Format.Builtin.HTTP_HEADERS, new TextMapAdapter(mapinject));
        header.setAll(mapinject);

        try (Scope scope = tracer.activateSpan(span)) {
            span.setTag("service.name", "Upstream");
            span.setTag("span.type", "web");
            span.setTag("resource.name", "GET /Upstream");
            span.setTag("resource", "GET /Upstream");
            span.setTag("customer_id", "45678");
            String rs = restTemplate.postForEntity("http://localhost:8080/Downstream", new HttpEntity(header), String.class).getBody();
            Thread.sleep(2000L);
            logger.info("In Upstream");
        } finally {
            span.finish();
        }
        return "Ok\n";
    }


    @RequestMapping("/Downstream")
    public String downstream() throws InterruptedException {

        Enumeration<String> e = request.getHeaderNames();
        Map<String, String> mapextract = new HashMap<>();

        while (e.hasMoreElements()) {
            // add the names of the request headers into the spanMap
            String key = e.nextElement();
            String value = request.getHeader(key);
            mapextract.put(key, value);
        }
        SpanContext parentSpan = tracer.extract(Format.Builtin.HTTP_HEADERS, new TextMapAdapter(mapextract));

        Span span = tracer.buildSpan("Downstream").asChildOf(parentSpan).start();
        try (Scope scope = tracer.activateSpan(span)) {
            span.setTag("service.name", "Downstream");
            span.setTag("span.type", "web");
            span.setTag("resource.name", "POST /Downstream");
            span.setTag("resource", "POST /Downstream");
            span.setTag("customer_id", "45678");
            Thread.sleep(2000L);
            logger.info("In Downstream");
        } finally {
            span.finish();
        }
        return "Ok\n";
    }

}