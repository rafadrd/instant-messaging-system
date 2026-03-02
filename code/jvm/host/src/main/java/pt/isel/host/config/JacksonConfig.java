package pt.isel.host.config;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pt.isel.domain.messages.UpdatedMessage;
import pt.isel.domain.messages.UpdatedMessage.KeepAlive;
import pt.isel.domain.messages.UpdatedMessage.NewMessage;

@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> {
            builder.mixIn(UpdatedMessage.class, UpdatedMessageMixin.class);
        };
    }

    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.PROPERTY,
            property = "type"
    )
    @JsonSubTypes({
            @JsonSubTypes.Type(value = NewMessage.class, name = "new-message"),
            @JsonSubTypes.Type(value = KeepAlive.class, name = "keep-alive")
    })
    public abstract static class UpdatedMessageMixin {
    }
}