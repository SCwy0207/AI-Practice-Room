package com.victor.ai_practice_room.service.impl;

import com.victor.ai_practice_room.entity.User;
import com.victor.ai_practice_room.mapper.UserMapper;
import com.victor.ai_practice_room.service.UserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import org.springframework.stereotype.Service;

/**
 * 用户Service实现类
 * 实现用户相关的业务逻辑
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

} 