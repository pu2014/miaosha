package com.pu.service;

import com.pu.error.BusinessException;
import com.pu.service.model.OrderModel;

/**
 * Description:
 * Created By @Author my on @Date 2020/3/26 22:26
 */
public interface IOrderService {
    OrderModel createOrder(Integer userId, Integer itemId, Integer amount) throws BusinessException;
}
