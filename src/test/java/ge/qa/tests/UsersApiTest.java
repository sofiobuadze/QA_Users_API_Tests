package ge.qa.tests;

import com.fasterxml.jackson.databind.ObjectMapper;
import ge.qa.client.UsersClient;
import ge.qa.mock.WireMockStubs;
import ge.qa.model.User;
import io.restassured.response.Response;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static ge.qa.mock.WireMockStubs.ALICE;
import static ge.qa.mock.WireMockStubs.BOB;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class UsersApiTest extends BaseTest {

    private final UsersClient client = new UsersClient();
    private final ObjectMapper mapper = new ObjectMapper();

    //GET all

    @DataProvider(name = "allUsers")
    public Object[][] allUsers() {
        return new Object[][]{
                // description, query params, expected status, expected users
                {"all users without filters", Map.of(), 200, List.of(ALICE, BOB)}
        };
    }

    @Test(dataProvider = "allUsers")
    public void testGetAllUsers_Positive(String description, Map<String, String> params,
                                         int expectedStatus, List<User> expected) {
        Response response = client.getUsers(params);

        assertEquals(response.statusCode(), expectedStatus, description);
        assertTrue(response.contentType().contains("application/json"), "Content-Type must be JSON");

        // Deserialization: JSON -> User[]
        List<User> actual = List.of(response.as(User[].class));
        assertEquals(actual, expected, description);

        // Serialization round-trip: User -> JSON -> User
        for (User u : actual) {
            assertEquals(roundTrip(u), u, "Serialization round-trip failed for " + u);
        }
    }

    //filters

    @DataProvider(name = "ageFilter")
    public Object[][] ageFilter() {
        return new Object[][]{
                {"age=30", Map.of("age", "30"), 200, List.of(ALICE)},
                {"age=25", Map.of("age", "25"), 200, List.of(BOB)}
        };
    }

    @Test(dataProvider = "ageFilter")
    public void testFilterByAge_Positive(String description, Map<String, String> params,
                                         int expectedStatus, List<User> expected) {
        Response response = client.getUsers(params);

        assertEquals(response.statusCode(), expectedStatus, description);
        List<User> actual = List.of(response.as(User[].class));
        assertEquals(actual, expected, description);
        actual.forEach(u -> assertEquals(String.valueOf(u.getAge()), params.get("age")));
    }

    @DataProvider(name = "genderFilter")
    public Object[][] genderFilter() {
        return new Object[][]{
                {"gender=female", Map.of("gender", "female"), 200, List.of(ALICE)},
                {"gender=male", Map.of("gender", "male"), 200, List.of(BOB)}
        };
    }

    @Test(dataProvider = "genderFilter")
    public void testFilterByGender_Positive(String description, Map<String, String> params,
                                            int expectedStatus, List<User> expected) {
        Response response = client.getUsers(params);

        assertEquals(response.statusCode(), expectedStatus, description);
        List<User> actual = List.of(response.as(User[].class));
        assertEquals(actual, expected, description);
        actual.forEach(u -> assertEquals(u.getGender(), params.get("gender")));
    }

    // negative

    @DataProvider(name = "invalidAge")
    public Object[][] invalidAge() {
        return new Object[][]{
                {"age=-1", Map.of("age", "-1"), 400}
        };
    }

    @Test(dataProvider = "invalidAge")
    public void testInvalidAge_Negative(String description, Map<String, String> params, int expectedStatus) {
        Response response = client.getUsers(params);

        assertEquals(response.statusCode(), expectedStatus, description);
    }

    @DataProvider(name = "serverError")
    public Object[][] serverError() {
        return new Object[][]{
                {"GET /users with error header", Map.of(), Map.of(WireMockStubs.ERROR_HEADER, "true"), 500}
        };
    }

    @Test(dataProvider = "serverError")
    public void testInternalServerError_Negative(String description, Map<String, String> params,
                                                 Map<String, String> headers, int expectedStatus) {
        Response response = client.getUsers(params, headers);

        assertEquals(response.statusCode(), expectedStatus, description);
    }

    @DataProvider(name = "invalidGender")
    public Object[][] invalidGender() {
        return new Object[][]{
                // The spec allows an empty list OR 422
                {"gender=unknown", Map.of("gender", "unknown"), List.of(200, 422)}
        };
    }

    @Test(dataProvider = "invalidGender")
    public void testInvalidGender_Negative(String description, Map<String, String> params,
                                           List<Integer> allowedStatuses) {
        Response response = client.getUsers(params);

        assertTrue(allowedStatuses.contains(response.statusCode()),
                description + ": unexpected status " + response.statusCode());
        if (response.statusCode() == 200) {
            assertEquals(response.as(User[].class).length, 0, "Expected an empty list");
        }
    }

    //  helpers

    private User roundTrip(User user) {
        try {
            return mapper.readValue(mapper.writeValueAsString(user), User.class);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
