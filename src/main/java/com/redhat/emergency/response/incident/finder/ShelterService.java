package com.redhat.emergency.response.incident.finder;

import java.math.BigDecimal;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ShelterService {

    private static final Logger log = LoggerFactory.getLogger(ShelterService.class);

    @ConfigProperty(name = "disaster-service.url")
    String serviceUrl;

    @Inject
    Vertx vertx;

    WebClient client;

    void onStart(@Observes StartupEvent e) {
        int servicePort = serviceUrl.contains(":") ? Integer.parseInt(serviceUrl.substring(serviceUrl.indexOf(":") + 1)) : 8080;
        String serviceHost = serviceUrl.contains(":") ? serviceUrl.substring(0, serviceUrl.indexOf(":")) : serviceUrl;
        client = WebClient.create(vertx, new WebClientOptions().setDefaultHost(serviceHost).setDefaultPort(servicePort).setMaxPoolSize(100).setHttp2MaxPoolSize(100));
    }

    public Uni<JsonArray> shelters() {
        return client.get("/shelters").send().onItem().apply(resp -> {
            if (resp.statusCode() != 200) {
                log.error("Error when calling disaster service. Return code " + resp.statusCode());
                throw new WebApplicationException(resp.statusCode());
            } else {
                return resp.bodyAsJsonArray();
            }
        });
    }

    public Uni<String> shelter(BigDecimal lat, BigDecimal lon) {
        return shelters().onItem().apply(jsonArray -> jsonArray.stream().map(o -> (JsonObject) o)
                .filter(shelter -> BigDecimal.valueOf(shelter.getDouble("lat")).equals(lat) && BigDecimal.valueOf(shelter.getDouble("lon")).equals(lon))
                .map(shelter -> shelter.getString("name")).findFirst().orElse(""));
    }

}
