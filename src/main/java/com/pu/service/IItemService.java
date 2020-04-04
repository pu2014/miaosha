package com.pu.service;

import com.pu.error.BusinessException;
import com.pu.service.model.ItemModel;
import org.springframework.beans.factory.InitializingBean;

import java.util.List;

/**
 * Description:
 * Created By @Author my on @Date 2020/3/26 11:13
 */
public interface IItemService {
    //创建商品
    ItemModel createItem(ItemModel itemModel) throws BusinessException;
    //商品列表浏览
    List<ItemModel> listItem();
    //商品详情浏览
    ItemModel getItemById(Integer id);
    //验证item及promo 缓存模型是否有效
    ItemModel getItemByIdInCache(Integer id);
    //库存扣减
    boolean decreaseStock(Integer itemId, Integer amount);
    //库存回补
    boolean increaseStock(Integer itemId, Integer amount);
    //异步更新库存
    boolean asynDecreaseStock(Integer itemId, Integer amount);
    //商品销量增加
    void increaseSales(Integer itemId, Integer amount);
    //初始化库存流水
    String initStockLog(Integer itemId, Integer amount);

}
