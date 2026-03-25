package pt.isel.repositories.mem;

import pt.isel.domain.channels.Channel;
import pt.isel.domain.messages.Message;
import pt.isel.domain.users.UserInfo;
import pt.isel.repositories.messages.MessageRepository;

import java.util.ArrayList;
import java.util.List;

public class MessageRepositoryInMem implements MessageRepository {
    private final List<Message> messages = new ArrayList<>();
    private long nextId = 1;

    @Override
    public Message create(String content, UserInfo user, Channel channel) {
        Message msg = new Message(nextId++, content, user, channel);
        messages.add(msg);
        return msg;
    }

    @Override
    public List<Message> findAllInChannel(Channel channel, int limit, int offset) {
        return messages.stream()
                .filter(m -> m.channel().id().equals(channel.id()))
                .skip(offset)
                .limit(limit)
                .toList();
    }

    @Override
    public Message findById(Long id) {
        return messages.stream().filter(m -> m.id().equals(id)).findFirst().orElse(null);
    }

    @Override
    public List<Message> findAll() {
        return new ArrayList<>(messages);
    }

    @Override
    public void save(Message entity) {
        messages.removeIf(m -> m.id().equals(entity.id()));
        messages.add(entity);
    }

    @Override
    public void deleteById(Long id) {
        messages.removeIf(m -> m.id().equals(id));
    }

    @Override
    public void clear() {
        messages.clear();
        nextId = 1;
    }
}