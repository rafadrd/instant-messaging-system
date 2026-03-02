package pt.isel.infrastructure.security;

import org.springframework.stereotype.Component;
import pt.isel.domain.security.PasswordEncoder;

@Component
public class SpringPasswordEncoderAdapter implements PasswordEncoder {

    private final org.springframework.security.crypto.password.PasswordEncoder springEncoder;

    public SpringPasswordEncoderAdapter(org.springframework.security.crypto.password.PasswordEncoder springEncoder) {
        this.springEncoder = springEncoder;
    }

    @Override
    public String encode(String rawPassword) {
        return springEncoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        return springEncoder.matches(rawPassword, encodedPassword);
    }
}