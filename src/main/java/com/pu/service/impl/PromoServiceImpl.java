package com.pu.service.impl;

import com.pu.dao.PromoMapper;
import com.pu.domain.Promo;
import com.pu.service.IPromoService;
import com.pu.service.model.PromoModel;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Description:
 * Created By @Author my on @Date 2020/3/27 10:52
 */
@Service
public class PromoServiceImpl  implements IPromoService {

    @Autowired
    private PromoMapper promoMapper;
    @Override
    public PromoModel getPromoByItemId(Integer itemId) {
        //获取秒杀活动信息
        Promo promo = promoMapper.selectByItemId(itemId);
        //dataObject -- > model
        PromoModel promoModel = convertModelFromDataObject(promo);
        if(promoModel == null){
            return null;
        }
        //判断当前活动是否存在
        DateTime now = new DateTime();
        if(promoModel.getStartDate().isAfterNow()){
            promoModel.setStatus(1);
        }else if(promoModel.getEndDate().isBeforeNow()){
            promoModel.setStatus(3);
        }else{
            promoModel.setStatus(2);
        }
        return promoModel;
    }


    private PromoModel convertModelFromDataObject(Promo promo){
        if(promo == null){
            return null;
        }
        PromoModel promoModel = new PromoModel();
        BeanUtils.copyProperties(promo, promoModel);
        promoModel.setPromoItemPrice(new BigDecimal(promo.getPromoItemPrice()));
        promoModel.setStartDate(new DateTime(promo.getStartDate()));
        promoModel.setEndDate(new DateTime(promo.getEndDate()));
        return promoModel;
    }
}
