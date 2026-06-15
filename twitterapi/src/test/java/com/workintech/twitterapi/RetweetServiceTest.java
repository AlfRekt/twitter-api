package com.workintech.twitterapi;

import com.workintech.twitterapi.dto.retweet.CreateRetweetRequest;
import com.workintech.twitterapi.dto.retweet.RetweetResponse;
import com.workintech.twitterapi.entity.Retweet;
import com.workintech.twitterapi.entity.Tweet;
import com.workintech.twitterapi.entity.User;
import com.workintech.twitterapi.exception.BadRequestException;
import com.workintech.twitterapi.exception.ResourceNotFoundException;
import com.workintech.twitterapi.exception.UnauthorizedActionException;
import com.workintech.twitterapi.repository.IRetweetRepository;
import com.workintech.twitterapi.repository.ITweetRepository;
import com.workintech.twitterapi.repository.IUserRepository;
import com.workintech.twitterapi.service.CurrentUserService;
import com.workintech.twitterapi.service.RetweetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RetweetServiceTest {

    @Mock IRetweetRepository retweetRepository;
    @Mock IUserRepository userRepository;
    @Mock ITweetRepository tweetRepository;
    @Mock
    CurrentUserService currentUserService;

    @InjectMocks
    RetweetService retweetService;

    private User user;
    private User other;
    private Tweet tweet;
    private Retweet retweet;

    @BeforeEach
    void setUp() {
        user  = new User(); user.setId(1L);
        other = new User(); other.setId(2L);
        tweet = new Tweet(); tweet.setId(10L);
        retweet = new Retweet(); retweet.setId(100L); retweet.setUser(user); retweet.setTweet(tweet);
    }

    @Test
    void retweet_succeeds_whenNotAlreadyRetweeted() {
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(tweetRepository.findById(10L)).thenReturn(Optional.of(tweet));
        when(retweetRepository.existsByUserIdAndTweetId(1L, 10L)).thenReturn(false);
        when(retweetRepository.save(any(Retweet.class))).thenAnswer(inv -> {
            Retweet r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });

        RetweetResponse response = retweetService.retweet(new CreateRetweetRequest(10L));

        assertEquals(1L, response.retweetedUserId());
        assertEquals(10L, response.tweetId());
        verify(retweetRepository).save(any(Retweet.class));
    }

    @Test
    void retweet_throws_whenAlreadyRetweeted() {
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(tweetRepository.findById(10L)).thenReturn(Optional.of(tweet));
        when(retweetRepository.existsByUserIdAndTweetId(1L, 10L)).thenReturn(true);

        assertThrows(BadRequestException.class,
                () -> retweetService.retweet(new CreateRetweetRequest(10L)));
        verify(retweetRepository, never()).save(any());
    }

    @Test
    void retweet_throws_whenTweetNotFound() {
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(tweetRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> retweetService.retweet(new CreateRetweetRequest(10L)));
    }

    @Test
    void delete_succeeds_whenOwner() {
        when(retweetRepository.findById(100L)).thenReturn(Optional.of(retweet));
        when(currentUserService.getCurrentUser()).thenReturn(user);

        retweetService.delete(100L);

        verify(retweetRepository).delete(retweet);
    }

    @Test
    void delete_throws_whenNotOwner() {
        when(retweetRepository.findById(100L)).thenReturn(Optional.of(retweet));
        when(currentUserService.getCurrentUser()).thenReturn(other);

        assertThrows(UnauthorizedActionException.class, () -> retweetService.delete(100L));
        verify(retweetRepository, never()).delete(any());
    }

    @Test
    void delete_throws_whenRetweetNotFound() {
        when(retweetRepository.findById(100L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> retweetService.delete(100L));
    }
}