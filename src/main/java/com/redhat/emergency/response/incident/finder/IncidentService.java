package com.redhat.emergency.response.incident.finder;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class IncidentService {

    private static final Logger log = LoggerFactory.getLogger(IncidentService.class);

    @ConfigProperty(name = "incident-service.url")
    String serviceUrl;

    @Inject
    Vertx vertx;

    WebClient client;

    void onStart(@Observes StartupEvent e) {
        int servicePort = serviceUrl.contains(":") ? Integer.parseInt(serviceUrl.substring(serviceUrl.indexOf(":") + 1)) : 8080;
        String serviceHost = serviceUrl.contains(":") ? serviceUrl.substring(0, serviceUrl.indexOf(":")) : serviceUrl;
        client = WebClient.create(vertx, new WebClientOptions().setDefaultHost(serviceHost).setDefaultPort(servicePort));
    }

    public Uni<JsonArray> incidentsByName(String name) {
        if (!name.startsWith("%")) {
            name = "%25" + name;
        }
        if (!name.endsWith("%")) {
            name = name + "%25";
        }
        name = name.replaceAll("\\s+", "%20");
        return client.get("/incidents/byname/" + name).send().onItem().apply(resp -> {
            if (resp.statusCode() != 200) {
                log.error("Error when calling incident service. Return code " + resp.statusCode());
                throw new WebApplicationException(resp.statusCode());
            } else {
                return resp.bodyAsJsonArray();
            }
        });
    }


}