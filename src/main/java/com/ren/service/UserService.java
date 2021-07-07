package com.ren.service;

import com.ren.error.BusinessException;
import com.ren.service.model.UserModel;
import org.springframework.stereotype.Repository;

/**
 * @author Ren
 */

@Repository
public interface UserService {

    // 通过用户id获取用户对象的方法
    UserModel getUserById(Integer id);

    // 通过缓存获取用户对象
    UserModel getUserByIdInCache(Integer id);

    //用户注册
    void register(UserModel userModel) throws Exception;


    /**
     *
     * @param telphone  用户注册手机
     * @param encrptPassword  用户加密后的密码
     * @throws BusinessException
     */
    // 用户登录
    UserModel validateLogin(String telphone, String encrptPassword) throws BusinessException;

}
