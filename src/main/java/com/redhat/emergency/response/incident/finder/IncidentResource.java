package com.redhat.emergency.response.incident.finder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.quarkus.vertx.web.Route;
import io.quarkus.vertx.web.RoutingExchange;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class IncidentResource {

    private static final Logger log = LoggerFactory.getLogger(IncidentResource.class);

    @Inject
    IncidentService incidentService;

    @Inject
    MissionService missionService;

    @Inject
    ShelterService shelterService;

    @Route(path = "/incidents", methods = HttpMethod.GET)

    void incidents(RoutingExchange ex) {

        String name = ex.request().getParam("name");
        doGetIncidents(name).subscribe().with(jsonArray -> {
            ex.response().setStatusCode(200).putHeader("Content-Type", "application/json").end(jsonArray.encode());
        }, t -> {
            log.error(t.getMessage(), t);
            ex.serverError().end();
        });
    }

    private Uni<JsonArray> doGetIncidents(String name) {
        log.info("Processing request");
        return incidentService.incidentsByName(name)
                .onItem().produceUni(jsonArray -> {
                    List<Uni<JsonObject>> unis = new ArrayList<>();
                    if (jsonArray.isEmpty()) {
                        return Uni.createFrom().item(() -> jsonArray);
                    }
                    jsonArray.stream().map(o -> (JsonObject)o).forEach(incident -> {
                        unis.add(missionService.missionByIncidentId(incident.getString("id")).onItem().ifNotNull().apply(mission -> {
                            incident.put("destinationLat", mission.getDouble("destinationLat"));
                            incident.put("destinationLon", mission.getDouble("destinationLong"));
                            JsonArray responderLocationHistory = mission.getJsonArray("responderLocationHistory");
                            if (responderLocationHistory != null && !responderLocationHistory.isEmpty()) {
                                JsonObject responderLocation = responderLocationHistory.getJsonObject(responderLocationHistory.size() - 1);
                                incident.put("currentPositionLat", responderLocation.getDouble("lat"));
                                incident.put("currentPositionLon", responderLocation.getDouble("lon"));
                            } else {
                                incident.put("currentPositionLat", new BigDecimal(incident.getString("lat")).doubleValue());
                                incident.put("currentPositionLon", new BigDecimal(incident.getString("lon")).doubleValue());
                            }
                            return incident;
                        }).onItem().produceUni(incident2 -> {
                            if (incident2.containsKey("destinationLat") && incident2.containsKey("destinationLon")) {
                                return shelterService.shelter(BigDecimal.valueOf(incident2.getDouble("destinationLat")), BigDecimal.valueOf(incident2.getDouble("destinationLon")))
                                        .onItem().apply(shelter -> {
                                            incident2.put("destinationName", shelter);
                                            return incident2;
                                        });
                            } else {
                                return Uni.createFrom().item(() -> incident2);
                            }
                        }));
                    });
                    return Uni.combine().all().unis(unis).combinedWith(l -> jsonArray);
                });
    }
}
