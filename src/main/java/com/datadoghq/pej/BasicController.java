package com.datadoghq.pej;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BasicController {

    private static final Logger logger = LoggerFactory.getLogger(BasicController.class);

    @RequestMapping("/Callme")
    public String service() throws InterruptedException {

        doSomeStuff("Hello");
        Thread.sleep(2000L);
        doSomeOtherStuff( "World!");
        logger.info("In Service");
        return "Ok\n";

    }

    private String doSomeStuff(String somestring) throws InterruptedException {
        String astring;
        astring = String.format("Hello, %s!", somestring);
        Thread.sleep(250L);
        logger.info("In doSomeStuff()");
        return astring;

    }

    private void doSomeOtherStuff(String somestring) throws InterruptedException {
        Thread.sleep(180L);
        logger.info("In doSomeOtherStuff()");
        Thread.sleep(320L);
    }

}