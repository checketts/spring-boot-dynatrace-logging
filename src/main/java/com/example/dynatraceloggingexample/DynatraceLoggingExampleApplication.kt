package com.example.dynatraceloggingexample

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.fromApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class DynatraceLoggingExampleApplication

fun main(args: Array<String>) {
	val app = SpringApplication(DynatraceLoggingExampleApplication::class.java)
	app.addInitializers(DynatracePropertySourceInitializer())
	app.run(*args)
}
