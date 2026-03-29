package pt.isel.domain.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.isel.domain.builders.PasswordValidationInfoBuilder;
import pt.isel.domain.fakes.FakePasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordSecurityDomainTest {

    private FakePasswordEncoder fakeEncoder;
    private PasswordSecurityDomain securityDomain;

    @BeforeEach
    void setUp() {
        fakeEncoder = new FakePasswordEncoder();
        PasswordPolicyConfig config = new PasswordPolicyConfig(8, true, true, true, true);
        securityDomain = new PasswordSecurityDomain(fakeEncoder, config);
    }

    @Test
    void CreatePasswordValidationInformation_ValidPassword_ReturnsInfo() {
        PasswordValidationInfo info = securityDomain.createPasswordValidationInformation("mySecret");

        assertThat(info.validationInfo()).isEqualTo("encoded_mySecret");
    }

    @Test
    void ValidatePassword_CorrectPassword_ReturnsTrue() {
        PasswordValidationInfo info = new PasswordValidationInfoBuilder().withValidationInfo("encoded_mySecret").build();

        boolean result = securityDomain.validatePassword("mySecret", info);

        assertThat(result).isTrue();
    }

    @Test
    void ValidatePassword_IncorrectPassword_ReturnsFalse() {
        PasswordValidationInfo info = new PasswordValidationInfoBuilder().withValidationInfo("encoded_mySecret").build();

        boolean result = securityDomain.validatePassword("wrongSecret", info);

        assertThat(result).isFalse();
    }

    @Test
    void IsSafePassword_ValidPassword_ReturnsTrue() {
        boolean result1 = securityDomain.isSafePassword("Strong1!");
        boolean result2 = securityDomain.isSafePassword("Another_P@ssw0rd");

        assertThat(result1).isTrue();
        assertThat(result2).isTrue();
    }

    @Test
    void IsSafePassword_TooShort_ReturnsFalse() {
        boolean result = securityDomain.isSafePassword("Stro1!a");

        assertThat(result).isFalse();
    }

    @Test
    void IsSafePassword_MissingUppercase_ReturnsFalse() {
        boolean result = securityDomain.isSafePassword("weakpass1!");

        assertThat(result).isFalse();
    }

    @Test
    void IsSafePassword_MissingLowercase_ReturnsFalse() {
        boolean result = securityDomain.isSafePassword("WEAKPASS1!");

        assertThat(result).isFalse();
    }

    @Test
    void IsSafePassword_MissingDigit_ReturnsFalse() {
        boolean result = securityDomain.isSafePassword("StrongPass!");

        assertThat(result).isFalse();
    }

    @Test
    void IsSafePassword_MissingSpecialChar_ReturnsFalse() {
        boolean result = securityDomain.isSafePassword("StrongPass123");

        assertThat(result).isFalse();
    }

    @Test
    void IsSafePassword_RelaxedPolicy_ReturnsTrue() {
        PasswordPolicyConfig relaxedConfig = new PasswordPolicyConfig(4, false, false, false, false);
        PasswordSecurityDomain relaxedDomain = new PasswordSecurityDomain(fakeEncoder, relaxedConfig);

        boolean result1 = relaxedDomain.isSafePassword("pass");
        boolean result2 = relaxedDomain.isSafePassword("1234");
        boolean result3 = relaxedDomain.isSafePassword("abc");

        assertThat(result1).isTrue();
        assertThat(result2).isTrue();
        assertThat(result3).isFalse();
    }
}