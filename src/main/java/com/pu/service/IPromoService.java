package com.pu.service;

import com.pu.service.model.PromoModel;

/**
 * Description:
 * Created By @Author my on @Date 2020/3/27 10:50
 */
public interface IPromoService {
    PromoModel getPromoByItemId(Integer itemId);
    //活动发布
    void PublishPromo(Integer promoId);

    //生成秒杀活动用的令牌
    String generateSecondKillToken(Integer promoId, Integer itemId, Integer userId);
}
