package com.redhat.emergency.response.incident.finder;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class MissionService {

    private static final Logger log = LoggerFactory.getLogger(MissionService.class);

    @ConfigProperty(name = "mission-service.url")
    String serviceUrl;

    @Inject
    Vertx vertx;

    WebClient client;

    void onStart(@Observes StartupEvent e) {
        int servicePort = serviceUrl.contains(":") ? Integer.parseInt(serviceUrl.substring(serviceUrl.indexOf(":") + 1)) : 8080;
        String serviceHost = serviceUrl.contains(":") ? serviceUrl.substring(0, serviceUrl.indexOf(":")) : serviceUrl;
        client = WebClient.create(vertx, new WebClientOptions().setDefaultHost(serviceHost).setDefaultPort(servicePort).setMaxPoolSize(100).setHttp2MaxPoolSize(100));
    }

    public Uni<JsonObject> missionByIncidentId(String incidentId) {
        return client.get("/api/missions/incident/" + incidentId).send().onItem().transform(resp -> {
            if (resp.statusCode() == 404) {
                return null;
            } else if (resp.statusCode() != 200) {
                log.error("Error when calling mission service. Return code " + resp.statusCode());
                throw new WebApplicationException(resp.statusCode());
            } else {
                return resp.bodyAsJsonObject();
            }
        });
    }

}
