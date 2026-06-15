package com.workintech.twitterapi;

import com.workintech.twitterapi.dto.user.UserResponse;
import com.workintech.twitterapi.entity.Role;
import com.workintech.twitterapi.entity.User;
import com.workintech.twitterapi.exception.BadRequestException;
import com.workintech.twitterapi.repository.IRoleRepository;
import com.workintech.twitterapi.repository.IUserRepository;
import com.workintech.twitterapi.service.AuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock IUserRepository userRepository;
    @Mock IRoleRepository roleRepository;
    @Mock PasswordEncoder passwordEncoder;

    @InjectMocks
    AuthenticationService authenticationService;

    @Captor ArgumentCaptor<User> userCaptor;

    private Role userRole;

    @BeforeEach
    void setUp() {
        userRole = new Role(); userRole.setId(1L); userRole.setAuthority("USER");
    }

    @Test
    void register_succeeds_andStoresEncodedPassword() {
        when(userRepository.findUserByUsername("fatih")).thenReturn(Optional.empty());
        when(userRepository.existsByEmail("fatih@mail.com")).thenReturn(false);
        when(passwordEncoder.encode("secret")).thenReturn("ENCODED");
        when(roleRepository.findByAuthority("USER")).thenReturn(Optional.of(userRole));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        UserResponse response =
                authenticationService.register("fatih", "fatih@mail.com", "secret");

        assertEquals(1L, response.id());
        assertEquals("fatih", response.userName());
        assertEquals("fatih@mail.com", response.email());

        verify(passwordEncoder).encode("secret");
        verify(userRepository).save(userCaptor.capture());
        assertEquals("ENCODED", userCaptor.getValue().getPassword());  // raw password never persisted
        assertTrue(userCaptor.getValue().getAuthorities().contains(userRole));
    }

    @Test
    void register_throws_whenUsernameTaken() {
        when(userRepository.findUserByUsername("fatih")).thenReturn(Optional.of(new User()));

        assertThrows(BadRequestException.class,
                () -> authenticationService.register("fatih", "fatih@mail.com", "secret"));
        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void register_throws_whenEmailTaken() {
        when(userRepository.findUserByUsername("fatih")).thenReturn(Optional.empty());
        when(userRepository.existsByEmail("fatih@mail.com")).thenReturn(true);

        assertThrows(BadRequestException.class,
                () -> authenticationService.register("fatih", "fatih@mail.com", "secret"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_throws_whenInputBlank() {
        assertThrows(BadRequestException.class,
                () -> authenticationService.register("fatih", "fatih@mail.com", "  "));
        verifyNoInteractions(passwordEncoder);
        verify(userRepository, never()).save(any());
    }
}