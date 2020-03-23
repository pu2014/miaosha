package com.pu.commom;

/**
 * Description:统一返回结果
 * Created By @Author my on @Date 2020/3/22 15:57
 */
public class ReturnType {
    //表明对应请求的返回处理结果“success“or "fail"
    private String status;

    //如果status正确，data返回json数据 否者返回通用的错误码格式
    private Object data;

    //定义一个通用的创建方法
    public static ReturnType create(Object result){
        return ReturnType.create(result,"success");
    }

    public static ReturnType create(Object result, String status) {
        ReturnType returnType = new ReturnType();
        returnType.setStatus(status);
        returnType.setData(result);
        return returnType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
