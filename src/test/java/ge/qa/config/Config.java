package ge.qa.config;

public final class Config {

    private Config() {
    }

    /** WireMock base URL. Override with -Dwiremock.url=http://host:port */
    public static String baseUrl() {
        return valueOrDefault("wiremock.url", "http://localhost:8080");
    }

    /** SQLite file location. Override with -Ddb.path=... */
    public static String dbPath() {
        return valueOrDefault("db.path", "test-results.db");
    }

    private static String valueOrDefault(String key, String def) {
        String v = System.getProperty(key);
        return (v == null || v.isBlank() || v.startsWith("${")) ? def : v;
    }
}
