package com.pu.service.impl;

import ch.qos.logback.classic.turbo.TurboFilter;
import com.pu.dao.ItemMapper;
import com.pu.dao.ItemStockMapper;
import com.pu.dao.StockLogMapper;
import com.pu.domain.Item;
import com.pu.domain.ItemStock;
import com.pu.domain.StockLog;
import com.pu.error.BusinessException;
import com.pu.error.EmBusinessError;
import com.pu.mq.MqProducer;
import com.pu.service.IItemService;
import com.pu.service.IPromoService;
import com.pu.service.model.ItemModel;
import com.pu.service.model.PromoModel;
import com.pu.validator.ValidationResult;
import com.pu.validator.ValidatorImpl;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * Description:
 * Created By @Author my on @Date 2020/3/26 11:15
 */
@Service
public class ItemServiceImpl implements IItemService {

    @Autowired
    private ValidatorImpl validator;

    @Autowired
    private ItemMapper itemMapper;

    @Autowired
    private ItemStockMapper itemStockMapper;

    @Autowired
    private IPromoService promoService;

    @Autowired
    @Qualifier("redisTemplate")
    private RedisTemplate redisTemplate;

    @Autowired
    private MqProducer mqProducer;

    @Autowired
    private StockLogMapper stockLogMapper;



    /**
     * 商品创建
     * @param itemModel
     * @return
     * @throws BusinessException
     */
    @Override
    @Transactional
    public ItemModel createItem(ItemModel itemModel) throws BusinessException {
        //校验入参
        ValidationResult result = validator.validate(itemModel);
        if(result.isHasErrors()){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, result.getErrMsg());
        }
        //model -- dataObject
        Item item = convertItemFromItemModel(itemModel);


        //写入数据库
        itemMapper.insertSelective(item);
        //获取插入后的id
        itemModel.setId(item.getId());
        ItemStock itemStock = convertItemStockFromItemModel(itemModel);
        itemStockMapper.insertSelective(itemStock);
        //返回对象
        return this.getItemById(itemModel.getId());
    }

    /**
     * 商品列表浏览
     * @return
     */
    @Override
    public List<ItemModel> listItem() {
        List<Item> items = itemMapper.listItem();
        List<ItemModel> itemModels = items.stream().map(item -> {
            ItemStock itemStock = itemStockMapper.selectByItemId(item.getId());
            ItemModel itemModel = convertModelFromDataObject(item, itemStock);
            return itemModel;
        }).collect(Collectors.toList());
        return itemModels;
    }

    /**
     * 商品详情
     * @param id
     * @return
     */
    @Override
    public ItemModel getItemById(Integer id) {
        Item item = itemMapper.selectByPrimaryKey(id);
        if(item == null){
            return null;
        }
        //操作获得库存数量
        ItemStock itemStock = itemStockMapper.selectByItemId(item.getId());

        //dataObj -- > model
        ItemModel itemModel = convertModelFromDataObject(item, itemStock);
        PromoModel promoModel = promoService.getPromoByItemId(itemModel.getId());
        if(promoModel != null && promoModel.getStatus().intValue() != 3){
            itemModel.setPromoModel(promoModel);
        }
        return itemModel;
    }

    @Override
    public ItemModel getItemByIdInCache(Integer id) {
        ItemModel itemModel = (ItemModel) redisTemplate.opsForValue().get("item_validate_" + id);
        if(itemModel == null){
            itemModel = this.getItemById(id);
            redisTemplate.opsForValue().set("item_validate_" + id, itemModel);
            redisTemplate.expire("item_validate_" + id, 10, TimeUnit.MINUTES);
        }
        return itemModel;
    }

    @Override
    public boolean asynDecreaseStock(Integer itemId, Integer amount) {
        boolean sendResult = mqProducer.asyncReduceStock(itemId, amount);
        return sendResult;
    }

    @Override
    @Transactional
    public boolean decreaseStock(Integer itemId, Integer amount) {
        //影响的条目数
        //对于sql是原子操作
        //int affectRow = itemStockMapper.decreaseStock(itemId, amount);

        //使用redis缓存
        long result = redisTemplate.opsForValue().increment("promo_item_stock_" + itemId, amount * -1);
        //return result >= 0 ? true : false;

        //更新redis成功
            if(result > 0){
                /**
            //使用mq消息队列 更新数据库库存，利用异步消息
            boolean sendResult = mqProducer.asyncReduceStock(itemId, amount);
            //消息发送不成功
            if(!sendResult){
                //redis回滚
                redisTemplate.opsForValue().increment("promo_item_stock_" + itemId, amount.intValue());
                return false;
            }
             */
            return true;
        }else if(result == 0){
            //售罄
            redisTemplate.opsForValue().set("promo_item_stock_invalid_" + itemId, "true");
            return true;
        }else{
        //更新redis失败,回滚
        increaseStock(itemId, amount);
        return false;

        }
    }

    @Override
    public boolean increaseStock(Integer itemId, Integer amount) {
        redisTemplate.opsForValue().increment("promo_item_stock_" + itemId, amount.intValue());
        return true;
    }

    @Override
    @Transactional
    public void increaseSales(Integer itemId, Integer amount) {
        itemMapper.increaseSales(itemId, amount);
    }


    /**
     * 库存售罄
     * @param itemId
     * @param amount
     * @return
     */
    //初始化对应的库存流水
    @Override
    @Transactional
    public String initStockLog(Integer itemId, Integer amount) {
        StockLog stockLog = new StockLog();
        stockLog.setItemId(itemId);
        stockLog.setAmount(amount);
        stockLog.setStockLogId(UUID.randomUUID().toString().replace("-", ""));
        stockLog.setStatus(1);//初始状态为1
        stockLogMapper.insertSelective(stockLog);
        return stockLog.getStockLogId();
    }

    private Item convertItemFromItemModel(ItemModel itemModel){
        if(itemModel == null){
            return null;
        }
        Item item = new Item();
        //UserModel中的price是BigDecimal类型而不用Double，Double在java内部传到前端，会有精度问题，不精确
        //有可能1.9，显示时是1.999999，为此在Service层，将price定为比较精确的BigDecimal类型
        //但是在拷贝到Dao层时，存入的是Double类型，拷贝方法对应类型不匹配的属性，不会进行拷贝。
        //拷贝完，将BigDecimal转为Double，再set进去
        BeanUtils.copyProperties(itemModel, item);
        item.setPrice(itemModel.getPrice().doubleValue());
        return item;
    }

    private ItemStock convertItemStockFromItemModel(ItemModel itemModel){
        if(itemModel == null){
            return null;
        }
        ItemStock itemStock = new ItemStock();

        itemStock.setItemId(itemModel.getId());
        itemStock.setStock(itemModel.getStock());
        return itemStock;
    }

    private ItemModel convertModelFromDataObject(Item item, ItemStock itemStock){
        ItemModel itemModel = new ItemModel();
        BeanUtils.copyProperties(item, itemModel);
        itemModel.setPrice(new BigDecimal(item.getPrice()));
        itemModel.setStock(itemStock.getStock());
        return itemModel;
    }
}
