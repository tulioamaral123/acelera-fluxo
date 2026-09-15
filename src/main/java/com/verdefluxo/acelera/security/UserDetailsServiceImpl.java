package com.verdefluxo.acelera.security;

import com.verdefluxo.acelera.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        String password = user.getPassword() != null ? user.getPassword() : "";
        String role = user.getRole() != null ? user.getRole().name() : "USER";
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                password,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
    }
}
