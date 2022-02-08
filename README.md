## Solution #5: Passing span instances across threads ==== WIP =======

### Goal of this activity (`05` branch)

In the previous activity we introduced the `asChilOf()` method that is meant to assign explicitly a parent span to a given span
This section will outline a practical use case using `asChildOf()` primitive with a multithreaded version of our application.
We will show how span objects can be passed across thread boundaries and also how asynchronous activities are reflected in traces.


### Main steps

* Providing an additional method that will be spun off by the main thread tied to our controller class
* Adding a thread that will execute a simple activity that will be traced

### Adding a new method that will be invoked by one of the two existing methods.

First off, let's add a third method that will be used by one of the existing methods. Let's call it `doSomeFinalStuff()`
Its signature can be similar to the other ones. But it needs at least to have one argument that will receive the span object.

```java
private void doSomeFinalStuff(Span parentSpan, String somestring) throws InterruptedException {
        Span span = tracer.buildSpan("doSomeFinalStuff").asChildOf(parentSpan).start();
        try (Scope scope = tracer.activateSpan(span)) {
            Thread.sleep(400L);
            logger.info("In doSomeFinalStuff()");
        } finally {
            span.finish();
        }
        System.out.println(somestring);
}
```

There is nothing much that really differs from the other methods.

### Adding a new thread

This step will involve creating a new thread from within one of the two other methods. Let's say `doSomeOtherStuff()`    
We will add an anonymous Thread whose sole purpose will be to call the newly created method for which a new span will be generated.
We will then have to decide which parent span will get assigned to that span. In order to do so we will modify `doSomeOtherStuff()` as follows:

**_Before_**

````java

    private void doSomeOtherStuff(Span parentSpan, String somestring) throws InterruptedException {
        Span span = tracer.buildSpan("doSomeOtherStuff").asChildOf(parentSpan).start();
        try (Scope scope = tracer.activateSpan(span)) {
            logger.info("In doSomeOtherStuff()");
        } finally {
            span.finish();
        }
        System.out.println(somestring);
        Thread.sleep(320L);
    }
````

**_After_**

```java
    private void doSomeOtherStuff(Span parentSpan, String somestring) throws InterruptedException {
        Span span = tracer.buildSpan("doSomeOtherStuff").asChildOf(parentSpan).start();
        try (Scope scope = tracer.activateSpan(span)) {

            Thread.sleep(180L);
            new Thread(
                    new Runnable() {
                        @SneakyThrows
                        public void run() {
                            doSomeFinalStuff(parentSpan, "Bonjour!");
                        }
                    }
            ).start();

            logger.info("In doSomeOtherStuff()");
        } finally {
            span.finish();
        }
        System.out.println(somestring);
        Thread.sleep(320L);
    }
```

**Note**: At this point, you will also need to consider importing an additional class manually if you use a Text editor.
This is generally handled automatically by IDEs (IntelliJ or Eclipse).
If you have to do it manually, add the following to the import section of your `BaseController` class

```java
import lombok.SneakyThrows;
```

This allows using the `SneakyThrows` annotation which is a convenience provided by the project lombok.
It essentially avoids typing extra code, as we would normally need to add a try/catch clauses manually.
The annotation does that for us.


**Observations**:


**Exercise**:


**Final remark**:



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