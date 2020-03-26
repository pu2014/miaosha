package com.pu.controller;

import com.pu.commom.ReturnType;
import com.pu.error.BusinessException;
import com.pu.error.EmBusinessError;
import com.pu.service.IOrderService;
import com.pu.service.model.OrderModel;
import com.pu.service.model.UserModel;
import org.springframework.beans.factory.annotation.Autowired;
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

    //封装下单请求
    @ResponseBody
    @RequestMapping(value="/createorder",method = {RequestMethod.POST},consumes = {CONTENT_TYPE_FORMED})
    public ReturnType createOrder(@RequestParam(name="itemId")Integer itemId,
                                  @RequestParam(name="amount")Integer amount) throws BusinessException {

        //获取用户的登录信息
        Boolean isLogin = (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");
        if(isLogin == null || !isLogin.booleanValue()){
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN, "用户登录信息异常");
        }
        UserModel loginUser = (UserModel) httpServletRequest.getSession().getAttribute("LOGIN_USER");


        OrderModel order = orderService.createOrder(loginUser.getId(), itemId, amount);
        return ReturnType.create(null);
    }
}
