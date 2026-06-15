package com.workintech.twitterapi.service;

import com.workintech.twitterapi.dto.like.CreateLikeRequest;
import com.workintech.twitterapi.dto.like.LikeResponse;
import com.workintech.twitterapi.dto.tweet.TweetResponse;
import com.workintech.twitterapi.dto.user.UserResponse;
import com.workintech.twitterapi.entity.Like;
import com.workintech.twitterapi.entity.Tweet;
import com.workintech.twitterapi.entity.User;
import com.workintech.twitterapi.exception.BadRequestException;
import com.workintech.twitterapi.exception.ResourceNotFoundException;
import com.workintech.twitterapi.exception.UnauthorizedActionException;
import com.workintech.twitterapi.repository.ILikeRepository;
import com.workintech.twitterapi.repository.ITweetRepository;
import com.workintech.twitterapi.repository.IUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LikeServiceTest {

    @Mock ILikeRepository likeRepository;
    @Mock ITweetRepository tweetRepository;
    @Mock IUserRepository userRepository;
    @Mock CurrentUserService currentUserService;

    @InjectMocks LikeService likeService;

    private User user;
    private Tweet tweet;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);

        tweet = new Tweet();
        tweet.setId(10L);
        tweet.setUser(user);
    }

    @Test
    void like_savesLike_whenNotAlreadyLiked() {
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(tweetRepository.findById(10L)).thenReturn(Optional.of(tweet));
        when(likeRepository.existsByUserIdAndTweetId(1L, 10L)).thenReturn(false);
        Like saved = new Like();
        saved.setId(100L);
        saved.setUser(user);
        saved.setTweet(tweet);
        when(likeRepository.save(any(Like.class))).thenReturn(saved);

        LikeResponse response = likeService.like(new CreateLikeRequest(10L));

        assertEquals(1L, response.userId());
        assertEquals(10L, response.tweetId());
        verify(likeRepository).save(any(Like.class));
    }

    @Test
    void like_throws_whenAlreadyLiked() {
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(tweetRepository.findById(10L)).thenReturn(Optional.of(tweet));
        when(likeRepository.existsByUserIdAndTweetId(1L, 10L)).thenReturn(true);

        assertThrows(BadRequestException.class,
                () -> likeService.like(new CreateLikeRequest(10L)));
        verify(likeRepository, never()).save(any());
    }

    @Test
    void like_throws_whenTweetNotFound() {
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(tweetRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> likeService.like(new CreateLikeRequest(10L)));
    }

    @Test
    void dislike_deletesLike_whenExists() {
        when(currentUserService.getCurrentUser()).thenReturn(user);
        Like existing = new Like();
        existing.setId(100L);
        existing.setUser(user);
        existing.setTweet(tweet);
        when(likeRepository.findByUserIdAndTweetId(1L, 10L)).thenReturn(Optional.of(existing));

        likeService.dislike(new CreateLikeRequest(10L));

        verify(likeRepository).delete(existing);
    }

    @Test
    void dislike_throws_whenLikeNotFound() {
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(likeRepository.findByUserIdAndTweetId(1L, 10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> likeService.dislike(new CreateLikeRequest(10L)));
    }

    @Test
    void getUsersWhoLiked_returnsLikers_whenTweetOwner() {
        Like like = new Like();
        like.setUser(user);
        like.setTweet(tweet);
        when(tweetRepository.findById(10L)).thenReturn(Optional.of(tweet));
        when(likeRepository.findByTweet_Id(10L)).thenReturn(List.of(like));

        List<UserResponse> result = likeService.getUsersWhoLiked(10L);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).id());
    }

    @Test
    void getLikedTweets_returnsTweets_whenSelf() {
        Like like = new Like();
        like.setUser(user);
        like.setTweet(tweet);
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(likeRepository.findByUser_Id(1L)).thenReturn(List.of(like));

        List<TweetResponse> result = likeService.getLikedTweets(1L);

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).id());
    }

    @Test
    void getLikedTweets_throws_whenNotSelf() {
        User stranger = new User();
        stranger.setId(2L);
        when(currentUserService.getCurrentUser()).thenReturn(stranger);

        assertThrows(UnauthorizedActionException.class,
                () -> likeService.getLikedTweets(1L));
    }
}