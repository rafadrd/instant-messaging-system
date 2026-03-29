package pt.isel.repositories.jdbi.utils;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JdbiUtilsTest {

    @Test
    void Params_EvenArguments_ReturnsMap() {
        Map<String, Object> result = JdbiUtils.params("key1", "value1", "key2", 42);

        assertThat(result).hasSize(2);
        assertThat(result.get("key1")).isEqualTo("value1");
        assertThat(result.get("key2")).isEqualTo(42);
    }

    @Test
    void Params_OddArguments_ThrowsException() {
        assertThatThrownBy(() -> JdbiUtils.params("key1", "value1", "key2"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Key-value array must have even length");
    }
}