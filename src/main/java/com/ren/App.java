package com.ren;

import com.ren.dao.UserDOMapper;
import com.ren.dataObject.UserDO;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ren
 */

@SpringBootApplication  //主启动类
@RestController
@MapperScan("com.ren.dao")
public class App {

    @Autowired
    private UserDOMapper userDOMapper;

    @RequestMapping("/")
    public String home(){
        UserDO userDO = userDOMapper.selectByPrimaryKey(1);
        if(userDO==null){
            return "用户对象不存在";
        }else{
            return userDO.getName();
        }
    }
    public static void main(String[] args) {

        System.out.println("hello world");
        SpringApplication.run(App.class,args);
    }

}


