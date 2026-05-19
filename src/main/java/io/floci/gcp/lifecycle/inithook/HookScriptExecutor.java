package io.floci.gcp.lifecycle.inithook;

import io.floci.gcp.config.EmulatorConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class HookScriptExecutor {

    private static final Logger LOG = Logger.getLogger(HookScriptExecutor.class);

    private final EmulatorConfig.InitHooksConfig initHooksConfig;

    @Inject
    public HookScriptExecutor(EmulatorConfig config) {
        this.initHooksConfig = config.initHooks();
    }

    public void run(File scriptFile) throws IOException, InterruptedException {
        run(scriptFile.getParentFile(), scriptFile.getName());
    }

    public void run(File hookDirectory, String scriptFileName) throws IOException, InterruptedException {
        String command = scriptFileName.endsWith(".py") ? "python3" : initHooksConfig.shellExecutable();
        Process process = new ProcessBuilder(command, scriptFileName)
                .directory(hookDirectory)
                .inheritIO()
                .start();
        run(process, scriptFileName);
    }

    void run(Process process, String scriptFileName) throws InterruptedException {
        int exitCode = waitForExit(process, scriptFileName);
        if (exitCode != 0) {
            throw new IllegalStateException(
                    String.format("Hook script failed: %s exited with code %d", scriptFileName, exitCode));
        }
    }

    private int waitForExit(Process process, String scriptFileName) throws InterruptedException {
        try {
            long timeout = initHooksConfig.timeoutSeconds();
            if (!process.waitFor(timeout, TimeUnit.SECONDS)) {
                terminate(process, scriptFileName);
                throw new IllegalStateException(
                        String.format("Hook script timed out after %d seconds: %s", timeout, scriptFileName));
            }
            return process.exitValue();
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    private void terminate(Process process, String scriptFileName) throws InterruptedException {
        process.destroy();
        if (process.isAlive()) {
            long grace = initHooksConfig.shutdownGracePeriodSeconds();
            if (!process.waitFor(grace, TimeUnit.SECONDS)) {
                LOG.debugv("Force-killing hook script: {0}", scriptFileName);
                process.destroyForcibly();
                process.waitFor(grace, TimeUnit.SECONDS);
            }
        }
    }
}
