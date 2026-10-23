package bd.cityv1.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.mock.env.MockEnvironment;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseConfigurationTest {

    @Test
    void missingDatabaseVariablesCannotFallBackToAnotherProject() throws IOException {
        Map<String, String> required = Map.of(
                "DB_URL", "spring.datasource.url",
                "DB_USERNAME", "spring.datasource.username",
                "DB_PASSWORD", "spring.datasource.password");
        for (var variable : required.entrySet()) {
            Map<String, Object> configured = new java.util.HashMap<>(Map.of(
                    "DB_URL", "jdbc:postgresql://localhost:5432/testdb",
                    "DB_USERNAME", "test-user",
                    "DB_PASSWORD", "test-password"));
            configured.remove(variable.getKey());
            MockEnvironment environment = environment(configured);
            assertThrows(IllegalArgumentException.class,
                    () -> environment.getProperty(variable.getValue()), variable.getKey() + " must be required");
        }
    }

    @Test
    void databaseConnectionValuesComeFromTheEnvironment() throws IOException {
        MockEnvironment environment = environment(Map.of(
                "DB_URL", "jdbc:postgresql://localhost:5432/testdb",
                "DB_USERNAME", "test-user",
                "DB_PASSWORD", "test-password"));

        assertEquals("jdbc:postgresql://localhost:5432/testdb",
                environment.getProperty("spring.datasource.url"));
        assertEquals("test-user", environment.getProperty("spring.datasource.username"));
        assertEquals("test-password", environment.getProperty("spring.datasource.password"));
    }

    private MockEnvironment environment(Map<String, Object> variables) throws IOException {
        MockEnvironment environment = new MockEnvironment();
        environment.getPropertySources().addFirst(new SystemEnvironmentPropertySource("test-env", variables));
        environment.getPropertySources().addLast(new PropertiesPropertySource("application",
                PropertiesLoaderUtils.loadProperties(new ClassPathResource("application.properties"))));
        return environment;
    }
}
