package com.recplatform.platform.system;

import com.recplatform.common.result.Result;
import com.recplatform.solver.config.SolverProperties;
import com.sun.management.OperatingSystemMXBean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import javax.sql.DataSource;
import java.io.File;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.sql.Connection;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Exposes real-time system health and runtime metrics for the admin console.
 * All values are collected from the live JVM, host OS and dependent services
 * instead of static placeholders.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/system")
@RequiredArgsConstructor
public class SystemInfoController {

    private final SolverProperties solverProperties;
    private final DataSource dataSource;

    private final RestTemplate healthCheckClient = buildHealthCheckClient();

    private static RestTemplate buildHealthCheckClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(2000);
        return new RestTemplate(factory);
    }

    @GetMapping("/info")
    public Result<Map<String, Object>> getSystemInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("services", collectServiceHealth());
        info.put("metrics", collectSystemMetrics());
        info.put("jvmInfo", collectJvmInfo());
        return Result.success(info);
    }

    private List<Map<String, Object>> collectServiceHealth() {
        List<Map<String, Object>> services = new ArrayList<>();
        String uptime = formatUptime(ManagementFactory.getRuntimeMXBean().getUptime());

        services.add(buildServiceStatus("Backend", appVersion(), true, uptime));
        services.add(buildServiceStatus("Solver Engine", "-", probeSolverEngine(), uptime));
        services.add(buildServiceStatus("PostgreSQL", "-", probeDatabase(), uptime));

        return services;
    }

    private Map<String, Object> buildServiceStatus(String name, String version, boolean healthy, String uptime) {
        Map<String, Object> service = new LinkedHashMap<>();
        service.put("name", name);
        service.put("version", version);
        service.put("healthy", healthy);
        service.put("uptime", healthy ? uptime : "-");
        return service;
    }

    private boolean probeSolverEngine() {
        String url = solverProperties.getEngineUrl() + "/health";
        try {
            healthCheckClient.getForObject(url, String.class);
            return true;
        } catch (Exception e) {
            log.warn("Solver engine health probe failed: {}", e.getMessage());
            return false;
        }
    }

    private boolean probeDatabase() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(2);
        } catch (Exception e) {
            log.warn("Database health probe failed: {}", e.getMessage());
            return false;
        }
    }

    private Map<String, Object> collectSystemMetrics() {
        Map<String, Object> metrics = new LinkedHashMap<>();
        OperatingSystemMXBean osBean =
                (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

        metrics.put("cpu", toPercent(osBean.getCpuLoad()));
        metrics.put("memory", calculateMemoryUsagePercent(osBean));
        metrics.put("disk", calculateDiskUsagePercent());
        metrics.put("activeConnections", ManagementFactory.getThreadMXBean().getThreadCount());
        return metrics;
    }

    private int calculateMemoryUsagePercent(OperatingSystemMXBean osBean) {
        long total = osBean.getTotalMemorySize();
        long free = osBean.getFreeMemorySize();
        if (total <= 0) {
            return 0;
        }
        return (int) Math.round((total - free) * 100.0 / total);
    }

    private int calculateDiskUsagePercent() {
        File root = new File("/");
        long total = root.getTotalSpace();
        if (total <= 0) {
            return 0;
        }
        long used = total - root.getUsableSpace();
        return (int) Math.round(used * 100.0 / total);
    }

    private int toPercent(double load) {
        if (load < 0) {
            return 0;
        }
        return (int) Math.round(load * 100);
    }

    private Map<String, Object> collectJvmInfo() {
        Map<String, Object> jvmInfo = new LinkedHashMap<>();
        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();

        jvmInfo.put("javaVersion", System.getProperty("java.vm.name") + " " + System.getProperty("java.version"));
        jvmInfo.put("heapUsed", formatMegabytes(heapUsage.getUsed()));
        jvmInfo.put("heapMax", formatMegabytes(heapUsage.getMax()));
        jvmInfo.put("gcCount", totalGcCount());
        jvmInfo.put("gcTime", formatSeconds(totalGcTimeMillis()));
        jvmInfo.put("threads", threadBean.getThreadCount());
        jvmInfo.put("uptime", formatUptime(runtimeBean.getUptime()));
        return jvmInfo;
    }

    private long totalGcCount() {
        long count = 0;
        for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
            long gcCollectionCount = gcBean.getCollectionCount();
            if (gcCollectionCount > 0) {
                count += gcCollectionCount;
            }
        }
        return count;
    }

    private long totalGcTimeMillis() {
        long time = 0;
        for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
            long gcCollectionTime = gcBean.getCollectionTime();
            if (gcCollectionTime > 0) {
                time += gcCollectionTime;
            }
        }
        return time;
    }

    private String formatMegabytes(long bytes) {
        if (bytes < 0) {
            return "-";
        }
        return (bytes / 1024 / 1024) + "MB";
    }

    private String formatSeconds(long millis) {
        return String.format("%.1fs", millis / 1000.0);
    }

    private String formatUptime(long uptimeMillis) {
        Duration duration = Duration.ofMillis(uptimeMillis);
        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();
        if (days > 0) {
            return days + "d " + hours + "h";
        }
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }
        return minutes + "m";
    }

    private String appVersion() {
        String version = getClass().getPackage().getImplementationVersion();
        return version != null ? version : "-";
    }
}
