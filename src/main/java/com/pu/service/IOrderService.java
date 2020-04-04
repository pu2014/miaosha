package com.pu.service;

import com.pu.error.BusinessException;
import com.pu.service.model.OrderModel;

/**
 * Description:
 * Created By @Author my on @Date 2020/3/26 22:26
 */
public interface IOrderService {
    //使用方式一：通过前端url上传过来秒杀商品活动id，然后下单接口校验对应id是否属于对应商品且活动已经开始
    OrderModel createOrder(Integer userId, Integer itemId, Integer promoId, Integer amount, String stockLogId) throws BusinessException;
    //方式二：直接在下单接口内判断对应商品是否存在秒杀活动，若存在进行中的则以秒杀价格下单
}
