
## Manual tracing example based on opentracing with a Spring Boot application 


### Introduction

This is an example of a Spring Boot application where we use Dependency Injection to switch from one tracer implementation to another.<br>
The goal is to take advantage of the spring framework and define beans for each tracer implementation.<br>
This allows anyone to have access to the Jaeger tracer or Datadog tracer by configuration without having to rebuild the application


### Download and install the application

<pre style="font-size: 12px">
git clone https://github.com/ptabasso2/springtest4.git
</pre>

### Configure the tracer
In the `Application` class, we define three beans along with the `@ConditionalOnProperty` which will be used to identify each of the tracers by their type (through the `tracer.type` property).<br>

Three types are available:
+ Jaeger
+ dd-tracer (This is the Datadog opentracing SDK (`dd-trace-ot`))
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

**_1. Start the  Datadog Agent_**

Please provide your API key
<pre style="font-size: 12px">
COMP10619:~ pejman.tabassomi$ docker run -d --rm -h datadog --name datadog_agent -v /var/run/docker.sock:/var/run/docker.sock:ro -v /proc/:/host/proc/:ro -v /sys/fs/cgroup/:/host/sys/fs/cgroup:ro -p 8126:8126 -p 8125:8125/udp -e DD_API_KEY=xxxxxxxxxxxxxxxxxxxxxxx -e DD_TAGS=env:datadoghq.com -e DD_APM_ENABLED=true -e DD_APM_NON_LOCAL_TRAFFIC=true -e DD_PROCESS_AGENT_ENABLED=true -e DD_LOG_LEVEL=debug gcr.io/datadoghq/agent:7
</pre>

**_2. Run the application_**
<pre style="font-size: 12px">
COMP10619:~ pejman.tabassomi$ java -Dtracer.type=dd-tracer -jar build/libs/springtest4-1.0.jar
</pre>

**_3. Run the test several times_** 
<pre style="font-size: 12px">
COMP10619:~ pejman.tabassomi$ curl localhost:8080/Callme
Ok
</pre>

**_4. Check the results in the Datadog UI (APM traces)_<br>**
https://app.datadoghq.com/apm/traces

**_5. Using the Java agent_<br>**

**Important:** If using the java agent (`dd-java-agent.jar`) , please note that some integrations need to be disabled to avoid span duplication in the generated traces. <br>
The duplication of spans results from what the java agent produces based on automatic instrumentation and what is done through manual tracing. <br>
The integrations we are referring to are disabled by setting the following system properties to false (`-Ddd.integration.XXX.enabled=false`)

<pre style="font-size: 12px">
COMP10619:~ pejman.tabassomi$ java -javaagent:./dd-java-agent.jar \
-Dtracer.type=dd-java-agent -Ddd.service=spring4 -Ddd.env=dev -Ddd.integration.spring-web.enabled=false \
-Ddd.integration.tomcat.enabled=false -Ddd.integration.servlet.enabled=false \
-jar build/libs/springtest4-1.0.jar
</pre>


### Test the application with Jaeger

**_1. Start the Jaeger backend_**
<pre style="font-size: 12px">
COMP10619:~ pejman.tabassomi$ docker run --rm -p 6831:6831/udp -p 6832:6832/udp -p 16686:16686 jaegertracing/all-in-one:1.7 --log-level=debug
</pre>

**_2. Run the application_**
<pre style="font-size: 12px">
COMP10619:~ pejman.tabassomi$ java -Dtracer.type=jaeger -jar build/libs/springtest4-1.0.jar
</pre>

**_3. Check the results in the Jaeger UI (APM traces)_<br>**
http://localhost:16686


