package pt.isel.host.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.test.context.ContextConfiguration;
import pt.isel.domain.channels.Channel;
import pt.isel.domain.messages.Message;
import pt.isel.domain.messages.UpdatedMessage;
import pt.isel.domain.users.UserInfo;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
@ContextConfiguration(classes = {JacksonConfig.class})
class JacksonConfigTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testSerializeAndDeserializeNewMessage() throws Exception {
        UserInfo user = new UserInfo(1L, "alice");
        Channel channel = new Channel(10L, "General", user, true);
        Message message = new Message(100L, "Hello World", user, channel, LocalDateTime.of(2025, 1, 1, 12, 0));
        UpdatedMessage.NewMessage newMessage = new UpdatedMessage.NewMessage(message);

        String json = objectMapper.writeValueAsString(newMessage);

        assertThat(json).isNotNull()
                .as("JSON should contain the polymorphic type identifier")
                .contains("\"type\":\"new-message\"");

        UpdatedMessage deserialized = objectMapper.readValue(json, UpdatedMessage.class);
        assertThat(deserialized).isInstanceOf(UpdatedMessage.NewMessage.class);

        UpdatedMessage.NewMessage deserializedNewMessage = (UpdatedMessage.NewMessage) deserialized;
        assertThat(deserializedNewMessage.message().id()).isEqualTo(100L);
        assertThat(deserializedNewMessage.message().content()).isEqualTo("Hello World");
        assertThat(deserializedNewMessage.message().user().id()).isEqualTo(1L);
    }

    @Test
    void testSerializeAndDeserializeKeepAlive() throws Exception {
        Instant now = Instant.now();
        UpdatedMessage.KeepAlive keepAlive = new UpdatedMessage.KeepAlive(now);

        String json = objectMapper.writeValueAsString(keepAlive);

        assertThat(json).isNotNull()
                .as("JSON should contain the polymorphic type identifier")
                .contains("\"type\":\"keep-alive\"");

        UpdatedMessage deserialized = objectMapper.readValue(json, UpdatedMessage.class);
        assertThat(deserialized).isInstanceOf(UpdatedMessage.KeepAlive.class);

        UpdatedMessage.KeepAlive deserializedKeepAlive = (UpdatedMessage.KeepAlive) deserialized;
        assertThat(deserializedKeepAlive.timestamp()).isEqualTo(now);
    }
}