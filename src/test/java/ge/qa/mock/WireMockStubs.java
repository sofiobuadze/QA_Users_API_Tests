package ge.qa.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ge.qa.config.Config;
import ge.qa.model.User;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

import java.util.List;

/**
 * Dynamically registers the stubs in a running WireMock instance through its admin API.
 * Users are serialized from {@link User} objects with Jackson.
 */
public final class WireMockStubs {

    public static final String ERROR_HEADER = "X-Simulate-Error";

    public static final User ALICE = User.builder().id(1).name("Alice").age(30).gender("female").build();
    public static final User BOB = User.builder().id(2).name("Bob").age(25).gender("male").build();

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private WireMockStubs() {
    }

    public static void register() {
        String admin = Config.baseUrl() + "/__admin";

        try {
            Response reset = RestAssured.given().post(admin + "/mappings/reset");
            if (reset.statusCode() != 200) {
                throw new IllegalStateException("WireMock reset failed: HTTP " + reset.statusCode());
            }
        } catch (RuntimeException e) {
            throw new IllegalStateException("WireMock is not reachable at " + Config.baseUrl()
                    + ". Start it first: ./start-wiremock.sh", e);
        }

        // Positive: GET /users -> all users (fallback, lowest priority)
        post(admin, stub(10, null, null, 200, List.of(ALICE, BOB)));

        // Positive: filters
        post(admin, stub(1, "age", "30", 200, List.of(ALICE)));
        post(admin, stub(1, "age", "25", 200, List.of(BOB)));
        post(admin, stub(1, "gender", "female", 200, List.of(ALICE)));
        post(admin, stub(1, "gender", "male", 200, List.of(BOB)));

        // Negative: invalid age -> 400
        post(admin, stub(1, "age", "-1", 400, MAPPER.createObjectNode().put("error", "Bad Request")));

        // Negative: unknown gender -> empty list
        post(admin, stub(1, "gender", "unknown", 200, List.of()));

        // Negative: GET /users -> 500 (selected by the X-Simulate-Error header)
        ObjectNode error = stub(1, null, null, 500, MAPPER.createObjectNode().put("error", "Internal Server Error"));
        ((ObjectNode) error.get("request")).putObject("headers")
                .putObject(ERROR_HEADER).put("equalTo", "true");
        post(admin, error);
    }

    private static ObjectNode stub(int priority, String param, String value, int status, Object body) {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("priority", priority);

        ObjectNode request = root.putObject("request");
        request.put("method", "GET");
        request.put("urlPath", "/users");
        if (param != null) {
            request.putObject("queryParameters").putObject(param).put("equalTo", value);
        }

        ObjectNode response = root.putObject("response");
        response.put("status", status);
        response.putObject("headers").put("Content-Type", "application/json");
        response.set("jsonBody", MAPPER.valueToTree(body));
        return root;
    }

    private static void post(String admin, ObjectNode stub) {
        Response r = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(stub.toString())
                .post(admin + "/mappings");
        if (r.statusCode() != 201) {
            throw new IllegalStateException("Stub registration failed: HTTP " + r.statusCode()
                    + " " + r.asString());
        }
    }
}
