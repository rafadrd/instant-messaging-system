package pt.isel.domain.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        assertEquals("encoded_mySecret", info.validationInfo());
    }

    @Test
    void testValidatePasswordSuccess() {
        PasswordValidationInfo info = new PasswordValidationInfo("encoded_mySecret");
        assertTrue(securityDomain.validatePassword("mySecret", info));
    }

    @Test
    void testValidatePasswordFailure() {
        PasswordValidationInfo info = new PasswordValidationInfo("encoded_mySecret");
        assertFalse(securityDomain.validatePassword("wrongSecret", info));
    }

    @Test
    void testIsSafePassword_Valid() {
        assertTrue(securityDomain.isSafePassword("Strong1!"));
        assertTrue(securityDomain.isSafePassword("Another_P@ssw0rd"));
    }

    @Test
    void testIsSafePassword_TooShort() {
        assertFalse(securityDomain.isSafePassword("Stro1!a"));
    }

    @Test
    void testIsSafePassword_MissingUppercase() {
        assertFalse(securityDomain.isSafePassword("weakpass1!"));
    }

    @Test
    void testIsSafePassword_MissingLowercase() {
        assertFalse(securityDomain.isSafePassword("WEAKPASS1!"));
    }

    @Test
    void testIsSafePassword_MissingDigit() {
        assertFalse(securityDomain.isSafePassword("StrongPass!"));
    }

    @Test
    void testIsSafePassword_MissingSpecialChar() {
        assertFalse(securityDomain.isSafePassword("StrongPass123"));
    }

    @Test
    void testIsSafePassword_WithRelaxedPolicy() {
        PasswordPolicyConfig relaxedConfig = new PasswordPolicyConfig(4, false, false, false, false);
        PasswordSecurityDomain relaxedDomain = new PasswordSecurityDomain(fakeEncoder, relaxedConfig);

        assertTrue(relaxedDomain.isSafePassword("pass")); // length 4, all lowercase
        assertTrue(relaxedDomain.isSafePassword("1234")); // all digits
        assertFalse(relaxedDomain.isSafePassword("abc")); // length 3, too short
    }
}