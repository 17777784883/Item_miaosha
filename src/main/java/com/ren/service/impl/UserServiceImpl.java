package com.ren.service.impl;

import com.alibaba.druid.util.StringUtils;
import com.ren.dao.UserDOMapper;
import com.ren.dao.UserPasswordDOMapper;
import com.ren.dataObject.UserDO;
import com.ren.dataObject.UserPasswordDO;
import com.ren.error.BusinessException;
import com.ren.error.EmBusinessError;
import com.ren.service.UserService;
import com.ren.service.model.UserModel;
import com.ren.validator.ValidationResult;
import com.ren.validator.ValidatorImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

/**
 * @author Ren
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserDOMapper userDOMapper;

    @Autowired
    private UserPasswordDOMapper userPasswordDOMapper;

    @Autowired
    private ValidatorImpl validator;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 根据id查询用户信息
     *
     * @param id
     * @return
     */
    @Override
    public UserModel getUserById(Integer id) {
        //System.out.println("进入获取user信息的方法");
        // 调用对应的userdomapper 获取到对应的用户dataObject
        UserDO userDo = userDOMapper.selectByPrimaryKey(id);

        if (userDo == null) {
            //System.out.println("获取user信息错误");
            return null;
        }
        //System.out.println("user获取完毕"+userDo.toString());

        // 通过用户的id获取用户的加密 密码信息
        UserPasswordDO userPasswordDO = userPasswordDOMapper.selectByUserId(userDo.getId());
        return convertFromDataObject(userDo, userPasswordDO);

    }


    @Override
    public UserModel getUserByIdInCache(Integer id) {

        UserModel userModel = (UserModel) redisTemplate.opsForValue().get("user_validate_"+id);
        if(userModel==null){
            userModel = this.getUserById(id);
            redisTemplate.opsForValue().set("user_validate_"+id,userModel);
            redisTemplate.expire("user_validate_"+id,10, TimeUnit.MINUTES);
        }

        return userModel;
    }

    /**
     * 信息注册
     *
     * @param userModel
     * @throws BusinessException
     */
    @Override
    @Transactional//声明事务
    public void register(UserModel userModel) throws Exception {


        System.out.println("进入注册方法");

        //校验
        if (userModel == null) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR);
        }
        System.out.println("前端传入参数不为空");


        if (StringUtils.isEmpty(userModel.getName())
                || userModel.getGender() == null
                || userModel.getAge() == null
                || StringUtils.isEmpty(userModel.getTelphone())) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR);
        }

        System.out.println("validate校验");
        /*ValidationResult result = validator.validate(userModel);
        System.out.println("校验完成");
        if (result.isHasErrors()) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, result.getErrMsg());
        }*/

        //实现model->dataobject方法
        UserDO userDO = convertFromModel(userModel);

        //insertSelective相对于insert方法，不会覆盖掉数据库的默认值

        try {
            System.out.println("进入向数据库中插入数据的方法");
            userDOMapper.insertSelective(userDO);
        } catch (Exception e) {
            //e.printStackTrace();
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"该手机号已注册");

        }

        userModel.setId(userDO.getId());

        UserPasswordDO userPasswordDO = convertPasswordFromModel(userModel);
        userPasswordDOMapper.insertSelective(userPasswordDO);

        return;
    }


    @Override
    public UserModel validateLogin(String telphone, String encrptPassword) throws BusinessException {
        // 通过用户的手机获取用户的信息
        UserDO userDO = userDOMapper.selectByTelphone(telphone);
        if(userDO == null){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR);
        }
        UserPasswordDO userPasswordDO = userPasswordDOMapper.selectByUserId(userDO.getId());
        UserModel userModel = convertFromDataObject(userDO,userPasswordDO);

        //比对信息内加密的密码 是否和传输的密码是否一致
        if (!StringUtils.equals(encrptPassword,userModel.getEncrptPassword())) {
            throw new BusinessException(EmBusinessError.USER_LOOGIN_FAIL);
        }
        return userModel;

    }

    /**
     * 将userModel 转化成 UserPasswordDO
     *
     * @param userModel
     * @return
     */
    private UserPasswordDO convertPasswordFromModel(UserModel userModel) {
        if (userModel == null) {
            return null;
        }
        UserPasswordDO userPasswordDO = new UserPasswordDO();
        userPasswordDO.setEncrptPassword(userModel.getEncrptPassword());
        userPasswordDO.setUserId(userModel.getId());

        return userPasswordDO;
    }

    /**
     * 将userModel转化成userModel
     *
     * @param userModel
     * @return
     */
    private UserDO convertFromModel(UserModel userModel) {
        if (userModel == null) {
            return null;
        }
        UserDO userDO = new UserDO();
        BeanUtils.copyProperties(userModel, userDO);
        return userDO;
    }

    /**
     * 将userDo 和 passeordDo 转化成userModel类型
     *
     * @param userDO
     * @param userPasswordDO
     * @return
     */
    private UserModel convertFromDataObject(UserDO userDO, UserPasswordDO userPasswordDO) {
        if (userDO == null) {
            return null;
        }
        UserModel userModel = new UserModel();
        BeanUtils.copyProperties(userDO, userModel);
        if (userPasswordDO != null) {
            userModel.setEncrptPassword(userPasswordDO.getEncrptPassword());
        }

        return userModel;
    }


}
