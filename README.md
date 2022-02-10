## Activity #6: Manual tracing covering inter-processing communication using the `tracer.inject()/extract()` idioms for context propagation ==== WIP =======

### Goal of this activity (`06` branch)

So far we have been able to explore the different scenarios that allow us to understand how spans and traces are generated.
Our code was exposing a single endpoint and will then invoke methods used to outline the tracing techniques.

In ths activity we will focus on inter-process communication and context propagation across services.
This happens when an upstream service initiates a request that will then be processed by a downstream service.

For the sake of simplicity we will cover this use case by simply relying on a single service that exposes two endpoints 
(This instead of having two distinct services each exposing a single endpoint). 
The first endpoint will be the one hit by the user request. 
The corresponding method will execute some instructions and will in turn issue the request toward the second endpoint.

### Main steps

* We will get rid of the methods used until now that would get executed after the endpoint was hit.
* We will rename the spring handler name from `service()` to `upstream()`
* Adding a second endpoint inside the sema controller class. Its handler will be named `downstream()`
* We will add rely on `tracer.inject()/extract()` method invocations to show how context propagation occurs


### Removing methods and adding a new method handler that will be invoked by the first method.

We will introduce two new beans of type `HttpServletRequest` and `RestTemplate`.
They will respectively be used by the second and first methods. 
`HttpServletRequest` interface is able to allow request information for HTTP Servlets and will be used to extract the header information
(on the receiving end) that will contain among other headers the `trace_id`, `span_id` values injected by the calling service.
The exact headers names are:

* `x-datadog-trace-id` 
* `x-datadog-span-id` 
* `x-datadog-sampling-priority`


**Note**: At this point, you will also need to consider importing an additional class manually if you use a Text editor.
This is generally handled automatically by IDEs (IntelliJ or Eclipse).
If you have to do it manually, add the following to the import section of your `BaseController` class


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