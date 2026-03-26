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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

        assertNotNull(json);
        assertTrue(json.contains("\"type\":\"new-message\""), "JSON should contain the polymorphic type identifier");

        UpdatedMessage deserialized = objectMapper.readValue(json, UpdatedMessage.class);
        assertInstanceOf(UpdatedMessage.NewMessage.class, deserialized);

        UpdatedMessage.NewMessage deserializedNewMessage = (UpdatedMessage.NewMessage) deserialized;
        assertEquals(100L, deserializedNewMessage.message().id());
        assertEquals("Hello World", deserializedNewMessage.message().content());
        assertEquals(1L, deserializedNewMessage.message().user().id());
    }

    @Test
    void testSerializeAndDeserializeKeepAlive() throws Exception {
        Instant now = Instant.now();
        UpdatedMessage.KeepAlive keepAlive = new UpdatedMessage.KeepAlive(now);

        String json = objectMapper.writeValueAsString(keepAlive);

        assertNotNull(json);
        assertTrue(json.contains("\"type\":\"keep-alive\""), "JSON should contain the polymorphic type identifier");

        UpdatedMessage deserialized = objectMapper.readValue(json, UpdatedMessage.class);
        assertInstanceOf(UpdatedMessage.KeepAlive.class, deserialized);

        UpdatedMessage.KeepAlive deserializedKeepAlive = (UpdatedMessage.KeepAlive) deserialized;
        assertEquals(now, deserializedKeepAlive.timestamp());
    }
}