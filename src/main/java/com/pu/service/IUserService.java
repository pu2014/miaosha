package com.pu.service;

import com.pu.error.BusinessException;
import com.pu.service.model.UserModel;

/**
 * Description:
 * Created By @Author my on @Date 2020/3/22 15:21
 */
public interface IUserService {
    UserModel getUserById(Integer id);
    UserModel getUserByIdInCache(Integer id);
    void register(UserModel userModel) throws BusinessException;
    UserModel vaildateLogin(String telephone, String password) throws BusinessException;
}
