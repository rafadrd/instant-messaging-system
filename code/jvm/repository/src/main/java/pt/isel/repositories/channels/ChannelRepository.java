package pt.isel.repositories.channels;

import pt.isel.domain.channels.Channel;
import pt.isel.domain.users.UserInfo;
import pt.isel.repositories.Repository;

import java.util.List;

public interface ChannelRepository extends Repository<Channel> {
    Channel create(String name, UserInfo owner, boolean isPublic);

    Channel findByName(String name);

    List<Channel> findAllByOwner(Long ownerId);

    List<Channel> findAllPublicChannels(int limit, int offset);

    List<Channel> searchByName(String query, int limit, int offset);
}