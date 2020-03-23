package com.pu.error;

/**
 * Description:
 * Created By @Author my on @Date 2020/3/22 16:33
 */
public interface Error {
    public int getErrCode();
    public String getErrMsg();
    public Error setErrMsg(String errMsg);
}
