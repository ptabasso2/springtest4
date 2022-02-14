
## APM & tracing lab - the definitive working example showing various techniques to trace a Java application


### Introduction

The purpose of this lab is to cover various activities around tracing and APM. Each activity of the lab is covered in a dedicated branch.
The structure is as follows:

* **main**: Vanilla java application consisting of a Spring Boot application that exposes a single endpoint </br>
* **02**: That section highlights auto-instrumentation done through the use of the java tracer </br>
* **03**: This section shows a practical example of manual tracing where spans are generated using the java sdk </br>
* **04**: Manual tracing using the `asChildOf()` opentracing idiom <br>
* **05**: Asynchronous activities and tracing across thread boundaries
* **06**: Manual tracing covering inter-processing communication using the `tracer.inject()/extract()` methods for context propagation <br>
* **07**: Log injection: Automatic instrumentation and trace_id/span_id injection into logs - WIP - </br>
* **08**: Example of manual tracing combining the sdk and the java agent (use of the tracer loaded by the java agent) - WIP - </br>
* **09**: Example of manual tracing using Jaeger - WIP - </br>
* **10**: Example of automatic tracing using `-Ddd.trace.methods` vs manual tracing and `@Trace` annotation - WIP - </br>
* **11**: Docker and Kubernetes integration - WIP - </br>

In each section, we'll describe the required steps to take in order to reach the goal.
The activities in this lab follow a logical order so that we can get to the more advanced concepts smoothly.

When an activity involves code modifications (starting activity `03`), the solutions for a given activity are available in the "twin" branch suffixed by `'s'` (except for activities in the `main` and `02` branches where there is no code change).
For example, the solution of activity `03` is presented in the branch `03s`.


### Goal of this activity (`main` branch)

This exercise is only meant to familiarize yourself with the structure of the project (directory structure, file names) but also the steps to follow to build, run and test the application.
There won't be much change in the code. Therefore, no solution branch created.

### Clone the repository

<pre style="font-size: 12px">
COMP10619:~ pejman.tabassomi$ git clone https://github.com/ptabasso2/springtest4.git
</pre>

### Pre-requisites

+ About 15 minutes
+ A favorite text editor or IDE
+ JDK 1.8 or later
+ Gradle 4+ or Maven 3.2+


### Directory structure of the project

The example below is the structure after having built the app.

<pre style="font-size: 12px">

COMP10619:SpringTest4 pejman.tabassomi$ tree
.
├── README.md
├── build
│   ├── classes
│   │   └── java
...
│   ├── generated
...
│
│   ├── libs
│   │   └── springtest4-1.0.jar
│   ├── resources
│   │   └── main
│   │       └── application.properties
│   └── tmp
│       ├── bootJar
│       │   └── MANIFEST.MF
│       └── compileJava
│           └── source-classes-mapping.txt
├── build.gradle
├── commands
├── gradle
│   └── wrapper
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── gradlew
├── logs
│   └── sprinttest4.log
└── src
    └── main
        ├── java
        │   └── com
        │       └── datadoghq
        │           └── pej
        │               ├── Application.java
        │               └── BasicController.java
        └── resources
            └── application.properties

31 directories, 16 files

</pre>

The main components of this project can be described as follows:
+ The `src` directory that contains the two class forming our app. The Application class will contain the implementation details to bootstrap the app. It can be seen as the class exposing the main method that will spin up the app. </br>
  The `BasicController` class will contain the details related to the endpoint that will be exposed. </br> There is also a configuration files that will contain the properties / settings that will be used by the application. </br> In the current version, it contains the logger configuration settings
+ The `build.gradle` file is the build configuraton file used by gradle.
+ The `build` directory contains the generated classes and archive file resulting from the compilation/build steps.


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


### Run the application

Running the application is fairly simple:

<pre style="font-size: 12px">
COMP10619:~ pejman.tabassomi$ java -jar build/libs/springtest4-1.0.jar

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::        (v2.2.2.RELEASE)

2022-02-06 19:27:12 [main] INFO  com.datadoghq.pej.Application - Starting Application on COMP10619.local with PID 30132 (/Users/pejman.tabassomi/SpringTest4/build/libs/springtest4-1.0.jar started by pejman.tabassomi in /Users/pejman.tabassomi/SpringTest4)
2022-02-06 19:27:12 [main] INFO  com.datadoghq.pej.Application - No active profile set, falling back to default profiles: default
2022-02-06 19:27:13 [main] INFO  o.s.b.w.e.tomcat.TomcatWebServer - Tomcat initialized with port(s): 8080 (http)
2022-02-06 19:27:13 [main] INFO  o.a.catalina.core.StandardService - Starting service [Tomcat]
2022-02-06 19:27:13 [main] INFO  o.a.catalina.core.StandardEngine - Starting Servlet engine: [Apache Tomcat/9.0.29]
2022-02-06 19:27:13 [main] INFO  o.a.c.c.C.[Tomcat].[localhost].[/] - Initializing Spring embedded WebApplicationContext
2022-02-06 19:27:13 [main] INFO  o.s.web.context.ContextLoader - Root WebApplicationContext: initialization completed in 914 ms
2022-02-06 19:27:13 [main] INFO  o.s.s.c.ThreadPoolTaskExecutor - Initializing ExecutorService 'applicationTaskExecutor'
2022-02-06 19:27:13 [main] INFO  o.s.b.w.e.tomcat.TomcatWebServer - Tomcat started on port(s): 8080 (http) with context path ''
2022-02-06 19:27:13 [main] INFO  com.datadoghq.pej.Application - Started Application in 6.833 seconds (JVM running for 7.26)

</pre>

The application will start a Tomcat server that will load our application that will be listening to connection on port 8080.


### Test the application

In another terminal run the following command, you should receive the answer `Ok`

<pre style="font-size: 12px">
COMP10619:~ pejman.tabassomi$ curl localhost:8080/Callme
Ok
</pre>

### Check the results in the Datadog UI (APM traces)
https://app.datadoghq.com/apm/traces