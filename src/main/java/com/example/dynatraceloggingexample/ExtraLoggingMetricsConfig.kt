package com.example.dynatraceloggingexample

import io.micrometer.core.instrument.MeterRegistry
import org.apache.logging.log4j.core.jmx.RingBufferAdminMBean
import org.springframework.context.annotation.Configuration
import java.lang.management.ManagementFactory
import javax.management.ObjectName

@Configuration
class ExtraLoggingMetricsConfig(
    final val meterRegistry: MeterRegistry,
) {
    final val mbs = ManagementFactory.getPlatformMBeanServer()
    init {
        val asyncLoggerConfig = mbs.queryNames(ObjectName(String.format(RingBufferAdminMBean.PATTERN_ASYNC_LOGGER_CONFIG, "*", "*")), null)?.firstOrNull()
        if(asyncLoggerConfig != null) {
            meterRegistry.gauge("log4j.ring.remainingCapacity", mbs) { m ->
                (m.getAttribute(asyncLoggerConfig, "RemainingCapacity") as Long).toDouble()
            }
            meterRegistry.gauge("log4j.ring.bufferSize", mbs) { m ->
                (m.getAttribute(asyncLoggerConfig, "BufferSize") as Long).toDouble()
            }
        }
    }
}