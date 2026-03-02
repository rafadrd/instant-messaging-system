package pt.isel.api.channels;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChannelInput(@NotBlank @Size(min = 1, max = 30) String name, boolean isPublic) {
}