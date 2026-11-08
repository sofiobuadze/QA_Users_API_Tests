package ge.qa.client;

import ge.qa.config.Config;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.Map;

public class UsersClient {

    public Response getUsers(Map<String, String> queryParams, Map<String, String> headers) {
        RequestSpecification spec = RestAssured.given()
                .baseUri(Config.baseUrl())
                .accept("application/json");
        if (queryParams != null) {
            spec.queryParams(queryParams);
        }
        if (headers != null) {
            spec.headers(headers);
        }
        return spec.when().get("/users");
    }

    public Response getUsers(Map<String, String> queryParams) {
        return getUsers(queryParams, Map.of());
    }
}
