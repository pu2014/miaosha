package com.pu.service.impl;

import com.pu.dao.UserMapper;
import com.pu.dao.UserPasswordMapper;
import com.pu.domain.User;
import com.pu.domain.UserPassword;
import com.pu.error.BusinessException;
import com.pu.error.EmBusinessError;
import com.pu.service.IUserService;
import com.pu.service.model.UserModel;
import com.pu.validator.ValidationResult;
import com.pu.validator.ValidatorImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.validation.constraints.Min;
import java.net.URLDecoder;
import java.util.concurrent.TimeUnit;

/**
 * Description:
 * Created By @Author my on @Date 2020/3/22 15:22
 */
@Service
public class UserServiceImpl implements IUserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserPasswordMapper userPasswordMapper;

    @Autowired
    private ValidatorImpl validator;

    @Autowired
    @Qualifier("redisTemplate")
    private RedisTemplate redisTemplate;

    @Override
    public UserModel getUserById(Integer id) {
        User user = userMapper.selectByPrimaryKey(id);
        if(user == null){
            return null;
        }
        //用户id获取用户加密密码信息
        UserPassword userPassword = userPasswordMapper.selectByUserId(id);
        return convertFromDataObject(user, userPassword);
    }

    @Override
    public UserModel getUserByIdInCache(Integer id) {
        UserModel userModel = (UserModel) redisTemplate.opsForValue().get("user_validate_" + id);
        if(userModel == null){
            userModel = getUserById(id);
            redisTemplate.opsForValue().set("user_validate_" + id, userModel);
            redisTemplate.expire("user_validate_" + id, 10, TimeUnit.MINUTES);
        }
        return userModel;
    }

    /**
     * 用户注册
     * @param userModel
     */
    @Override
    public void register(UserModel userModel) throws BusinessException {
        if(userModel == null){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR);
        }
        /*if (StringUtils.isEmpty(userModel.getName())
                || userModel.getGender() == null
                || userModel.getAge() == null
                || StringUtils.isEmpty(userModel.getTelephone())){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR);
        }*/
        ValidationResult result = validator.validate(userModel);
        if(result.isHasErrors()){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,result.getErrMsg());
        }
        //实现model -> user
        //insertSelective 防止插入null，而是插入字段为null时，插入数据库默认的值。
        User user = convertFromUserModel(userModel);
        try{
            userMapper.insertSelective(user);
        }catch (DuplicateKeyException ex){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"手机号已经注册");
        }
        //等到插入后的id
        userModel.setId(user.getId());
        userPasswordMapper.insertSelective(convertPasswordFromModel(userModel));
    }

    @Override
    public UserModel vaildateLogin(String telephone, String password) throws BusinessException {
        //通过用户的手机获取用户信息
        User user = userMapper.selectByTelephone(telephone);
        if(user == null){
            throw new BusinessException(EmBusinessError.USER_LOGIN_FAIL);
        }
        UserPassword userPassword = userPasswordMapper.selectByUserId(user.getId());
        UserModel userModel = convertFromDataObject(user, userPassword);

        //密码校捡
        if(!StringUtils.equals(password, userModel.getEncrptPassword())){
            throw new BusinessException(EmBusinessError.USER_LOGIN_FAIL);
        }
        return userModel;


    }

    private UserPassword convertPasswordFromModel(UserModel userModel){
        if(userModel == null){
            return null;
        }
        UserPassword userPassword = new UserPassword();
        userPassword.setEncrptPassword(userModel.getEncrptPassword());
        userPassword.setUserId(userModel.getId());
        return userPassword;
    }


    private User convertFromUserModel(UserModel userModel){
        if(userModel == null){
            return null;
        }
        User user = new User();
        BeanUtils.copyProperties(userModel, user);
        return user;
    }

    /**
     * user + password 转换为 model
     * @param user
     * @param password
     * @return
     */
    private UserModel convertFromDataObject(User user, UserPassword password){
        if(user == null){
            return null;
        }
        UserModel model = new UserModel();
        BeanUtils.copyProperties(user, model);
        if(password != null){
            model.setEncrptPassword(password.getEncrptPassword());
        }
        return model;
    }
}
