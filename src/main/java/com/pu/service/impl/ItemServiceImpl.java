package com.pu.service.impl;

import com.pu.dao.ItemMapper;
import com.pu.dao.ItemStockMapper;
import com.pu.domain.Item;
import com.pu.domain.ItemStock;
import com.pu.error.BusinessException;
import com.pu.error.EmBusinessError;
import com.pu.service.IItemService;
import com.pu.service.model.ItemModel;
import com.pu.validator.ValidationResult;
import com.pu.validator.ValidatorImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
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
        return convertModelFromDataObject(item, itemStock);
    }

    @Override
    @Transactional
    public boolean decreaseStock(Integer itemId, Integer amount) {
        //影响的条目数
        //对于sql是原子操作
        int affectRow = itemStockMapper.decreaseStock(itemId, amount);
        return affectRow != 0 ? true : false;
    }

    @Override
    @Transactional
    public void increaseSales(Integer itemId, Integer amount) {
        itemMapper.increaseSales(itemId, amount);
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