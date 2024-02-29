package com.example.dynatraceloggingexample

import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController


@RestController
class RootController {
    private val logger = LoggerFactory.getLogger(RootController::class.java)

    @GetMapping
    fun index(): String {
        val result = "Index hit at ${System.currentTimeMillis()}"
        logger.info(result)
        return result
    }
}