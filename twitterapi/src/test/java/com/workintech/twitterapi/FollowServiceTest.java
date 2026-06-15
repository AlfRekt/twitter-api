package com.workintech.twitterapi;

import com.workintech.twitterapi.dto.follow.CreateFollowRequest;
import com.workintech.twitterapi.dto.follow.FollowResponse;
import com.workintech.twitterapi.dto.user.UserResponse;
import com.workintech.twitterapi.entity.Follow;
import com.workintech.twitterapi.entity.User;
import com.workintech.twitterapi.exception.BadRequestException;
import com.workintech.twitterapi.exception.ResourceNotFoundException;
import com.workintech.twitterapi.repository.IFollowRepository;
import com.workintech.twitterapi.repository.IUserRepository;
import com.workintech.twitterapi.service.CurrentUserService;
import com.workintech.twitterapi.service.FollowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FollowServiceTest {

    @Mock IFollowRepository followRepository;
    @Mock IUserRepository userRepository;
    @Mock
    CurrentUserService currentUserService;

    @InjectMocks
    FollowService followService;

    private User follower;
    private User following;

    @BeforeEach
    void setUp() {
        follower  = new User(); follower.setId(1L);
        following = new User(); following.setId(2L);
    }

    @Test
    void follow_succeeds_whenValid() {
        when(currentUserService.getCurrentUser()).thenReturn(follower);
        when(userRepository.findById(2L)).thenReturn(Optional.of(following));
        when(followRepository.existsByFollower_IdAndFollowing_Id(1L, 2L)).thenReturn(false);
        when(followRepository.save(any(Follow.class))).thenAnswer(inv -> {
            Follow f = inv.getArgument(0);
            f.setId(500L);
            return f;
        });

        FollowResponse response = followService.follow(new CreateFollowRequest(2L));

        assertEquals(1L, response.followerId());
        assertEquals(2L, response.followingId());
    }

    @Test
    void follow_throws_whenFollowingSelf() {
        when(currentUserService.getCurrentUser()).thenReturn(follower);
        when(userRepository.findById(1L)).thenReturn(Optional.of(follower));

        assertThrows(BadRequestException.class,
                () -> followService.follow(new CreateFollowRequest(1L)));
        verify(followRepository, never()).save(any());
    }

    @Test
    void follow_throws_whenAlreadyFollowing() {
        when(currentUserService.getCurrentUser()).thenReturn(follower);
        when(userRepository.findById(2L)).thenReturn(Optional.of(following));
        when(followRepository.existsByFollower_IdAndFollowing_Id(1L, 2L)).thenReturn(true);

        assertThrows(BadRequestException.class,
                () -> followService.follow(new CreateFollowRequest(2L)));
        verify(followRepository, never()).save(any());
    }

    @Test
    void follow_throws_whenTargetUserNotFound() {
        when(currentUserService.getCurrentUser()).thenReturn(follower);
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> followService.follow(new CreateFollowRequest(2L)));
    }

    @Test
    void unfollow_succeeds_whenFollowing() {
        Follow follow = new Follow();
        follow.setId(500L); follow.setFollower(follower); follow.setFollowing(following);
        when(currentUserService.getCurrentUser()).thenReturn(follower);
        when(followRepository.findByFollower_IdAndFollowing_Id(1L, 2L)).thenReturn(Optional.of(follow));

        followService.unfollow(2L);

        verify(followRepository).delete(follow);
    }

    @Test
    void unfollow_throws_whenNotFollowing() {
        when(currentUserService.getCurrentUser()).thenReturn(follower);
        when(followRepository.findByFollower_IdAndFollowing_Id(1L, 2L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> followService.unfollow(2L));
    }

    @Test
    void getFollowing_returnsUsersThisUserFollows() {
        Follow follow = new Follow();
        follow.setFollower(follower); follow.setFollowing(following);
        when(userRepository.findById(1L)).thenReturn(Optional.of(follower));
        when(followRepository.findByFollower_Id(1L)).thenReturn(List.of(follow));

        List<UserResponse> result = followService.getFollowing(1L);

        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).id());   // reads the "following" side
    }

    @Test
    void getFollowers_returnsUsersWhoFollowThisUser() {
        Follow follow = new Follow();
        follow.setFollower(follower); follow.setFollowing(following);
        when(userRepository.findById(2L)).thenReturn(Optional.of(following));
        when(followRepository.findByFollowing_Id(2L)).thenReturn(List.of(follow));

        List<UserResponse> result = followService.getFollowers(2L);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).id());
    }
}