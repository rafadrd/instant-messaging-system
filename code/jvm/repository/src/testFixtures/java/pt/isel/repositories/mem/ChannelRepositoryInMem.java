package pt.isel.repositories.mem;

import pt.isel.domain.channels.Channel;
import pt.isel.domain.users.UserInfo;
import pt.isel.repositories.channels.ChannelRepository;

import java.util.ArrayList;
import java.util.List;

public class ChannelRepositoryInMem implements ChannelRepository {
    private final List<Channel> channels = new ArrayList<>();
    private long nextId = 1;

    @Override
    public Channel create(String name, UserInfo owner, boolean isPublic) {
        if (findByName(name) != null) throw new RuntimeException("channels_name_key");
        Channel channel = new Channel(nextId++, name, owner, isPublic);
        channels.add(channel);
        return channel;
    }

    @Override
    public Channel findById(Long id) {
        return channels.stream().filter(c -> c.id().equals(id)).findFirst().orElse(null);
    }

    @Override
    public Channel findByName(String name) {
        return channels.stream().filter(c -> c.name().equals(name)).findFirst().orElse(null);
    }

    @Override
    public List<Channel> findAllByOwner(Long ownerId) {
        return channels.stream().filter(c -> c.owner().id().equals(ownerId)).toList();
    }

    @Override
    public List<Channel> findAllPublicChannels(int limit, int offset) {
        return channels.stream()
                .filter(Channel::isPublic)
                .skip(offset)
                .limit(limit)
                .toList();
    }

    @Override
    public List<Channel> searchByName(String query, int limit, int offset) {
        return channels.stream()
                .filter(c -> c.isPublic() && c.name().toLowerCase().contains(query.toLowerCase()))
                .skip(offset)
                .limit(limit)
                .toList();
    }

    @Override
    public List<Channel> findAll() {
        return new ArrayList<>(channels);
    }

    @Override
    public void save(Channel entity) {
        Channel existing = findByName(entity.name());
        if (existing != null && !existing.id().equals(entity.id())) throw new RuntimeException("channels_name_key");
        channels.removeIf(c -> c.id().equals(entity.id()));
        channels.add(entity);
    }

    @Override
    public void deleteById(Long id) {
        channels.removeIf(c -> c.id().equals(id));
    }

    @Override
    public void clear() {
        channels.clear();
        nextId = 1;
    }
}