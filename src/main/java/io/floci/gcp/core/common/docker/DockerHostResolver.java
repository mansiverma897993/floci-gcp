package io.floci.gcp.core.common.docker;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.net.InetAddress;

/**
 * Detects the hostname that sidecar containers should use to reach floci-gcp.
 * Returns the container's own Docker network IP when running inside Docker,
 * or host.docker.internal when running natively on the host.
 */
@ApplicationScoped
public class DockerHostResolver {

    private static final Logger LOG = Logger.getLogger(DockerHostResolver.class);

    private static final String HOST_DOCKER_INTERNAL = "host.docker.internal";
    private static final String LINUX_DOCKER_BRIDGE = "172.17.0.1";

    private final ContainerDetector containerDetector;
    private final CurrentContainerNetworkResolver currentContainerNetworkResolver;

    @Inject
    public DockerHostResolver(ContainerDetector containerDetector,
                              CurrentContainerNetworkResolver currentContainerNetworkResolver) {
        this.containerDetector = containerDetector;
        this.currentContainerNetworkResolver = currentContainerNetworkResolver;
    }

    public DockerHostResolver(ContainerDetector containerDetector) {
        this(containerDetector, null);
    }

    public String resolve() {
        if (containerDetector.isRunningInContainer()) {
            if (currentContainerNetworkResolver != null) {
                java.util.Optional<String> currentNetworkIp = currentContainerNetworkResolver.resolveContainerIp();
                if (currentNetworkIp.isPresent()) {
                    LOG.infov("Running in Docker — using current network IP: {0}", currentNetworkIp.get());
                    return currentNetworkIp.get();
                }
            }
            try {
                String ip = InetAddress.getLocalHost().getHostAddress();
                LOG.infov("Running in Docker — using container IP: {0}", ip);
                return ip;
            } catch (Exception e) {
                LOG.warnv("Could not resolve local host address, falling back to bridge IP: {0}", e.getMessage());
                return LINUX_DOCKER_BRIDGE;
            }
        }

        LOG.debugv("Running on host ({0}) — sidecar containers will use host.docker.internal",
                System.getProperty("os.name"));
        return HOST_DOCKER_INTERNAL;
    }

    public boolean isLinuxHost() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("linux") || os.contains("nix") || os.contains("nux");
    }
}