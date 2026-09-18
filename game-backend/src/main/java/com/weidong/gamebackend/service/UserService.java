package com.weidong.gamebackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.weidong.gamebackend.model.User;

/**
 * 用户服务: 注册、登录校验与查询。
 */
public interface UserService extends IService<User> {

    /** 注册新用户（用户名唯一校验 + BCrypt 哈希），返回落库后的用户 */
    User register(String username, String rawPassword);

    /** 登录校验，失败抛 LOGIN_FAILED 业务异常（不区分用户不存在/密码错误，防枚举） */
    User login(String username, String rawPassword);

    /** 按用户名查询 */
    User findByUsername(String username);
}
