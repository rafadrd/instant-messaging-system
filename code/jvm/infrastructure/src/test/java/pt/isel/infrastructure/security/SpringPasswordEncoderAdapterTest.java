package pt.isel.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

        assertEquals("encodedPassword", result);
        verify(springEncoder).encode("myPassword");
    }

    @Test
    void testMatchesDelegatesToSpringEncoder() {
        PasswordEncoder springEncoder = mock(PasswordEncoder.class);
        when(springEncoder.matches("myPassword", "encodedPassword")).thenReturn(true);

        SpringPasswordEncoderAdapter adapter = new SpringPasswordEncoderAdapter(springEncoder);
        boolean result = adapter.matches("myPassword", "encodedPassword");

        assertTrue(result);
        verify(springEncoder).matches("myPassword", "encodedPassword");
    }

    @Test
    void testIntegrationWithRealBCrypt() {
        SpringPasswordEncoderAdapter adapter = new SpringPasswordEncoderAdapter(new BCryptPasswordEncoder());

        String rawPassword = "SecurePassword123!";
        String encoded = adapter.encode(rawPassword);

        assertTrue(adapter.matches(rawPassword, encoded));
        assertFalse(adapter.matches("WrongPassword!", encoded));
    }
}