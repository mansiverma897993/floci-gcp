package io.floci.gcp.lifecycle;

import io.floci.gcp.core.common.ServiceRegistry;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;

@Path("/health")
@Produces(MediaType.APPLICATION_JSON)
public class HealthController {

    private final ServiceRegistry serviceRegistry;

    @Inject
    public HealthController(ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }

    @GET
    public Response health() {
        return Response.ok(Map.of(
                "services", serviceRegistry.getServices(),
                "version", resolveVersion())).build();
    }

    static String resolveVersion() {
        String env = System.getenv("FLOCI_GCP_VERSION");
        return (env != null && !env.isBlank()) ? env : "dev";
    }
}
