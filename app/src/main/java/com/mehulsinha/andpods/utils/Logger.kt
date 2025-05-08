package com.mehulsinha.andpods.utils

import java.time.LocalDateTime

/**
 * A simple logging utility for Kotlin applications.
 * Supports multiple log levels and customizable output format.
 */
class Logger(
    private val name: String,
    private val minLevel: LogLevel = LogLevel.DEBUG,
    private val formatter: LogFormatter = DefaultLogFormatter()
) {
    /**
     * Log levels supported by the logger
     */
    enum class LogLevel(val value: Int) {
        TRACE(0),
        DEBUG(1),
        INFO(2),
        WARN(3),
        ERROR(4),
        FATAL(5);
    }

    /**
     * Interface for formatting log messages
     */
    interface LogFormatter {
        fun format(level: LogLevel, name: String, message: String): String
    }

    /**
     * Default implementation of LogFormatter
     */
    class DefaultLogFormatter : LogFormatter {
        override fun format(level: LogLevel, name: String, message: String): String {
            val timestamp = LocalDateTime.now().toString()
            return "[$timestamp] ${level.name} [$name] - $message"
        }
    }

    /**
     * Log a message at the specified level
     */
    private fun log(level: LogLevel, message: String) {
        if (level.value >= minLevel.value) {
            println(formatter.format(level, name, message))
        }
    }

    // Convenience methods for each log level
    fun trace(message: String) = log(LogLevel.TRACE, message)
    fun debug(message: String) = log(LogLevel.DEBUG, message)
    fun info(message: String) = log(LogLevel.INFO, message)
    fun warn(message: String) = log(LogLevel.WARN, message)
    fun error(message: String) = log(LogLevel.ERROR, message)
    fun fatal(message: String) = log(LogLevel.FATAL, message)

    companion object {
        /**
         * Create a logger with the name of the calling class
         */
        inline fun <reified T> getLogger(): Logger {
            return Logger(T::class.java.simpleName)
        }
    }
}