package com.selimhorri.app.unit.service;

import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.repository.UserRepository;
import com.selimhorri.app.service.impl.UserServiceImpl;
import com.selimhorri.app.unit.util.UserUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private UserDto userDto;

    @BeforeEach
    void setUp() {
        userDto = UserUtil.getSampleUserDto();
    }

    @Test
    void testFindById_ShouldReturnUserDto() {
        when(userRepository.findById(userDto.getUserId())).thenReturn(Optional.of(UserUtil.getSampleUser()));

        UserDto result = userService.findById(userDto.getUserId());

        assertNotNull(result);
        assertEquals(userDto.getUserId(), result.getUserId());
        assertEquals(userDto.getFirstName(), result.getFirstName());
        assertEquals(userDto.getLastName(), result.getLastName());

    }

    @Test
    void testSave_ShouldReturnUserDto() {
        when(userRepository.save(any())).thenReturn(UserUtil.getSampleUser());

        UserDto result = userService.save(userDto);

        assertNotNull(result);
        assertEquals(userDto.getUserId(), result.getUserId());
        assertEquals(userDto.getFirstName(), result.getFirstName());
        assertEquals(userDto.getLastName(), result.getLastName());
    }


}


