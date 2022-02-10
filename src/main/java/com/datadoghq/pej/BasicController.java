package com.datadoghq.pej;


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
    private HttpServletRequest request;

    @Autowired
    private RestTemplate restTemplate;


    @RequestMapping("/Upstream")
    public String service() throws InterruptedException {

        Map<String,String> mapinject=new HashMap<>();
        HttpHeaders headers = new HttpHeaders();

        mapinject.put("X-Subway-Payment","token");
        mapinject.put("X-Favorite-Food", "pizza");
        headers.setAll(mapinject);

        restTemplate.postForEntity("http://localhost:8080/Downstream", new HttpEntity<>(headers), String.class).getBody();

        Thread.sleep(2000L);
        logger.info("In Upstream");
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

        Thread.sleep(2000L);
        logger.info("In Downstream");

        return "Ok\n";
    }

}