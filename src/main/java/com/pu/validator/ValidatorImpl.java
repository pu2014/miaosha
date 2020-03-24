package com.pu.validator;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Set;

/**
 * Description:
 * Created By @Author my on @Date 2020/3/24 19:42
 */
@Component
public class ValidatorImpl implements InitializingBean {

    //javax.Validator
    private Validator validator;

    //实现校验方法并返回校验结果
    public ValidationResult validate(Object bean){
        ValidationResult result = new ValidationResult();
        Set<ConstraintViolation<Object>> constraintViolationSet = validator.validate(bean);
        if(constraintViolationSet.size() > 0){
            result.setHasErrors(true);
            constraintViolationSet.forEach(constraintViolation->{
                String errMsg = constraintViolation.getMessage();
                String propertyName = constraintViolation.getPropertyPath().toString();
                result.getErrMsgMap().put(propertyName, errMsg);
            });
        }
        return result;
    }

    @Override

    public void afterPropertiesSet() throws Exception {
        //将hibernate validator通过工厂的初始化方法使其实例化
        this.validator = Validation.buildDefaultValidatorFactory().getValidator();


    }
}
