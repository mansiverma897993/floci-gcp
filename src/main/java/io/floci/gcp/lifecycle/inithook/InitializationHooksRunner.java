package io.floci.gcp.lifecycle.inithook;

import io.floci.gcp.lifecycle.InitLifecycleState;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class InitializationHooksRunner {

    private static final Logger LOG = Logger.getLogger(InitializationHooksRunner.class);

    private static final FilenameFilter SCRIPT_FILTER =
            (ignored, name) -> name.endsWith(".sh") || name.endsWith(".py");

    private final HookScriptExecutor executor;
    private final InitLifecycleState state;

    @Inject
    public InitializationHooksRunner(HookScriptExecutor executor, InitLifecycleState state) {
        this.executor = executor;
        this.state = state;
    }

    public boolean hasHooks(InitializationHook hook) {
        return !mergedScripts(hook).isEmpty();
    }

    public void run(InitializationHook hook) throws IOException, InterruptedException {
        List<File> scripts = mergedScripts(hook);
        for (File script : scripts) {
            LOG.infov("Executing {0} hook: {1}", hook.getName(), script.getAbsolutePath());
            try {
                executor.run(script);
                state.addScript(hook, script.getAbsolutePath(), "successful", 0);
            } catch (IllegalStateException e) {
                state.addScript(hook, script.getAbsolutePath(), "error", parseExitCode(e));
                throw e;
            }
        }
    }

    private static List<File> mergedScripts(InitializationHook hook) {
        Map<String, File> merged = new LinkedHashMap<>();
        for (File dir : hook.getPrimaryPaths()) {
            collectScripts(dir, merged);
        }
        for (File dir : hook.getCompatPaths()) {
            collectScripts(dir, merged);
        }
        return merged.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .toList();
    }

    private static void collectScripts(File dir, Map<String, File> target) {
        if (!dir.isDirectory()) return;
        File[] scripts = dir.listFiles(SCRIPT_FILTER);
        if (scripts == null) return;
        for (File script : scripts) {
            target.putIfAbsent(script.getName(), script);
        }
    }

    private static int parseExitCode(IllegalStateException e) {
        String msg = e.getMessage();
        if (msg != null && msg.contains("exited with code ")) {
            try {
                return Integer.parseInt(msg.substring(msg.lastIndexOf(' ') + 1));
            } catch (NumberFormatException ignored) {}
        }
        return 1;
    }
}
