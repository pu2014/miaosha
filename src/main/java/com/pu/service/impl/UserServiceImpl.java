package com.pu.service.impl;

import com.pu.dao.UserMapper;
import com.pu.dao.UserPasswordMapper;
import com.pu.domain.User;
import com.pu.domain.UserPassword;
import com.pu.error.BusinessException;
import com.pu.error.EmBusinessError;
import com.pu.service.IUserService;
import com.pu.service.model.UserModel;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

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

    /**
     * 用户注册
     * @param userModel
     */
    @Override
    public void register(UserModel userModel) throws BusinessException {
        if(userModel == null){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR);
        }
        if (StringUtils.isEmpty(userModel.getName())
                || userModel.getGender() == null
                || userModel.getAge() == null
                || StringUtils.isEmpty(userModel.getTelephone())){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR);
        }
        //实现model -> user
        //insertSelective 防止插入null，而是插入字段为null时，插入数据库默认的值。
        User user = convertFromUserModel(userModel);
        try{
            userMapper.insertSelective(user);
        }catch (DuplicateKeyException ex){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"手机号已经注册");
        }
        userMapper.insertSelective(user);
        //等到插入后的id
        userModel.setId(user.getId());
        userPasswordMapper.insertSelective(convertPasswordFromModel(userModel));
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