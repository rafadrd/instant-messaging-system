package pt.isel.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SpringPasswordEncoderAdapterTest {

    @Test
    void testEncodeDelegatesToSpringEncoder() {
        PasswordEncoder springEncoder = mock(PasswordEncoder.class);
        when(springEncoder.encode("myPassword")).thenReturn("encodedPassword");

        SpringPasswordEncoderAdapter adapter = new SpringPasswordEncoderAdapter(springEncoder);
        String result = adapter.encode("myPassword");

        assertThat(result).isEqualTo("encodedPassword");
        verify(springEncoder).encode("myPassword");
    }

    @Test
    void testMatchesDelegatesToSpringEncoder() {
        PasswordEncoder springEncoder = mock(PasswordEncoder.class);
        when(springEncoder.matches("myPassword", "encodedPassword")).thenReturn(true);

        SpringPasswordEncoderAdapter adapter = new SpringPasswordEncoderAdapter(springEncoder);
        boolean result = adapter.matches("myPassword", "encodedPassword");

        assertThat(result).isTrue();
        verify(springEncoder).matches("myPassword", "encodedPassword");
    }

    @Test
    void testIntegrationWithRealBCrypt() {
        SpringPasswordEncoderAdapter adapter = new SpringPasswordEncoderAdapter(new BCryptPasswordEncoder());

        String rawPassword = "SecurePassword123!";
        String encoded = adapter.encode(rawPassword);

        assertThat(adapter.matches(rawPassword, encoded)).isTrue();
        assertThat(adapter.matches("WrongPassword!", encoded)).isFalse();
    }
}