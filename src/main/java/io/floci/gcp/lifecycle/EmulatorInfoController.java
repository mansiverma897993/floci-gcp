package io.floci.gcp.lifecycle;

import io.floci.gcp.config.EmulatorConfig;
import io.floci.gcp.core.common.ServiceRegistry;
import io.floci.gcp.lifecycle.inithook.InitializationHook;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.LinkedHashMap;
import java.util.Map;

@Path("/_floci-gcp")
@Produces(MediaType.APPLICATION_JSON)
public class EmulatorInfoController {

    private final ServiceRegistry serviceRegistry;
    private final InitLifecycleState initLifecycleState;
    private final EmulatorConfig config;

    @Inject
    public EmulatorInfoController(ServiceRegistry serviceRegistry,
                                  InitLifecycleState initLifecycleState,
                                  EmulatorConfig config) {
        this.serviceRegistry = serviceRegistry;
        this.initLifecycleState = initLifecycleState;
        this.config = config;
    }

    @GET
    @Path("/health")
    public Response health() {
        return Response.ok(Map.of(
                "services", serviceRegistry.getServices(),
                "version", HealthController.resolveVersion())).build();
    }

    @GET
    @Path("/info")
    public Response info() {
        return Response.ok(Map.of(
                "version", HealthController.resolveVersion(),
                "port", config.port(),
                "defaultProject", config.defaultProjectId())).build();
    }

    @GET
    @Path("/init")
    public Response init() {
        Map<String, Object> completed = new LinkedHashMap<>();
        completed.put("boot", initLifecycleState.isBootCompleted());
        completed.put("start", initLifecycleState.isStartCompleted());
        completed.put("ready", initLifecycleState.isReadyCompleted());
        completed.put("shutdown", initLifecycleState.isShutdownStarted());

        Map<String, Object> scripts = new LinkedHashMap<>();
        for (InitializationHook hook : InitializationHook.values()) {
            scripts.put(hook.getResponseKey(), initLifecycleState.getScripts(hook).stream()
                    .map(r -> Map.of("script", r.script(), "state", r.state(), "return_code", r.returnCode()))
                    .toList());
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("completed", completed);
        body.put("scripts", scripts);
        return Response.ok(body).build();
    }
}
