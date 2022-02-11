## Solution #6: Manual tracing for inter-process communication and context propagation using the opentracing's tracer.inject()/extract() methods ==== WIP =======

### Goal of this activity (`06` branch)

So far we have been able to explore different scenarios that allow us to understand how spans and traces are generated.
Our code was originally exposing a single endpoint and would then invoke methods to outline various tracing techniques.

In ths activity we will focus on **inter-process communication** and **context propagation** across services.
This happens when an upstream service initiates a request that will then be processed by a downstream service.

For the sake of simplicity we will address this use case by relying on a single service that exposes two endpoints
(Instead of having two distinct services each exposing a single endpoint).
The first endpoint will be hit by the user request.
The corresponding method will execute some instructions and will in turn issue the request toward the second endpoint.

In order to do so we will start with a simple implementation in our Controller class:
Two spring handlers named `upstream()` and `downstream()` mapping respectively the two endpoints (`/Upstream` and `/Downstream`)

We will then add the necessary code to make the context propagation work.

### Observations related to the application

* A `RestTemplate` bean is declared in the `Application` class and will be autowired in the controller class.
* Another bean `request` of type `HttpServletRequest` is injected in the controller class.
  The `HttpServletRequest` interface is able to allow request information for HTTP Servlets and will be used to extract the header information
  (on the receiving end) that will contain among other headers the `trace_id`, `span_id` values injected by the calling service.
  The exact headers names are:

  * `x-datadog-trace-id`
  * `x-datadog-span-id`
  * `x-datadog-sampling-priority`

(Note that this one doesn't need to be declared as a bean as it is managed under the hood by the spring framework).
* We will rely on `tracer.inject()/extract()` method invocations to show how context propagation occurs on both sides

On the Rest client side (Upstream)
* A map structure `mapinject` (`HashMap` type) that will hold the various headers is declared. There are already two custom headers present as an example.
  That data structure will be reused to add the additional headers (`trace-id`, `span-id` and `sampling-priority-id`) when invoking the `tracer.inject()` method
* An `HttpHeaders` object that will wrap the map above and that will be used when issuing the http request through an `HttpEntity` object.
* `RestTemplate`'s `postForEntity()` method is used to execute a POST operation on the `/Downstream` endpoint, while sending headers at the same time.

On the Rest server side (Downstream)

* A map structure `mapextract` (`HashMap` type) that will be filled with the headers received from the http request through the `HttpServletRequest` object.


### Main steps in this activity

* Tracing instructions in both methods:
  * The `tracer.inject()` call added inside the `upstream()` method
  * The `tracer.extract()` call added inside the `downstream()` method


### The `upstream()` method

**_Before_**

```java
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
```

**_After_**

```java
    @RequestMapping("/Upstream")
    public String service() throws InterruptedException {

        Map<String,String> mapinject=new HashMap<>();
        HttpHeaders headers = new HttpHeaders();

        mapinject.put("X-Subway-Payment","token");
        mapinject.put("X-Favorite-Food", "pizza");

(1)       Span span = tracer.buildSpan("Upstream").start();
(2)       tracer.inject(span.context(), Format.Builtin.HTTP_HEADERS, new TextMapAdapter(mapinject));
        
        headers.setAll(mapinject);

(3)     try (Scope scope = tracer.activateSpan(span)) {
            span.setTag("service.name", "Upstream");
            span.setTag("span.type", "web");
            span.setTag("resource.name", "GET /Upstream");
            span.setTag("resource", "GET /Upstream");
            span.setTag("customer_id", "45678");
(4)         restTemplate.postForEntity("http://localhost:8080/Downstream", new HttpEntity(headers), String.class).getBody();
            Thread.sleep(2000L);
            logger.info("In Upstream");
        } finally {
            span.finish();
        }
        
        return "Ok\n";

    }
```

**Note**: At this point, you will also need to consider importing additional classes manually if you use a Text editor.
This is generally handled automatically by IDEs (IntelliJ or Eclipse).

In the `Àpplication` class add the following imports:

```java
import datadog.opentracing.DDTracer;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;
```

In the `BaseController` class, add the following imports:

```java
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapAdapter;
```


**Observations**

* (1) We use a span builder and start the span at the same time.
* (2) In order to maintain the trace context over the process boundaries and remote calls,
  we need a way to propagate the span context over the wire.
  The OpenTracing API provides two functions in the Tracer interface to do that, `inject(spanContext, format, carrier)` and `extract(format, carrier)`. The `format` parameter refers to one of the three standard encodings (`TEXT_MAP`, `HTTP_HEADERS`, `BINARY`) that define how the span context gets encoded.  
  In our case this will be `HTTP_HEADERS`
* (2) A Carrier is an interface or data structure that’s useful for inter-process communication (IPC). It “carries” the tracing state from one process to another.
  It allows the tracer to write key-value pairs via `put(key, value)` method for a given format
* (3) The try-with-resources bloc is used to activate the span and wrap the previous instructions.
* (4) The Rest call remains the same, the only difference is that after the`inject()` call in (2), the map now will contain the three additional headers.


### The `downstream()` method

**_Before_**

```java
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
```

This service does very basic things:
* It gets the header from the request through an `HttpServletRequest` object.
* Fills a HashMap that will be reused next to build the span context through the `extract()` method
* Pause the thread and log some text.


**_After_**


```java
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

(1)     SpanContext parentSpan = tracer.extract(Format.Builtin.HTTP_HEADERS, new TextMapAdapter(mapextract));
(2)     Span span = tracer.buildSpan("Downstream").asChildOf(parentSpan).start();

(3)     try (Scope scope = tracer.activateSpan(span)) {
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
```

**Note**: At this point, you will also need to consider importing additional classes manually if you use a Text editor.
This is generally handled automatically by IDEs (IntelliJ or Eclipse).

In the `BaseController` class, add the following import:
```java
import io.opentracing.SpanContext;
```

**Observations**
* (1) When calling the `extract()` method the `mapextract` map will provide the elements to build the `SpanContext`
* (2) From that `SpanContext` we can get a `Span` object that will be marked as a child of the parent span. The parent span was initiated by the http request.
  Therefore the instructions we are going to trace will be associated to this new child span.
* (3) Enclosing the instructions in the block where the span is active.

**Exercise**

**Final remark**


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
COMP10619:~ pejman.tabassomi$ curl localhost:8080/Upstream
Ok
</pre>


### Check the results in the Datadog UI (APM traces)
https://app.datadoghq.com/apm/traces