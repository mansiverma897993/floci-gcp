package io.floci.gcp.lifecycle.inithook;

import java.io.File;
import java.util.List;

public enum InitializationHook {

    BOOT("boot", "boot",
         List.of("/etc/floci-gcp/init/boot.d"),
         List.of()),
    START("startup", "start",
          List.of("/etc/floci-gcp/init/start.d"),
          List.of()),
    READY("ready", "ready",
          List.of("/etc/floci-gcp/init/ready.d"),
          List.of()),
    STOP("shutdown", "shutdown",
         List.of("/etc/floci-gcp/init/stop.d", "/etc/floci-gcp/init/shutdown.d"),
         List.of());

    private final String name;
    private final String responseKey;
    private final List<File> primaryPaths;
    private final List<File> compatPaths;

    InitializationHook(String name, String responseKey,
                       List<String> primaryPaths, List<String> compatPaths) {
        this.name = name;
        this.responseKey = responseKey;
        this.primaryPaths = primaryPaths.stream().map(File::new).toList();
        this.compatPaths = compatPaths.stream().map(File::new).toList();
    }

    public String getName() { return name; }
    public String getResponseKey() { return responseKey; }
    public List<File> getPrimaryPaths() { return primaryPaths; }
    public List<File> getCompatPaths() { return compatPaths; }
}
