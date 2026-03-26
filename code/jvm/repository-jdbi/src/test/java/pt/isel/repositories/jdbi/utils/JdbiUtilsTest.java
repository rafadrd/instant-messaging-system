package pt.isel.repositories.jdbi.utils;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JdbiUtilsTest {

    @Test
    void testParamsWithEvenArguments() {
        Map<String, Object> result = JdbiUtils.params("key1", "value1", "key2", 42);

        assertEquals(2, result.size());
        assertEquals("value1", result.get("key1"));
        assertEquals(42, result.get("key2"));
    }

    @Test
    void testParamsThrowsIllegalArgumentExceptionOnOddArguments() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> JdbiUtils.params("key1", "value1", "key2")
        );

        assertEquals("Key-value array must have even length", exception.getMessage());
    }
}