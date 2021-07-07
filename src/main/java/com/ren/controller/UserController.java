package com.ren.controller;

import com.alibaba.druid.util.StringUtils;
import com.ren.controller.viewObject.UserVO;
import com.ren.error.BusinessException;
import com.ren.error.EmBusinessError;
import com.ren.response.CommonReturnType;
import com.ren.service.UserService;
import com.ren.service.model.UserModel;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import sun.misc.BASE64Encoder;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author Ren
 */

@Controller
@RequestMapping("/user")
@CrossOrigin(allowCredentials = "true",allowedHeaders = "*")
public class UserController extends BaseController {

    @Autowired
    private UserService userService;


    @Autowired
    private HttpServletRequest httpServletRequest;


    @Autowired
    private RedisTemplate redisTemplate;

    // 用户登录接口
    @RequestMapping(value = "/login", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType login(@RequestParam(name="telphone") String telhone,
                                  @RequestParam(name="password") String password) throws BusinessException, UnsupportedEncodingException, NoSuchAlgorithmException {
        // 入参校验
        if(StringUtils.isEmpty(telhone)||
            StringUtils.isEmpty(password)){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR);
        }

        // 用户登录服务，用来校验用户登录是否合法
        UserModel userModel = userService.validateLogin(telhone, this.EncodeByMD5(password));


        // 将登录凭证加入到用户登录成功的session内
        // this.httpServletRequest.getSession().setAttribute("IS_LOGIN",true);
        // this.httpServletRequest.getSession().setAttribute("LOGIN_USER",userModel);


        /*修改成若用户登录验证成功后将对应的登录信息和登录凭证一起存入redis中*/
        // 生成登录凭证token, UUID
        String uuidToken = UUID.randomUUID().toString();
        uuidToken = uuidToken.replaceAll("-", "");

        // 建立token和用户态之间的联系，uuidToken的类型为userModel
        redisTemplate.opsForValue().set(uuidToken,userModel, Duration.ofMillis(30L));
        //redisTemplate.expire(uuidToken,1, TimeUnit.HOURS);

        //  下发了Token，返回给前台，前台将其
        return CommonReturnType.create(uuidToken);



    }


    /**
     * 用户注册接口
     *
     * @return
     * @throws BusinessException
     */
    @RequestMapping(value = "/register", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType register(@RequestParam(name = "telphone") String telphone,
                                     @RequestParam(name = "otpCode") String otpCode,
                                     @RequestParam(name = "name") String name,
                                     @RequestParam(name = "gender") String gender,
                                     @RequestParam(name = "age") Integer age,
                                     @RequestParam(name = "password") String password
    ) throws Exception {
        //验证手机号 和对应的code相符合
        System.out.println(telphone+"  "+otpCode+"   "+name+"   "+gender+"   "+age+"   "+password);
        String inSessionOtpCode = (String) this.httpServletRequest.getSession().getAttribute(telphone);
        System.out.println("生成的验证码: "+inSessionOtpCode);
        if (!StringUtils.equals(otpCode, inSessionOtpCode)) {
            System.out.println("短信验证码不符合");
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "短信验证码不符合");
        }
        // 用户的注册流程
        UserModel userModel = new UserModel();
        userModel.setName(name);
        userModel.setGender(Byte.valueOf(gender));
        userModel.setAge(age);
        userModel.setTelphone(telphone);
        userModel.setRegisterMode("byphone");
        userModel.setEncrptPassword(this.EncodeByMD5(password));
        userService.register(userModel);

        return CommonReturnType.create(userModel);
    }

    public String EncodeByMD5(String str) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        //确定计算方法
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        BASE64Encoder base64Encoder = new BASE64Encoder();
        //加密字符串
        String newstr = base64Encoder.encode(md5.digest(str.getBytes("utf-8")));
        return newstr;

    }

    /**
     * 用户获取otp短信的接口
     */
    @RequestMapping(value = "/getotp", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType getOtp(@RequestParam(name = "telphone") String telphone) {
        // 需要按照一定的规则生成otp验证码
        Random random = new Random();
        int randomInt = random.nextInt(99999);
        randomInt += 10000;
        String otpCode = String.valueOf(randomInt);


        //将otp验证码同对应的用户的手机号关联,使用httpsession的方式绑定他的手机号与otpcode
        httpServletRequest.getSession().setAttribute(telphone, otpCode);


        //将otp验证码通过短信通道发送给用户，暂时省略
        System.out.println("telphone = " + telphone + "& otpCode = " + otpCode);


        return CommonReturnType.create(null);

    }


    /**
     * 查询用户信息
     *
     * @param id
     * @return
     * @throws BusinessException
     */
    @ResponseBody
    @RequestMapping("/get")
    public CommonReturnType getUser(@RequestParam(name = "id") Integer id) throws BusinessException {
        //调用service服务获取对id的用户对象并返回给前端
        UserModel userModel = userService.getUserById(id);

        // 若获取用户信息不存在
        if (userModel == null) {
            throw new BusinessException(EmBusinessError.USER_NOT_EXIST);
        }

        // 将核心领域模型用户转化为可供UI使用的ViewObject
        UserVO userVO = convertFromModel(userModel);


        // 返回通用对象
        return CommonReturnType.create(userVO);
    }

    private UserVO convertFromModel(UserModel userModel) {
        if (userModel == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(userModel, userVO);
        return userVO;
    }


    @ResponseBody
    @RequestMapping("/test")
    public String test() throws UnknownHostException {
        InetAddress inetAddress = InetAddress.getLocalHost();
        String hostAddress = inetAddress.getHostAddress();

        return hostAddress.toString();

    }

}
