package com.sky.service;

import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.result.Result;
import com.sky.vo.UserLoginVO;
import org.springframework.web.bind.annotation.RequestBody;

public interface UserService {

    /**
     * 微信登录
     * @param userLoginDTO
     */
    User login(UserLoginDTO userLoginDTO);
}
