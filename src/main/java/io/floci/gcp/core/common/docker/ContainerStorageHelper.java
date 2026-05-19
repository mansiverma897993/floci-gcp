package io.floci.gcp.core.common.docker;

import io.floci.gcp.config.EmulatorConfig;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Central helper for sidecar container volume management.
 *
 * Two modes:
 * - Named-volume (default) — manages per-resource Docker named volumes labelled floci-gcp=true.
 * - Host-path (legacy) — active when storage.host-persistent-path is set to an absolute path.
 */
public final class ContainerStorageHelper {

    private static final Logger LOG = Logger.getLogger(ContainerStorageHelper.class);

    private ContainerStorageHelper() {}

    public static String resourceName(String service, String volumeId, String fallbackId) {
        return "floci-gcp-" + service + "-" + (volumeId != null ? volumeId : fallbackId);
    }

    public static boolean isNamedVolumeMode(EmulatorConfig config) {
        return !config.storage().hostPersistentPath().startsWith("/");
    }

    public static void applyStorage(
            ContainerBuilder.Builder builder,
            ContainerLifecycleManager lifecycleManager,
            String service,
            String volumeId,
            String fallbackId,
            String internalMount) {
        String volumeName = resourceName(service, volumeId, fallbackId);
        lifecycleManager.ensureVolume(volumeName);
        builder.withNamedVolume(volumeName, internalMount);
    }

    public static void removeStorage(
            EmulatorConfig config,
            ContainerLifecycleManager lifecycleManager,
            String service,
            String volumeId,
            String fallbackId) {
        String volumeName = resourceName(service, volumeId, fallbackId);
        boolean isMemory = "memory".equals(config.storage().mode());
        if (isMemory || config.storage().pruneVolumesOnDelete()) {
            lifecycleManager.removeVolume(volumeName);
        } else {
            LOG.infov("Retained Docker volume {0}. Remove manually: docker volume rm {0}", volumeName);
        }
    }

    public static void ensureHostDir(String hostDataPath) {
        try {
            Files.createDirectories(Path.of(hostDataPath));
        } catch (IOException e) {
            LOG.errorv("Failed to create data directory {0}: {1}", hostDataPath, e.getMessage());
        }
    }
}
