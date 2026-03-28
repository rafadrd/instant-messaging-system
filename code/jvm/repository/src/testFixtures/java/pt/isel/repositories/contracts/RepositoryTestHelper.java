package pt.isel.repositories.contracts;

import pt.isel.domain.channels.AccessType;
import pt.isel.domain.channels.Channel;
import pt.isel.domain.channels.ChannelMember;
import pt.isel.domain.invitations.Invitation;
import pt.isel.domain.security.PasswordValidationInfo;
import pt.isel.domain.users.User;
import pt.isel.domain.users.UserInfo;
import pt.isel.repositories.Transaction;
import pt.isel.repositories.TransactionManager;

import java.time.LocalDateTime;

public interface RepositoryTestHelper {
    TransactionManager getTxManager();

    default User insertUser(Transaction trx, String username) {
        return trx.repoUsers().create(username, new PasswordValidationInfo("hash"));
    }

    default Channel insertChannel(Transaction trx, String name, User owner, boolean isPublic) {
        return trx.repoChannels().create(name, toUserInfo(owner), isPublic);
    }

    default ChannelMember insertMember(Transaction trx, User user, Channel channel, AccessType accessType) {
        return trx.repoMemberships().addUserToChannel(toUserInfo(user), channel, accessType);
    }

    default Invitation insertInvitation(Transaction trx, String token, User creator, Channel channel, AccessType accessType, LocalDateTime expiresAt) {
        return trx.repoInvitations().create(token, toUserInfo(creator), channel, accessType, expiresAt);
    }

    default UserInfo toUserInfo(User user) {
        return new UserInfo(user.id(), user.username());
    }
}