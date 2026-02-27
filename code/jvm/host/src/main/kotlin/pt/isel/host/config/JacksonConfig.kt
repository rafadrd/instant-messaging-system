package pt.isel.host.config

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import pt.isel.domain.messages.KeepAlive
import pt.isel.domain.messages.NewMessage
import pt.isel.domain.messages.UpdatedMessage

@Configuration
class JacksonConfig {
    @Bean
    fun jacksonCustomizer(): Jackson2ObjectMapperBuilderCustomizer =
        Jackson2ObjectMapperBuilderCustomizer { builder ->
            builder.modulesToInstall(KotlinModule.Builder().build())
            builder.mixIn(UpdatedMessage::class.java, UpdatedMessageMixin::class.java)
        }

    @JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type",
    )
    @JsonSubTypes(
        JsonSubTypes.Type(value = NewMessage::class, name = "new-message"),
        JsonSubTypes.Type(value = KeepAlive::class, name = "keep-alive"),
    )
    abstract class UpdatedMessageMixin
}
