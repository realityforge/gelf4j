GELF4j - GELF client-side integration library
=============================================

[![Build Status](https://secure.travis-ci.org/realityforge/gelf4j.png?branch=master)](http://travis-ci.org/realityforge/gelf4j)

What is GELF4j
--------------

[GELF](http://www.graylog2.org/about/gelf) attempts to address the shortcomings of standard syslog format. The protocol supports large messages through a chunked message protocol and allows end users to define arbitrary fields in the log message.

GELF4j is a client-side integration library to send GELF messages. GELF4j has integration points with the major logging frameworks; JDK Logging, Log4J and Logback. GELF4j also includes a simple command line applications to send a single GELF message as well as an application to forward local logs to a GELF compliant server (i.e. [Graylog2](http://www.graylog2.org)).

How to use GELF4j
-----------------

To integrate into a logging framework simply drop the jar into your classpath and configure the logging framework to use it. The jars are available form the github projects downloads. The "-all" artifact includes all the required dependencies. At some point i the future the libraries mayt attempt to be placed in maven central.

The following examples demonstrate it's use in various frameworks.

JDK Logging
-----------

    ...
    handlers = gelf4j.logging.GelfHandler
    .level = ALL
    gelf4j.logging.GelfHandler.level = ALL
    gelf4j.logging.GelfHandler.host=graylog.example.com
    gelf4j.logging.GelfHandler.port=1942
    gelf4j.logging.GelfHandler.compressedChunking=false
    gelf4j.logging.GelfHandler.defaultFields={"environment": "DEV", "application": "MyAPP"}
    gelf4j.logging.GelfHandler.additionalFields={"thread_name": "threadName", "exception": "exception"}
    ...

Logback XML Example
-------------------

    ...
    <appender name="gelf" class="gelf4j.logback.GelfAppender">
        <host>graylog.example.com</host>
        <port>1942</port>
        <compressedChunking>false</compressedChunking>
        <defaultFields>{"environment": "DEV", "application": "MyAPP"}</defaultFields>
        <additionalFields>{"thread_name": "threadName", "exception": "exception"}</additionalFields>
    </appender>

    <root level="info">
        <appender-ref ref="gelf" />
    </root>
    ...

Log4j XML Example
-----------------

    ...
    <!-- define the appender -->
    <appender name="gelf" class="gelf4j.log4j.GelfAppender">
        <param name="Threshold" value="INFO"/>
        <param name="host" value="graylog.example.com"/>
        <param name="port" value="1942"/>
        <param name="compressedChunking" value="false"/>
        <param name="defaultFields" value='{"environment": "DEV", "application": "MyAPP"}'/>
        <param name="additionalFields" value='{"thread_name": "threadName", "exception": "exception"}'/>
    </appender>

    <!-- add the appender to a logger -->
    <root>
        <priority value="INFO"/>
        <appender-ref ref="graylog2"/>
    </root>
    ...


Log4j Properties Example
------------------------

    ...
    log4j.appender.gelf.host=graylog.example.com
    log4j.appender.gelf.port=1942
    log4j.appender.gelf.compressedChunking=false
    log4j.appender.gelf.defaultFields={"environment": "DEV", "application": "MyAPP"}
    log4j.appender.gelf.additionalFields={"thread_name": "threadName", "exception": "exception"}
    log4j.rootLogger=INFO, gelf
    ...

Options
-------

Each of the different log systems can be configured with similar parameters;

- **host**: The hostname or ip address of the GELF compliant server where it will send the GELF messages
- **port**: Port on which the gelf compliant server is listening. Default: 12201 (*optional*)
- **compressedChunking**: Set to true to use the compressed format when chunking messages. The format used by Graylog2 server versions 0.9.6 and later. Set to false for Graylog2 server < 0.9.6. Default: true (*optional*)
- **defaultFields**: A JSON format object for constant values merged ito the message. Default: {} (*optional*)
- **additionalFields**: A JSON object that describes dynamic fields that should be merged into the message. The key indicates the name of the field in message while the value is a symbolic key that indicates the source or type information that should be merged into the message. The supported symbolic keys vary between the different supported logging frameworks. Default: {"threadName": "threadName", "exception": "exception", "loggerName": "loggerName", "timestampMs": "timestampMs"} (*optional*)

The set of symbolic keys supported by different logging frameworks is listed below. In addition both Log4j and Logback support the notion of "Mapped Diagnostic Contexts" or MDCs. The integration with these frameworks is such that if a symbolic key is not one of the several listed below, the integration will use the value in the MDC under the specified key.
- **threadName**: The thread name in which the log message was generated.
- **timestampMs**: The time at which the log message was generated in milliseconds.
- **loggerName**: The name of the logger that generated the message.
- **exception**: The exception message if any that was logged with the message.
- **loggerNdc**: The nested diagnostic context of the message. (Log4j only).
- **threadId**: The unique id of the thread in the system. (JDK Logging only).
- **SourceClassName**: The name of the class in which the log message was generated. (JDK Logging only).
- **SourceMethodName**: The name of the method in which the log message was generated. (JDK Logging only).

How-to Build
------------

Gelf4j uses [Apache Buildr](http://buildr.apache.org) and requires you to have already built and installed the [getopt4j](https://github.com/realityforge/getopt4j) library.

Credits
-------

The project was a rewrite and merge of multiple existing products, notably [gelfj](https://github.com/t0xa/gelfj) by Anton Yakimov and [logback-gelf](https://github.com/Moocar/logback-gelf) by Anthony Marcar. While most (all?) of the code has been rewritten since then, the motivation and inspiration and credit goes to those projects. See the [LICENSE](LICENSE) for additional nodes on credit.
