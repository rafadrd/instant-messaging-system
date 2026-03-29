package pt.isel.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpringPasswordEncoderAdapterTest {

    @Mock
    private PasswordEncoder springEncoder;

    @InjectMocks
    private SpringPasswordEncoderAdapter adapter;

    @Test
    void Encode_ValidPassword_DelegatesToSpringEncoder() {
        when(springEncoder.encode("myPassword")).thenReturn("encodedPassword");

        String result = adapter.encode("myPassword");

        assertThat(result).isEqualTo("encodedPassword");
        verify(springEncoder).encode("myPassword");
    }

    @Test
    void Matches_ValidPasswords_DelegatesToSpringEncoder() {
        when(springEncoder.matches("myPassword", "encodedPassword")).thenReturn(true);

        boolean result = adapter.matches("myPassword", "encodedPassword");

        assertThat(result).isTrue();
        verify(springEncoder).matches("myPassword", "encodedPassword");
    }

    @Test
    void Matches_RealBCrypt_ReturnsExpectedResult() {
        SpringPasswordEncoderAdapter realAdapter = new SpringPasswordEncoderAdapter(new BCryptPasswordEncoder());

        String rawPassword = "SecurePassword123!";
        String encoded = realAdapter.encode(rawPassword);

        assertThat(realAdapter.matches(rawPassword, encoded)).isTrue();
        assertThat(realAdapter.matches("WrongPassword!", encoded)).isFalse();
    }
}