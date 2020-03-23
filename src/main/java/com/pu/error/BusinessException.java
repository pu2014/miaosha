package com.pu.error;

/**
 * Description:
 * Created By @Author my on @Date 2020/3/22 16:42
 */

//包装器业务异常设计
public class BusinessException extends Exception implements Error {

    private Error error;
    //直接接受Error的传入参数用于构造业务异常
    public BusinessException(Error error) {
        super();
        this.error = error;
    }
    //接受自定义errMsg的方式构造业务异常
    public BusinessException(Error error, String errMsg){
        super();
        this.error = error;
        this.error.setErrMsg(errMsg);
    }

    @Override
    public int getErrCode() {
        return this.error.getErrCode();
    }

    @Override
    public String getErrMsg() {
        return this.error.getErrMsg();
    }

    @Override
    public Error setErrMsg(String errMsg) {
        this.error.setErrMsg(errMsg);
        return this;
    }
}
