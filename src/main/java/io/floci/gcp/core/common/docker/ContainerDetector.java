package io.floci.gcp.core.common.docker;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

@ApplicationScoped
public class ContainerDetector {

    private static final Logger LOG = Logger.getLogger(ContainerDetector.class);

    private static final String DOCKER_ENV_MARKER = "/.dockerenv";
    private static final String PODMAN_ENV_MARKER = "/run/.containerenv";
    private static final String CGROUP_V1_FILE = "/proc/1/cgroup";
    private static final String MOUNTINFO_FILE = "/proc/self/mountinfo";
    private static final String[] CGROUP_MARKERS = {"docker", "kubepods", "libpod", "moby",
            "containerd", "cri-containerd"};
    private static final String[] MOUNTINFO_MARKERS = {"/docker/", "/libpod-", "/moby/",
            "/containerd/", "/cri-containerd-"};

    private volatile Boolean cachedResult;

    public boolean isRunningInContainer() {
        if (cachedResult != null) {
            return cachedResult;
        }
        cachedResult = detect();
        LOG.infov("Container detection result: {0}", cachedResult);
        return cachedResult;
    }

    private boolean detect() {
        if (fileExists(DOCKER_ENV_MARKER)) {
            return true;
        }
        if (fileExists(PODMAN_ENV_MARKER)) {
            return true;
        }
        if (hasContainerEnvVariable()) {
            return true;
        }
        if (hasCgroupV1Markers()) {
            return true;
        }
        return hasMountInfoMarkers();
    }

    private boolean hasContainerEnvVariable() {
        String containerEnv = getEnv("container");
        if (containerEnv != null && !containerEnv.isBlank()) {
            return true;
        }
        String dotnetContainer = getEnv("DOTNET_RUNNING_IN_CONTAINER");
        if ("true".equalsIgnoreCase(dotnetContainer)) {
            return true;
        }
        String genericContainer = getEnv("CONTAINER");
        return genericContainer != null && !genericContainer.isBlank();
    }

    private boolean hasCgroupV1Markers() {
        return fileContainsAny(CGROUP_V1_FILE, CGROUP_MARKERS);
    }

    private boolean hasMountInfoMarkers() {
        if (!fileExists(MOUNTINFO_FILE)) {
            return false;
        }
        try {
            String content = readFileContent(Path.of(MOUNTINFO_FILE));
            return content.lines()
                    .anyMatch(line -> isRootMountInfoLine(line) && containsAny(line, MOUNTINFO_MARKERS));
        } catch (IOException e) {
            LOG.debugv("Could not read {0}: {1}", MOUNTINFO_FILE, e.getMessage());
        }
        return false;
    }

    private boolean fileContainsAny(String filePath, String... markers) {
        if (!fileExists(filePath)) {
            return false;
        }
        try {
            String content = readFileContent(Path.of(filePath));
            return containsAny(content, markers);
        } catch (IOException e) {
            LOG.debugv("Could not read {0}: {1}", filePath, e.getMessage());
        }
        return false;
    }

    private boolean isRootMountInfoLine(String line) {
        String[] fields = line.split(" ");
        return fields.length > 4 && "/".equals(fields[4]);
    }

    private boolean containsAny(String content, String... markers) {
        String lower = content.toLowerCase(Locale.ROOT);
        for (String marker : markers) {
            if (lower.contains(marker.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    boolean fileExists(String path) {
        return Files.exists(Path.of(path));
    }

    String getEnv(String name) {
        return System.getenv(name);
    }

    String readFileContent(Path path) throws IOException {
        return Files.readString(path);
    }
}