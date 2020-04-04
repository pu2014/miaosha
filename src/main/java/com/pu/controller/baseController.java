package com.pu.controller;

import com.pu.response.ReturnType;
import com.pu.error.BusinessException;
import com.pu.error.EmBusinessError;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * Description:
 * Created By @Author my on @Date 2020/3/22 17:16
 */
public class baseController {

    public static final String CONTENT_TYPE_FORMED="application/x-www-form-urlencoded";

    //定义exceptionhandler 解决未被controller层吸收的exception
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public Object handlerException(HttpServletRequest request, Exception e){
        Map<String, Object> responseData = new HashMap<>();
        if(e instanceof BusinessException) {
            //防止反序列化,封装成我们自己的异常类
            BusinessException businessException = (BusinessException) e;
            //得到封装类中的信息
            responseData.put("errCode", businessException.getErrCode());
            responseData.put("errMsg", businessException.getErrMsg());
        }else{
            responseData.put("errCode", EmBusinessError.UNKNOWN_ERROR.getErrCode());
            responseData.put("errMsg", EmBusinessError.UNKNOWN_ERROR.getErrMsg());
        }
        return ReturnType.create(responseData, "fail");
    }
}
