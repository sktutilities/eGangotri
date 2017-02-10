import ch.qos.logback.classic.AsyncAppender
import ch.qos.logback.classic.PatternLayout
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.FileAppender
import org.apache.log4j.RollingFileAppender

import static ch.qos.logback.classic.Level.INFO

def LOG_PATH = "target"
appender("Console-Appender", ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = "[%-5level] %d{yyyy-MM-dd HH:mm:ss}  %c{1} - %msg%n"
    }
}
appender("File-Appender", FileAppender) {
    file = "${LOG_PATH}/egangotri.log"
    encoder(PatternLayoutEncoder) {
        pattern = "[%-5level] %d{yyyy-MM-dd HH:mm:ss}  %c{1} - %msg%n"
        outputPatternAsHeader = true
    }
}
logger("com.egangotri", INFO, ["Console-Appender", "File-Appender" ], false)
root(INFO, ["Console-Appender"])