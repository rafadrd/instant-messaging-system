package pt.isel.domain.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordSecurityDomainTest {

    private PasswordSecurityDomain securityDomain;
    private PasswordEncoder fakeEncoder;

    @BeforeEach
    void setUp() {
        fakeEncoder = new PasswordEncoder() {
            @Override
            public String encode(String rawPassword) {
                return "encoded_" + rawPassword;
            }

            @Override
            public boolean matches(String rawPassword, String encodedPassword) {
                return encodedPassword.equals("encoded_" + rawPassword);
            }
        };

        PasswordPolicyConfig config = new PasswordPolicyConfig(8, true, true, true, true);
        securityDomain = new PasswordSecurityDomain(fakeEncoder, config);
    }

    @Test
    void testCreatePasswordValidationInformation() {
        PasswordValidationInfo info = securityDomain.createPasswordValidationInformation("mySecret");
        assertThat(info.validationInfo()).isEqualTo("encoded_mySecret");
    }

    @Test
    void testValidatePasswordSuccess() {
        PasswordValidationInfo info = new PasswordValidationInfo("encoded_mySecret");
        assertThat(securityDomain.validatePassword("mySecret", info)).isTrue();
    }

    @Test
    void testValidatePasswordFailure() {
        PasswordValidationInfo info = new PasswordValidationInfo("encoded_mySecret");
        assertThat(securityDomain.validatePassword("wrongSecret", info)).isFalse();
    }

    @Test
    void testIsSafePassword_Valid() {
        assertThat(securityDomain.isSafePassword("Strong1!")).isTrue();
        assertThat(securityDomain.isSafePassword("Another_P@ssw0rd")).isTrue();
    }

    @Test
    void testIsSafePassword_TooShort() {
        assertThat(securityDomain.isSafePassword("Stro1!a")).isFalse();
    }

    @Test
    void testIsSafePassword_MissingUppercase() {
        assertThat(securityDomain.isSafePassword("weakpass1!")).isFalse();
    }

    @Test
    void testIsSafePassword_MissingLowercase() {
        assertThat(securityDomain.isSafePassword("WEAKPASS1!")).isFalse();
    }

    @Test
    void testIsSafePassword_MissingDigit() {
        assertThat(securityDomain.isSafePassword("StrongPass!")).isFalse();
    }

    @Test
    void testIsSafePassword_MissingSpecialChar() {
        assertThat(securityDomain.isSafePassword("StrongPass123")).isFalse();
    }

    @Test
    void testIsSafePassword_WithRelaxedPolicy() {
        PasswordPolicyConfig relaxedConfig = new PasswordPolicyConfig(4, false, false, false, false);
        PasswordSecurityDomain relaxedDomain = new PasswordSecurityDomain(fakeEncoder, relaxedConfig);

        assertThat(relaxedDomain.isSafePassword("pass")).isTrue();
        assertThat(relaxedDomain.isSafePassword("1234")).isTrue();
        assertThat(relaxedDomain.isSafePassword("abc")).isFalse();
    }
}