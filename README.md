## Activity #3: Manual tracing example

### Goal of this activity (`03` branch)

This section shows a practical example of manual tracing where spans are generated using the java sdk.
In the previous exercise, we familiarized ourselves with automatic instrumentation by using the java agent.
Now we are going to take a radically different approach and modify our code to control how the spans and traces are generated.


### Main steps

* Using the sdk and adding it as a dependancy to the project 
* Instantiate a Tracer
* Create a simple trace
* Add metadata and tag our trace

We are using the following basic features of the OpenTracing API:

* a `tracer` instance is used to create a span builder via `buildSpan()`
* each `span` is given an _operation name_, `"Service", "doSomeStuff", "doSomeOtherStuff"` in this case
* builder is used to create a span via `start()`
* each `span` must be finished by calling its `finish()` function
* the start and end timestamps of the span will be captured automatically by the tracer implementation

### Adding the sdk to the project

In order to do so, we will simply add the following dependancy to the dependancy bloc of the `build.gradle` file
`implementation group: 'com.datadoghq', name: 'dd-trace-ot', version: '0.90.0'`

This should look like

```java
dependencies {
        compile("org.springframework.boot:spring-boot-starter-web")
        implementation group: 'com.datadoghq', name: 'dd-trace-ot', version: '0.90.0'
        compileOnly 'org.projectlombok:lombok:1.18.10'
        annotationProcessor 'org.projectlombok:lombok:1.18.10'
}
```

### Instantiate a tracer

In order to get an instance of our tracer, we will actually leverage Spring's "dependency injection" capability 
through which the Spring container “injects” objects into other objects or “dependencies”.

Simply put we will first declare a bean in the `Application` class.
(This mainly consists of annotating the following method using the `@Bean` annotation).
This bean will essentially be a method that is going to build a `Tracer` instance.

In doing so, we will be able to refer to that bean anywhere else in our code through the `@Autowired` annotation.
This annotation allows Spring to resolve and inject collaborating beans into our bean.
We will actually refer to it later in the `BaseController` class. 

Let's first add the following block in the `Application` class:

```java
@Bean
public Tracer ddtracer() {
    Tracer tracer = new DDTracer.DDTracerBuilder().build();
    GlobalTracer.registerIfAbsent(tracer);
    return tracer;
}
```

**Note**: At this point, you will also need to consider importing the various classes manually that are needed if you use a Text editor. 
This is generally handled automatically by IDEs (IntelliJ or Eclipse).
If you have to do it manually, add the following to the import section of your `Application` class

```java
import datadog.opentracing.DDTracer;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;
import org.springframework.context.annotation.Bean;
```


### Creating the traces

It's time now to build and start spans. We will want to do this in the `BaseController` class.
Let's first inject the `Tracer` bean.
This consists of adding these two lines immediately after the Logger instance declaration:

```java
@Autowired
private Tracer tracer;
```

Now that we can access the `Tracer` instance, let's add the tracing idioms in our code:
We will change the three method implementation as follows:

Example with the `service()` method:

**_Before_**

```java
@RequestMapping("/Callme")
public String service() throws InterruptedException {

        doSomeStuff("Hello");
        Thread.sleep(2000L);
        doSomeOtherStuff( "World!");
        logger.info("In Service");
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

**Note**: At this point, you will also need to consider importing the various classes manually that are needed if you use a Text editor.
This is generally handled _automatically_ by IDEs (IntelliJ or Eclipse).
If you have to do it manually, add the following to the import section of your `BaseController` class

```java
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import org.springframework.beans.factory.annotation.Autowired;
```


**Observations**:

+ A span is built and started at the same time. It is being given an _operation name_ `Service`.
+ We use a `try-with-resource` construct that will be responsible for "auto-closing" the scope object
+ We are adding a tag `customer-id` to the span by using the `setTag()` method
+ When all the activities are complete (ex `doSomeStuff()`, `doSomeOtherStuff()` etc...) we need to finish the span explicitly.


**Exercise**:

Now as an exercise, you will want to apply the same changes to the two other methods `doSomeStuff()`, `doSomeOtherStuff()` 
1. Choose the operation name such that it gets named after the method name
2. Add two distinct tags for each of the created spans.

**Final remark**:

At this stage, the objective is well achieved, we managed to instrument our application 
using the instrumentation api and the spans and traces are sent to the backend after 
having been processed by the Datadog Agent.
But we have not detailed the points related to the dependency of the spans between them. 

The method calls as we have seen them do induce an implicit dependency link between nested spans.
Creating and starting a span in the context of an existing span, automatically makes it a child span 
which then becomes the active span. This has the benefit of simplifying avoiding the hassle of managing the parent/child relationships explicitly.

That said, there can be certain use cases where it can be necessary to explicitly assign a parent to a given span. This is done by using the opentracing `asChildOf()` method.
We will see an example in the next activity. 



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