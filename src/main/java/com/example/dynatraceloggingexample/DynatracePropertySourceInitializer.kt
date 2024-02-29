package com.example.dynatraceloggingexample

import org.springframework.boot.env.OriginTrackedMapPropertySource
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.PropertiesPropertySource
import java.io.FileInputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.function.BiConsumer


class DynatracePropertySourceInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
    override fun initialize(configurableApplicationContext: ConfigurableApplicationContext) {
        val environment = configurableApplicationContext.environment

        val dynatrace = mutableMapOf<String, String>()
        try {
            println("Checking logs ${Paths.get("/home/vcap/app/.java-buildpack/dynatrace_one_agent/log/java").toFile().listFiles()}")
            Paths.get("/home/vcap/app/.java-buildpack/dynatrace_one_agent/log/java").toFile().listFiles()?.forEach { file ->
                println("Log File $file")
                Files.readAllLines(file.toPath()).filter { it.contains("Resource Attributes") }.forEach { line ->
                    val split = line.split("'")
                    println("Log Line $line $split")
                    try {
                        val (prefix, key, colon, value) = split
                        dynatrace[key] = value
                    } catch (e: Exception) {
                        println("Split $split ${e.message}")
                    }
                }
            }
            println("maps $dynatrace")
            environment.propertySources.addFirst(
                OriginTrackedMapPropertySource("oneagent_logs", dynatrace)
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }
}