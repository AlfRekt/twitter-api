package com.workintech.twitterapi;

import com.workintech.twitterapi.dto.retweet.CreateQuoteRequest;
import com.workintech.twitterapi.dto.tweet.CreateTweetRequest;
import com.workintech.twitterapi.dto.tweet.TweetResponse;
import com.workintech.twitterapi.dto.tweet.UpdateTweetRequest;
import com.workintech.twitterapi.entity.Tweet;
import com.workintech.twitterapi.entity.User;
import com.workintech.twitterapi.exception.BadRequestException;
import com.workintech.twitterapi.exception.ResourceNotFoundException;
import com.workintech.twitterapi.exception.UnauthorizedActionException;
import com.workintech.twitterapi.repository.ITweetRepository;
import com.workintech.twitterapi.repository.IUserRepository;
import com.workintech.twitterapi.service.CurrentUserService;
import com.workintech.twitterapi.service.TweetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TweetServiceTest {

    @Mock ITweetRepository tweetRepository;
    @Mock IUserRepository userRepository;
    @Mock
    CurrentUserService currentUserService;

    @InjectMocks
    TweetService tweetService;

    private User owner;
    private User other;
    private Tweet tweet;

    @BeforeEach
    void setUp() {
        owner = new User(); owner.setId(1L);
        other = new User(); other.setId(2L);
        tweet = new Tweet(); tweet.setId(10L); tweet.setContent("hello"); tweet.setUser(owner);
    }

    @Test
    void save_createsTweet_withPrincipalAsAuthor() {
        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(tweetRepository.save(any(Tweet.class))).thenAnswer(inv -> {
            Tweet t = inv.getArgument(0);
            t.setId(10L);
            return t;
        });

        TweetResponse response = tweetService.save(new CreateTweetRequest("hello"));

        assertEquals("hello", response.content());
        assertEquals(1L, response.userId());
    }

    @Test
    void save_throws_whenContentBlank() {
        when(currentUserService.getCurrentUser()).thenReturn(owner);

        assertThrows(BadRequestException.class,
                () -> tweetService.save(new CreateTweetRequest("   ")));
        verify(tweetRepository, never()).save(any());
    }

    @Test
    void findById_throws_whenNotFound() {
        when(tweetRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> tweetService.findById(99L));
    }

    @Test
    void findAll_returnsResponsesNewestFirst() {
        when(tweetRepository.findAll(any(Sort.class))).thenReturn(List.of(tweet));

        List<TweetResponse> result = tweetService.findAll();

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).id());
    }

    @Test
    void update_succeeds_whenOwner() {
        when(tweetRepository.findById(10L)).thenReturn(Optional.of(tweet));
        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(tweetRepository.save(any(Tweet.class))).thenAnswer(inv -> inv.getArgument(0));

        TweetResponse response = tweetService.update(10L, new UpdateTweetRequest("edited"));

        assertEquals("edited", response.content());
    }

    @Test
    void update_throws_whenNotOwner() {
        when(tweetRepository.findById(10L)).thenReturn(Optional.of(tweet));
        when(currentUserService.getCurrentUser()).thenReturn(other);

        assertThrows(UnauthorizedActionException.class,
                () -> tweetService.update(10L, new UpdateTweetRequest("edited")));
        verify(tweetRepository, never()).save(any());
    }

    @Test
    void delete_succeeds_whenOwner() {
        when(tweetRepository.findById(10L)).thenReturn(Optional.of(tweet));
        when(currentUserService.getCurrentUser()).thenReturn(owner);

        tweetService.delete(10L);

        verify(tweetRepository).delete(tweet);
    }

    @Test
    void delete_succeeds_whenAdminNotOwner() {
        when(tweetRepository.findById(10L)).thenReturn(Optional.of(tweet));
        when(currentUserService.getCurrentUser()).thenReturn(other);
        when(currentUserService.isAdmin(other)).thenReturn(true);

        tweetService.delete(10L);

        verify(tweetRepository).delete(tweet);
    }

    @Test
    void delete_throws_whenNotOwnerNotAdmin() {
        when(tweetRepository.findById(10L)).thenReturn(Optional.of(tweet));
        when(currentUserService.getCurrentUser()).thenReturn(other);
        when(currentUserService.isAdmin(other)).thenReturn(false);

        assertThrows(UnauthorizedActionException.class, () -> tweetService.delete(10L));
        verify(tweetRepository, never()).delete(any());
    }

    @Test
    void quote_createsQuoteTweet_referencingOriginal() {
        when(currentUserService.getCurrentUser()).thenReturn(other);
        when(tweetRepository.findById(10L)).thenReturn(Optional.of(tweet));
        when(tweetRepository.save(any(Tweet.class))).thenAnswer(inv -> {
            Tweet t = inv.getArgument(0);
            t.setId(11L);
            return t;
        });

        TweetResponse response = tweetService.quote(new CreateQuoteRequest(10L, "look at this"));

        assertEquals("look at this", response.content());
        assertEquals(2L, response.userId());
        assertEquals(10L, response.quotedTweetId());
    }

    @Test
    void quote_throws_whenQuotedTweetNotFound() {
        when(currentUserService.getCurrentUser()).thenReturn(other);
        when(tweetRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> tweetService.quote(new CreateQuoteRequest(10L, "x")));
    }
}