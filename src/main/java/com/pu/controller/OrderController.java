package com.pu.controller;

import com.pu.commom.ReturnType;
import com.pu.error.BusinessException;
import com.pu.error.EmBusinessError;
import com.pu.service.IOrderService;
import com.pu.service.model.OrderModel;
import com.pu.service.model.UserModel;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * Description:
 * Created By @Author my on @Date 2020/3/26 23:52
 */
@Controller("order")
@RequestMapping("/order")
@CrossOrigin(allowCredentials = "true", allowedHeaders = "*")
public class OrderController extends baseController {

    @Autowired
    private IOrderService orderService;

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Qualifier("redisTemplate")
    @Autowired
    private RedisTemplate redisTemplate;

    //封装下单请求
    @ResponseBody
    @RequestMapping(value="/createorder",method = {RequestMethod.POST},consumes = {CONTENT_TYPE_FORMED})
    public ReturnType createOrder(@RequestParam(name="itemId")Integer itemId,
                                  @RequestParam(name="amount")Integer amount,
                                  @RequestParam(name="promoId",required = false)Integer promoId) throws BusinessException {

        String token = httpServletRequest.getParameterMap().get("token")[0];
        if(StringUtils.isEmpty(token)){
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN, "用户登录信息异常");
        }
        //获取用户的登录信息
        UserModel userModel = (UserModel) redisTemplate.opsForValue().get(token);
        if(userModel == null){
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN, "用户登录信息已经过期");
        }
        //基于cookie的方式
        /*
        Boolean isLogin = (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");
        if(isLogin == null || !isLogin.booleanValue()){
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN, "用户登录信息异常");
        }
        UserModel loginUser = (UserModel) httpServletRequest.getSession().getAttribute("LOGIN_USER");
        */

        OrderModel order = orderService.createOrder(userModel.getId(), itemId, promoId, amount);
        return ReturnType.create(null);
    }
}
