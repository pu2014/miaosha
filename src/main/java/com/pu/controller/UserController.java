package com.pu.controller;

import com.alibaba.druid.util.StringUtils;
import com.pu.commom.ReturnType;
import com.pu.controller.view.UserView;
import com.pu.error.BusinessException;
import com.pu.error.EmBusinessError;
import com.pu.service.IUserService;
import com.pu.service.model.UserModel;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import sun.misc.BASE64Encoder;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Description:
 * @CrossOrigin(allowCredentials = "true",allowedHeaders = "*")
 * @CrossOrigin 任何域发过来默认授信
 * DEFAULT_ALLOWED_HEADERS 默认为true 需配合前端设置xhrFields授信后使得跨域session共享
 * Created By @Author my on @Date 2020/3/22 15:19
 */
@Controller("user")
@RequestMapping("/user")
@CrossOrigin(allowCredentials = "true", allowedHeaders = "*")
public class UserController extends baseController{

    @Autowired
    private IUserService userService;
    /**
     * bean模式是单列模式，多个用户的访问
     * HttpServletRequest 本质是个 proxy
     * getHttpServletMapping 让用户在自己的线程处理自己的request
     */
    @Autowired
    private HttpServletRequest httpServletRequest;

    @Qualifier("redisTemplate")
    @Autowired
    private RedisTemplate redisTemplatel;



    @ResponseBody
    @RequestMapping(value="/login",method = {RequestMethod.POST},consumes = {CONTENT_TYPE_FORMED})
    public ReturnType login(@RequestParam(name="telephone")String telephone, @RequestParam(name="password")String password) throws BusinessException, UnsupportedEncodingException, NoSuchAlgorithmException {
        //入参校验
        if(StringUtils.isEmpty(telephone) || StringUtils.isEmpty(password)){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR);
        }
        //用户登录是否合法
        UserModel userModel = userService.vaildateLogin(telephone, this.encodeByMd5(password));
        //加入到用户登录成功的session类
        //假设单点session登录
        /*
        this.httpServletRequest.getSession().setAttribute("IS_LOGIN", true);
        //存入redis缓存必须实现Serializable接口（也可以修改redis配置使其支持json格式）
        this.httpServletRequest.getSession().setAttribute("LOGIN_USER", userModel);
        */

        //基于Java实现token传输sessionid
        // 修改为用户登录验证成功将对应的登录信息和登录凭证一起存入redis中
        //生成登录凭证token，UUID
        String uuidToken = UUID.randomUUID().toString();
        uuidToken = uuidToken.replace("-", "");
        //建立token和用户登录态之间的联系
        redisTemplatel.opsForValue().set(uuidToken, userModel);
        //5分钟失效
        redisTemplatel.expire(uuidToken, 5, TimeUnit.MINUTES);
        return ReturnType.create(uuidToken);
    }
    /**
     * 用户注册的接口
     * @param telephone
     * @return
     */
    @ResponseBody
    @RequestMapping(value="/register",method = {RequestMethod.POST},consumes = {CONTENT_TYPE_FORMED})
    public ReturnType register(@RequestParam(name="telephone")String telephone,
                               @RequestParam(name="optCode")String optCode,
                               @RequestParam(name="name")String name,
                               @RequestParam(name="gender")Integer gender,
                               @RequestParam(name="age")Integer age,
                               @RequestParam(name="password")String password) throws BusinessException, UnsupportedEncodingException, NoSuchAlgorithmException {
        //验证手机号和对应的optcode相符合
        String inSessionOptCode = (String)httpServletRequest.getSession().getAttribute(telephone);
            // 用druid的比较防止都为null
        if(!StringUtils.equals(optCode, inSessionOptCode)){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"短信验证码不符合");
        }
        //用户注册
        UserModel userModel = new UserModel();
        userModel.setName(name);
        userModel.setGender(new Byte(String.valueOf(gender)));
        userModel.setAge(age);
        userModel.setTelephone(telephone);
        userModel.setRegisterMode("byphone");
        /**
         其中，Md5加密是采用了散列算法，也就是哈希算法，可以进行多次散列加密。Md5加密是不可逆的，无法解密。
         MD5是不可逆的单向加密方式,注册的时候如果密码用MD5的方式进行加密,那么在数据库中显示的密码就是经过MD5加密后的特征码,
         登录的时候,输入的密码会转换成MD5的格式与数据库的MD5特征码进行对比,一致就可以成功登录。
         */
        userModel.setEncrptPassword(this.encodeByMd5(password));
        userService.register(userModel);
        //返回用户界面
        return ReturnType.create(null);
    }

    public String encodeByMd5(String str) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        //确定计算方法
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        BASE64Encoder encoder = new BASE64Encoder();
        //加密字符串
        String newStr = encoder.encode(md5.digest(str.getBytes("utf-8")));
        return newStr;
    }

    //获取短信的接口
    @ResponseBody
    @RequestMapping(value="/getopt",method = {RequestMethod.POST},consumes = {CONTENT_TYPE_FORMED})
    public ReturnType getOtp(@RequestParam(name="telephone")String phoneNum){
        //生成OPT验证码
        Random random = new Random();
        int randomInt = random.nextInt(99999);
        randomInt += randomInt;
        String optCode = String.valueOf(randomInt);
        //OPT与用户手机的关联,使用httpsession的方式绑定

        httpServletRequest.getSession().setAttribute(phoneNum, optCode);
        //将OPT验证码通过短信通道发送给用户
        System.out.println("telephone =" + phoneNum + " & optCode =" +  httpServletRequest.getSession().getAttribute(phoneNum));
        return ReturnType.create(null);
    }

    @RequestMapping("/get")
    @ResponseBody
    public ReturnType getUser(@RequestParam(name="id")Integer id) throws BusinessException {
        UserModel userModel = userService.getUserById(id);


        //若对应用户不存在
        if(userModel == null){
            throw new BusinessException(EmBusinessError.USER_NOT_EXIST);
        }
        //将核心领域模型对象转化为可以供ui使用的页面展示对象
        UserView userView = convertFromModel(userModel);
        return ReturnType.create(userView);
    }

    private UserView convertFromModel(UserModel model){
        if(model == null){
            return null;
        }
        UserView userView = new UserView();
        BeanUtils.copyProperties(model, userView);
        return userView;
    }

}
