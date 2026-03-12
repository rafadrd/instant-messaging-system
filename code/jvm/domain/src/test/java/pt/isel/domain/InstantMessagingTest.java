package pt.isel.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.isel.domain.channels.AccessType;
import pt.isel.domain.channels.Channel;
import pt.isel.domain.channels.ChannelMember;
import pt.isel.domain.common.Either;
import pt.isel.domain.invitations.Invitation;
import pt.isel.domain.invitations.InvitationStatus;
import pt.isel.domain.messages.Message;
import pt.isel.domain.security.PasswordEncoder;
import pt.isel.domain.security.PasswordPolicyConfig;
import pt.isel.domain.security.PasswordSecurityDomain;
import pt.isel.domain.security.PasswordValidationInfo;
import pt.isel.domain.users.AuthenticatedUser;
import pt.isel.domain.users.User;
import pt.isel.domain.users.UserInfo;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InstantMessagingTest {

    private PasswordSecurityDomain passwordSecurityDomain;

    @BeforeEach
    void setup() {
        PasswordEncoder passwordEncoder = new PasswordEncoder() {
            @Override
            public String encode(String rawPassword) {
                return "encoded-" + rawPassword;
            }

            @Override
            public boolean matches(String rawPassword, String encodedPassword) {
                return encodedPassword.equals("encoded-" + rawPassword);
            }
        };

        PasswordPolicyConfig policyConfig = new PasswordPolicyConfig(8, true, true, true, true);
        passwordSecurityDomain = new PasswordSecurityDomain(passwordEncoder, policyConfig);
    }

    @Test
    void testAuthenticatedUserCreation() {
        User user = new User(1L, "user1", new PasswordValidationInfo("encoded-pass"));
        AuthenticatedUser authUser = new AuthenticatedUser(user, "token123");

        assertEquals(user, authUser.user());
        assertEquals("token123", authUser.token());
    }

    @Test
    void testChannelCreation() {
        UserInfo owner = new UserInfo(1L, "owner");
        Channel channel = new Channel(1L, "general", owner);

        assertEquals(1L, channel.id());
        assertEquals("general", channel.name());
        assertEquals(owner, channel.owner());
        assertTrue(channel.isPublic());
    }

    @Test
    void testChannelMemberCreation() {
        UserInfo user = new UserInfo(1L, "member");
        Channel channel = new Channel(1L, "general", user);
        ChannelMember member = new ChannelMember(1L, user, channel, AccessType.READ_WRITE);

        assertEquals(AccessType.READ_WRITE, member.accessType());
        assertEquals(user, member.user());
    }

    @Test
    void testInvitationCreation() {
        UserInfo creator = new UserInfo(1L, "admin");
        Channel channel = new Channel(1L, "private", creator, false);
        LocalDateTime expiry = LocalDateTime.now().plusDays(1);

        Invitation invitation = new Invitation(1L, "inv-token", creator, channel, AccessType.READ_ONLY, expiry);

        assertEquals("inv-token", invitation.token());
        assertEquals(InvitationStatus.PENDING, invitation.status());
        assertEquals(expiry, invitation.expiresAt());
    }

    @Test
    void testMessageCreation() {
        UserInfo sender = new UserInfo(1L, "alice");
        Channel channel = new Channel(1L, "general", sender);
        Message message = new Message(1L, "Hello World", sender, channel);

        assertEquals("Hello World", message.content());
        assertNotNull(message.createdAt());
    }

    @Test
    void testEitherMonad() {
        // Test Success
        Either<String, Integer> success = Either.success(42);
        assertInstanceOf(Either.Right.class, success);
        assertEquals(42, ((Either.Right<String, Integer>) success).value());

        // Test Mapping
        Either<String, String> mapped = success.map(Object::toString);
        assertEquals("42", ((Either.Right<String, String>) mapped).value());

        // Test Failure
        Either<String, Integer> failure = Either.failure("Error");
        assertInstanceOf(Either.Left.class, failure);
        assertEquals("Error", ((Either.Left<String, Integer>) failure).value());
    }

    @Test
    void testPasswordSecurityDomainValidation() {
        PasswordValidationInfo info = passwordSecurityDomain.createPasswordValidationInformation("Password123!");

        assertTrue(passwordSecurityDomain.validatePassword("Password123!", info));
        assertFalse(passwordSecurityDomain.validatePassword("WrongPass", info));
    }

    @Test
    void testIsSafePassword() {
        assertTrue(passwordSecurityDomain.isSafePassword("SafePass123!"));
        assertFalse(passwordSecurityDomain.isSafePassword("S1!a"));
        assertFalse(passwordSecurityDomain.isSafePassword("safepass123!"));
        assertFalse(passwordSecurityDomain.isSafePassword("SAFEPASS123!"));
        assertFalse(passwordSecurityDomain.isSafePassword("SafePassReq!"));
        assertFalse(passwordSecurityDomain.isSafePassword("SafePass123"));
    }

    @Test
    void testUserInfoFromUser() {
        PasswordValidationInfo pwd = new PasswordValidationInfo("secret");
        User user = new User(100L, "bob", pwd);
        UserInfo info = new UserInfo(user.id(), user.username());

        assertEquals(100L, info.id());
        assertEquals("bob", info.username());
    }
}