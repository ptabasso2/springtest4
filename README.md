## Solution #4: Manual tracing using the `asChildOf()` opentracing idiom

### Goal of this activity (`04` branch)

In the previous activity we've been able to successfully instrument our application using the datadog java api and generate spans and traces.
We've also seen that those spans are linked together following a parent/child dependency pattern.
In this section we will change our code so that we can explicitly assign the parent span to a given span.
This is something that may be needed in certain use cases. For example if the application uses multiple threads, we will need to pass the span context across the thread boundaries.
The work base for this exercise will be the code state at the end of activity `03` (Cf branch `03s`).

### Main steps

* Changing the code implementation in order to introduce Opentracing's `asChildOf()` method.
* This change will occur for the two subsequent spans created for both `doSomeStuff()` and `doSomeOtherStuff()`
* The `service()` method signature will remain unchanged as it is the parent span for the two other methods.
  But its implementation will change as the two other method signatures will be modified.
* The modification will only target the class where spans are created (`BaseController`).
  The `Application` remains unchanged as this does not impact the tracer instantiation operation.


### Introducing the `asChildOf()` method.


Let's change the implementation details of the `service()` method.
This change mainly consists of changing the way `doSomeStuff()` and `doSomeOtherStuff()` are called.
Their signature will now be changed so that they will take an additional argument that is used to pass the parent span object as follows:


**_Before_**

```java
@RequestMapping("/Callme")
public String service() throws InterruptedException {

        Span span = tracer.buildSpan("Service").start();
        try (Scope scope = tracer.activateSpan(span)) {
            span.setTag("customer_id", "45678");
            doSomeStuff("Hello");
            Thread.sleep(2000L);
            doSomeOtherStuff( "World!");
            logger.info("In Service");
        } finally {
            span.finish();
        }
        return "Ok\n";

}
```

**_After_**

```java
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
```


We are now left with:
1. Changing the implementation of `doSomeStuff()` and `doSomeOtherStuff()` as each of them will receive the parent span.
2. Create a span that will be marked as a child using the `asChildOf()` method


**_Before_**

```java
private String doSomeStuff(String somestring) throws InterruptedException {

        String astring;
        Span span = tracer.buildSpan("doSomeStuff").start();
        try (Scope scope1 = tracer.activateSpan(span)) {
            astring = String.format("Hello, %s!", somestring);
            Thread.sleep(250L);
            logger.info("In doSomeStuff()");
        } finally {
            span.finish();
        }
        return astring;

}
```

**_After_**

```java
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
```

**Exercise**:

Now as an exercise, you will want to apply the same changes to the `doSomeOtherStuff()`


**Final remark**:

At this stage, the objective is well achieved, we managed to change the implementation of our application
This will not change much in the UI when observing the trace layout and comparing it with what was obtained in the previous exercise


### Build the application

<pre style="font-size: 12px">
COMP10619:~ pejman.tabassomi$ ./gradlew build

Deprecated Gradle features were used in this build, making it incompatible with Gradle 7.0.
Use '--warning-mode all' to show the individual deprecation warnings.
See https://docs.gradle.org/6.9.1/userguide/command_line_interface.html#sec:command_line_warnings

BUILD SUCCESSFUL in 1s
3 actionable tasks: 3 executed

</pre>


At this stage, the artifact that will be produced (`springtest4-1.0.jar`) will be placed under the `./build/libs` directory that gets created during the build process.


### Start the  Datadog Agent (If not already running)

For the sake of simplicity, we will be running the Datadog agent through its containerized version.
Please provide your API key

<pre style="font-size: 12px">
COMP10619:~ pejman.tabassomi$ docker run -d --rm -h datadog --name datadog_agent \ 
-v /var/run/docker.sock:/var/run/docker.sock:ro \
-v /proc/:/host/proc/:ro \
-v /sys/fs/cgroup/:/host/sys/fs/cgroup:ro \
-p 8126:8126 -p 8125:8125/udp -e DD_API_KEY=xxxxxxxxxxxxxxxxxxxxxxx \
-e DD_TAGS=env:datadoghq.com -e DD_APM_ENABLED=true \
-e DD_APM_NON_LOCAL_TRAFFIC=true -e DD_PROCESS_AGENT_ENABLED=true \
-e DD_LOG_LEVEL=debug gcr.io/datadoghq/agent:7
</pre>


### Run the application

Running the application is fairly simple:

<pre style="font-size: 12px">
COMP10619:SpringTest4 pejman.tabassomi$ java -Ddd.service=springtest4 \
-Ddd.env=dev -Ddd.version=1.0 -jar build/libs/springtest4-1.0.jar

LOGBACK: No context given for c.q.l.core.rolling.SizeAndTimeBasedRollingPolicy@143110009

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::        (v2.2.2.RELEASE)

2022-02-06 22:58:26 [main] INFO  com.datadoghq.pej.Application - Starting Application on COMP10619.local with PID 76957 (/Users/pejman.tabassomi/SpringTest4/build/libs/springtest4-1.0.jar started by pejman.tabassomi in /Users/pejman.tabassomi/SpringTest4)
2022-02-06 22:58:26 [main] INFO  com.datadoghq.pej.Application - No active profile set, falling back to default profiles: default
2022-02-06 22:58:27 [main] INFO  o.s.b.w.e.tomcat.TomcatWebServer - Tomcat initialized with port(s): 8080 (http)
2022-02-06 22:58:27 [main] INFO  o.a.catalina.core.StandardService - Starting service [Tomcat]
2022-02-06 22:58:27 [main] INFO  o.a.catalina.core.StandardEngine - Starting Servlet engine: [Apache Tomcat/9.0.29]
2022-02-06 22:58:27 [main] INFO  o.a.c.c.C.[Tomcat].[localhost].[/] - Initializing Spring embedded WebApplicationContext
2022-02-06 22:58:27 [main] INFO  o.s.web.context.ContextLoader - Root WebApplicationContext: initialization completed in 906 ms
2022-02-06 22:58:27 [main] INFO  o.s.s.c.ThreadPoolTaskExecutor - Initializing ExecutorService 'applicationTaskExecutor'
2022-02-06 22:58:27 [main] INFO  o.s.b.w.e.tomcat.TomcatWebServer - Tomcat started on port(s): 8080 (http) with context path ''
2022-02-06 22:58:27 [main] INFO  com.datadoghq.pej.Application - Started Application in 7.019 seconds (JVM running for 7.486)
2022-02-06 22:58:28 [dd-task-scheduler] INFO  datadog.trace.core.StatusLogger - DATADOG TRACER CONFIGURATION {"version":"0.90.0~32708e53ec","os_name":"Mac OS X",
"os_version":"10.15.7","architecture":"x86_64","lang":"jvm","lang_version":"12.0.2","jvm_vendor":"Oracle Corporation","jvm_version":"12.0.2+10",
"java_class_version":"56.0","http_nonProxyHosts":"null","http_proxyHost":"null","enabled":true,"service":"springtest4","agent_url":"http://localhost:8126",
"agent_error":true,"debug":false,"analytics_enabled":false,"sampling_rules":[{},{}],"priority_sampling_enabled":true,"logs_correlation_enabled":true,
"profiling_enabled":false,"appsec_enabled":false,"dd_version":"0.90.0~32708e53ec","health_checks_enabled":true,"configuration_file":"no config file present",
"runtime_id":"f535c690-eaad-442e-b291-ba11b8b9a66a","logging_settings":{},"cws_enabled":false,"cws_tls_refresh":5000}

</pre>

Note that the last line refers to the Datadog Tracer with a set of default and provided system properties.
The provided ones (service, env, version) where specified above when launching the app.


### Test the application

In another terminal run the following command, you should receive the answer `Ok`

<pre style="font-size: 12px">
COMP10619:~ pejman.tabassomi$ curl localhost:8080/Callme
Ok
</pre>


### Check the results in the Datadog UI (APM traces)
https://app.datadoghq.com/apm/traces