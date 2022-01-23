
## Tracer switch example with a Spring Boot application 


### Introduction

This is an example of a Spring Boot application where we use Dependency Injection to switch from one tracer implementation to another.<br>
The goal is to take advantage of the spring framework and define beans for each tracer implementation.<br>
This allows anyone to have access to the Jaeger tracer or Datadog tracer by configuration without having to rebuild the application


### Download and install the application

<pre style="font-size: 12px">
git clone https://github.com/ptabasso2/SpringTest4
</pre>

### Configure the tracer
In the `Application` class, we define three beans along with the `@ConditionalOnProperty` which will be used to identify each of the tracers by their type (`tracer.type` property).<br>

Three types are available:
+ Jaeger
+ dd-tracer (This is the Datadog opentracing API (`dd-trace-ot`))
+ dd-java-agent (Tracer that comes with the java agent)


By setting the value to the desired tracer, the corresponding bean will be picked and auto injected by the spring framework everywhere the `@Autowired` annotation is used inside the application.<br>
This property can be set either inside the `application.properties` file or as an option to the JVM (system property) or environment variable. <br>

Exemple:
<pre style="font-size: 12px">
java -Dtracer.type=dd-tracer -jar build/libs/springtest4-1.0.jar
</pre>

Important: Jaeger is the **default type** if nothing is specified in the application.properties file or as system property

### Build the application

<pre style="font-size: 12px">
COMP10619:~ pejman.tabassomi$ ./gradlew build
</pre>


### Test the application with  Datadog

_1. Start the  Datadog Agent_
<pre style="font-size: 12px">
COMP10619:~ pejman.tabassomi$ docker run -d --rm -h datadog --name datadog_agent -v /var/run/docker.sock:/var/run/docker.sock:ro -v /proc/:/host/proc/:ro -v /sys/fs/cgroup/:/host/sys/fs/cgroup:ro -p 8126:8126 -p 8125:8125/udp -e DD_API_KEY=xxxxxxxxxxxxxxxxxxxxxxx -e DD_TAGS=env:datadoghq.com -e DD_APM_ENABLED=true -e DD_APM_NON_LOCAL_TRAFFIC=true -e DD_PROCESS_AGENT_ENABLED=true -e DD_LOG_LEVEL=debug gcr.io/datadoghq/agent:7
</pre>

_2. Run the application_
<pre style="font-size: 12px">
COMP10619:~ pejman.tabassomi$ java -Dtracer.type=dd-tracer -jar build/libs/springtest4-1.0.jar
</pre>

_3. Run the test several times_ 
<pre style="font-size: 12px">
COMP10619:~ pejman.tabassomi$ curl localhost:8080/Callme
Ok
</pre>

_3. Check the results in the Datadog UI (APM traces)_<br>
https://app.datadoghq.com/apm/traces


### Test the application with Jaeger

_1. Start the Jaeger backend_
<pre style="font-size: 12px">
COMP10619:~ pejman.tabassomi$ docker run --rm -p 6831:6831/udp -p 6832:6832/udp -p 16686:16686 jaegertracing/all-in-one:1.7 --log-level=debug
</pre>

_2. Run the application_
<pre style="font-size: 12px">
COMP10619:~ pejman.tabassomi$ java -Dtracer.type=jaeger -jar build/libs/springtest4-1.0.jar
</pre>

_3. Check the results in the Jaeger UI (APM traces)_<br>
http://localhost:16686


