package com.pu.validator;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Description:
 * Created By @Author my on @Date 2020/3/24 19:36
 */
public class ValidationResult {
    //校验是否有错
    private boolean hasErrors = false;

    //存放错误信息的map
    private Map<String, String> errMsgMap = new HashMap<>();

    public boolean isHasErrors() {
        return hasErrors;
    }

    public void setHasErrors(boolean hasErrors) {
        this.hasErrors = hasErrors;
    }

    public Map<String, String> getErrMsgMap() {
        return errMsgMap;
    }

    public void setErrMsgMap(Map<String, String> errMsgMap) {
        this.errMsgMap = errMsgMap;
    }
    //实现通用的通过格式化字符串的信息后去错误的结果的msg方法
    public String getErrMsg(){
        return StringUtils.join(errMsgMap.values().toArray(),",");
    }


}

