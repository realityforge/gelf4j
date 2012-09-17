## 1.1:

* Fix      : Handle the scenario where the logging frameworks supply a null message.

## 1.0:

* Fix      : Ensure that the MDC data in the log4j appender works with 
             the AsyncAppender. Thanks to Christoph Neuroth.

## 0.9:

* Enhance  : Use Java NIO when sending the packets.
* Enhance  : Improve the testing to ensure delivery across a socket occurs.
* Enhance  : Package the artifacts as OSGi bundles.

## 0.8:

* Enhance  : Improve the documentation.
* Change   : Remove the uncompleted remnants of the log forwarding application.
* Change   : Update to use getop4j CLI argument parser.

## 0.7:

* Initial release
