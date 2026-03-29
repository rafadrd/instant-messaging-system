package pt.isel.host.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.test.context.ContextConfiguration;
import pt.isel.domain.builders.MessageBuilder;
import pt.isel.domain.builders.UserInfoBuilder;
import pt.isel.domain.messages.Message;
import pt.isel.domain.messages.UpdatedMessage;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
@ContextConfiguration(classes = {JacksonConfig.class})
class JacksonConfigTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void SerializeDeserialize_NewMessage_MaintainsType() throws Exception {
        Message message = new MessageBuilder()
                .withId(100L)
                .withContent("Hello World")
                .withUser(new UserInfoBuilder().withId(1L).withUsername("alice").build())
                .withCreatedAt(LocalDateTime.of(2025, 1, 1, 12, 0))
                .build();
        UpdatedMessage.NewMessage newMessage = new UpdatedMessage.NewMessage(message);

        String json = objectMapper.writeValueAsString(newMessage);
        UpdatedMessage deserialized = objectMapper.readValue(json, UpdatedMessage.class);

        assertThat(json).isNotNull().contains("\"type\":\"new-message\"");
        assertThat(deserialized).isInstanceOf(UpdatedMessage.NewMessage.class);
        UpdatedMessage.NewMessage deserializedNewMessage = (UpdatedMessage.NewMessage) deserialized;
        assertThat(deserializedNewMessage.message().id()).isEqualTo(100L);
        assertThat(deserializedNewMessage.message().content()).isEqualTo("Hello World");
        assertThat(deserializedNewMessage.message().user().id()).isEqualTo(1L);
    }

    @Test
    void SerializeDeserialize_KeepAlive_MaintainsType() throws Exception {
        Instant now = Instant.now();
        UpdatedMessage.KeepAlive keepAlive = new UpdatedMessage.KeepAlive(now);

        String json = objectMapper.writeValueAsString(keepAlive);
        UpdatedMessage deserialized = objectMapper.readValue(json, UpdatedMessage.class);

        assertThat(json).isNotNull().contains("\"type\":\"keep-alive\"");
        assertThat(deserialized).isInstanceOf(UpdatedMessage.KeepAlive.class);
        UpdatedMessage.KeepAlive deserializedKeepAlive = (UpdatedMessage.KeepAlive) deserialized;
        assertThat(deserializedKeepAlive.timestamp()).isEqualTo(now);
    }
}