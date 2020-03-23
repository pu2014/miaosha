package com.pu.error;

/**
 * Description:
 * Created By @Author my on @Date 2020/3/22 16:35
 */
public enum EmBusinessError implements Error {
    //通用错误类型00001
    PARAMETER_VALIDATION_ERROR(10001,"参数不合法"),
    //10000开头表示为用户信息相关错误定义
    USER_NOT_EXIST(10001,"用户不存在"),
    //未知错误
    UNKNOWN_ERROR(30001,"未知错误")
    ;

    EmBusinessError(int errCode, String errMsg) {
        this.errCode = errCode;
        this.errMsg = errMsg;
    }

    private int errCode;
    private String errMsg;


    @Override
    public int getErrCode() {
        return this.errCode;
    }

    @Override
    public String getErrMsg() {
        return this.errMsg;
    }

    @Override
    public Error setErrMsg(String errMsg) {
        this.errMsg = errMsg;
        return this;
    }
}
