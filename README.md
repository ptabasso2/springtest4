## Activity #2: Auto-instrumenting the spring boot application

### Goal of this activity (`02` branch)

In the previous exercise, we familiarized ourselves with the structure of the application that will be used throughout the lab. <br>
This section will describe how automatic instrumentation is done using the java agent.
The instructions for downloading the java agent can be found [here](https://docs.datadoghq.com/tracing/setup_overview/setup/java/?tab=containers#java-installation)
But this branch actually already contains the agent named `dd-java-agent.jar`


### Test the application with Datadog

**_1. Start the  Datadog Agent_**

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


**_2. Using the Java agent_<br>**


<pre style="font-size: 12px">
COMP10619:~ pejman.tabassomi$ java -javaagent:./dd-java-agent.jar \
-Ddd.service=springtest4 -Ddd.env=dev -Ddd.version=1.0 -Ddd.profiling.enabled=true \
-XX:FlightRecorderOptions=stackdepth=256 -Ddd.logs.injection=true -Ddd.trace.sample.rate=1
-jar build/libs/springtest4-1.0.jar

</pre>


**_3. Run the test several times_**
<pre style="font-size: 12px">
COMP10619:~ pejman.tabassomi$ curl localhost:8080/Callme
Ok
</pre>

**_4. Check the results in the Datadog UI (APM traces)_<br>**
https://app.datadoghq.com/apm/traces


