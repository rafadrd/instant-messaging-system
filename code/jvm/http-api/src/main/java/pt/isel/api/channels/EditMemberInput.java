package pt.isel.api.channels;

import jakarta.validation.constraints.NotNull;
import pt.isel.domain.channels.AccessType;

public record EditMemberInput(@NotNull AccessType accessType) {
}