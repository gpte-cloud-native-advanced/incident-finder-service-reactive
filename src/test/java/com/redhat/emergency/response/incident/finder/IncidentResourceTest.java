package com.redhat.emergency.response.incident.finder;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class IncidentResourceTest {

    @InjectMock
    IncidentService incidentService;

    @InjectMock
    MissionService missionService;

    @InjectMock
    ShelterService shelterService;


    @Test
    void testIncidentResource() {

        when(incidentService.incidentsByName(anyString())).thenReturn(Uni.createFrom()
                .item(() -> new JsonArray().add(new JsonObject().put("id", "incident123"))));

        when(missionService.missionByIncidentId(anyString())).thenReturn(Uni.createFrom()
                .item(() -> new JsonObject().put("destinationLat", 30.1234).put("destinationLong", -77.9876)
                        .put("responderLocationHistory", new JsonArray().add(new JsonObject()
                                .put("lat", 31.4569).put("lon", -78.6549)))));

        when(shelterService.shelter(any(BigDecimal.class), any(BigDecimal.class))).thenReturn(Uni.createFrom().item(() -> "shelter"));

        given().when().queryParam("name", "jones").get("/incidents")
                .then().statusCode(200).body("$.size", is(1), "[0].id", is("incident123"), "[0].currentPositionLat", equalTo(31.4569f),
                    "[0].currentPositionLon", equalTo(-78.6549f), "[0].destinationLat", equalTo(30.1234f), "[0].destinationLon", equalTo(-77.9876f),
                    "[0].destinationName", is("shelter"));
    }

    @Test
    void testIncidentResourceTwoIncidents() {

        when(incidentService.incidentsByName(anyString())).thenReturn(Uni.createFrom()
                .item(() -> new JsonArray().add(new JsonObject().put("id", "incident123")).add(new JsonObject().put("id", "incident456"))));

        when(missionService.missionByIncidentId(anyString())).thenReturn(Uni.createFrom()
                .item(() -> new JsonObject().put("destinationLat", 30.1234).put("destinationLong", -77.9876)
                        .put("responderLocationHistory", new JsonArray().add(new JsonObject()
                                .put("lat", 31.4569).put("lon", -78.6549)))))
                .thenReturn(Uni.createFrom().item(() -> new JsonObject().put("destinationLat", 40.1234).put("destinationLong", -87.9876)
                                .put("responderLocationHistory", new JsonArray().add(new JsonObject()
                                        .put("lat", 41.4569).put("lon", -88.6549)))));

        when(shelterService.shelter(any(BigDecimal.class), any(BigDecimal.class))).thenReturn(Uni.createFrom().item(() -> "shelter"))
                .thenReturn(Uni.createFrom().item(() -> "shelter2"));

        given().when().queryParam("name", "jones").get("/incidents")
                .then().statusCode(200).body("$.size", is(2), "[0].id", is("incident123"), "[0].currentPositionLat", equalTo(31.4569f),
                "[0].currentPositionLon", equalTo(-78.6549f), "[0].destinationLat", equalTo(30.1234f), "[0].destinationLon", equalTo(-77.9876f),
                "[0].destinationName", is("shelter"),
                "[1].id", is("incident456"), "[1].currentPositionLat", equalTo(41.4569f),
                "[1].currentPositionLon", equalTo(-88.6549f), "[1].destinationLat", equalTo(40.1234f), "[1].destinationLon", equalTo(-87.9876f),
                "[1].destinationName", is("shelter2"));
    }

    @Test
    void testIncidentResourceMissionWithEmptyLocationHistory() {

        when(incidentService.incidentsByName(anyString())).thenReturn(Uni.createFrom()
                .item(() -> new JsonArray().add(new JsonObject().put("id", "incident123").put("lat", "30.1234").put("lon", "-77.9876"))));

        when(missionService.missionByIncidentId(anyString())).thenReturn(Uni.createFrom()
                .item(() -> new JsonObject().put("destinationLat", 30.1234).put("destinationLong", -77.9876)
                        .put("responderLocationHistory", new JsonArray())));

        when(shelterService.shelter(any(BigDecimal.class), any(BigDecimal.class))).thenReturn(Uni.createFrom().item(() -> "shelter"));

        given().when().queryParam("name", "jones").get("/incidents")
                .then().statusCode(200).body("$.size", is(1), "[0].id", is("incident123"), "[0].currentPositionLat", equalTo(30.1234f),
                "[0].currentPositionLon", equalTo(-77.9876f), "[0].destinationLat", equalTo(30.1234f), "[0].destinationLon", equalTo(-77.9876f),
                "[0].destinationName", is("shelter"));
    }

    @Test
    void testIncidentResourceEmptyArray() {

        when(incidentService.incidentsByName(anyString())).thenReturn(Uni.createFrom()
                .item(JsonArray::new));

        given().when().queryParam("name", "jones").get("/incidents")
                .then().statusCode(200).body("$.size", is(0));
    }

    @Test
    void testIncidentResourceMissionIsNull() {

        when(incidentService.incidentsByName(anyString())).thenReturn(Uni.createFrom()
                .item(() -> new JsonArray().add(new JsonObject().put("id", "incident123").put("lat", "30.1234").put("lon", "-77.9876"))));

        when(missionService.missionByIncidentId(anyString())).thenReturn(Uni.createFrom().nullItem());

        given().when().queryParam("name", "jones").get("/incidents")
                .then().statusCode(200).body("$.size", is(1), "[0].id", is("incident123"));
    }

}
