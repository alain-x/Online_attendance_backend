package com.online.attendance.security;

import com.online.attendance.user.AppUser;
import com.online.attendance.user.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String companySlug;
        String rawUsername;
        int idx = username != null ? username.indexOf("::") : -1;
        if (idx <= 0) {
            throw new UsernameNotFoundException("Invalid principal format");
        }
        companySlug = username.substring(0, idx);
        rawUsername = username.substring(idx + 2);

        AppUser user = userRepository.findByUsernameAndCompanySlug(rawUsername, companySlug)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return new User(
                companySlug + "::" + user.getUsername(),
                user.getPasswordHash(),
                user.isEnabled(),
                true,
                true,
                true,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }
}
