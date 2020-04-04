package com.pu.service.impl;

import com.pu.dao.PromoMapper;
import com.pu.domain.Promo;
import com.pu.error.BusinessException;
import com.pu.error.EmBusinessError;
import com.pu.service.IItemService;
import com.pu.service.IPromoService;
import com.pu.service.IUserService;
import com.pu.service.model.ItemModel;
import com.pu.service.model.PromoModel;
import com.pu.service.model.UserModel;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Description:
 * Created By @Author my on @Date 2020/3/27 10:52
 */
@Service
public class PromoServiceImpl  implements IPromoService {

    @Autowired
    private PromoMapper promoMapper;

    @Autowired
    private IItemService itemService;
    @Autowired
    @Qualifier("redisTemplate")
    private RedisTemplate redisTemplate;

    @Autowired
    private IUserService userService;
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

    @Override
    public void PublishPromo(Integer promoId) {
        //通过id获取活动
        Promo promo = promoMapper.selectByPrimaryKey(promoId);
        if(promo.getItemId() == null || promo.getItemId().intValue() == 0){
            //没有对应的商品
            return;
        }
        ItemModel itemModel = itemService.getItemById(promo.getItemId());
        //库存同步到redis中
        redisTemplate.opsForValue().set("promo_item_stock_" + itemModel.getId(), itemModel.getStock());
        redisTemplate.expire("promo_item_stock_" + itemModel.getId(), 2, TimeUnit.HOURS);

        //将大闸的限制数字设置到redis内  库存的5倍数
        redisTemplate.opsForValue().set("promo_door_count_" + promoId, itemModel.getStock().intValue() * 5);
        redisTemplate.expire("promo_door_count_" + promoId, 2, TimeUnit.HOURS);
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

    @Override
    public String generateSecondKillToken(Integer promoId, Integer itemId, Integer userId) {

        //redisTemplate.opsForValue().get("promo_item_stock_invalid_" + itemId);
        if(redisTemplate.hasKey("promo_item_stock_invalid_" + itemId)){
            return null;
        }
        //获取秒杀活动信息
        Promo promo = promoMapper.selectByPrimaryKey(promoId);
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
        //判断商品信息是否存在
        ItemModel itemModel = itemService.getItemByIdInCache(itemId);
        if(itemModel == null){
            return null;
        }
        if(promoModel.getStatus().intValue() != 2){
            return null;
        }
        //判断用户是否存在
        UserModel userModel = userService.getUserByIdInCache(userId);
        if(userModel == null){
            return null;
        }

        //获取秒杀大闸的count数量
        long result = redisTemplate.opsForValue().increment("promo_door_count_" + promoId, -1);
        if(result < 0){
            return null;
        }

        //生成令牌 且设置有效时期
        String token = UUID.randomUUID().toString().replace("-", "");
        String tokenIndex = "promo_token_" + promoId + "_userId_" + userId + "_itemId_" + itemId;
        redisTemplate.opsForValue().set(tokenIndex, token);
        redisTemplate.expire(tokenIndex, 5, TimeUnit.MINUTES);
        return token;
    }
}
