package com.pu.controller;

import com.pu.response.ReturnType;
import com.pu.controller.view.ItemView;
import com.pu.error.BusinessException;
import com.pu.service.ICacheService;
import com.pu.service.IItemService;
import com.pu.service.IPromoService;
import com.pu.service.model.ItemModel;
import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Description:
 * Created By @Author my on @Date 2020/3/26 12:13
 */
@Controller("item")
@RequestMapping("/item")
@CrossOrigin(allowCredentials = "true", allowedHeaders = "*")
public class ItemController extends baseController {

    @Autowired
    private IItemService itemService;

    @Autowired
    @Qualifier("redisTemplate")
    private RedisTemplate redisTemplate;

    @Autowired
    private ICacheService cacheService;

    @Autowired
    private IPromoService promoService;


    @GetMapping(value = "/listitem")
    @ResponseBody
    public ReturnType listItem() {
        List<ItemModel> itemModels = itemService.listItem();
        List<ItemView> itemViews = itemModels.stream().map(itemModel -> {
            ItemView itemView = convertViewFromModel(itemModel);
            return itemView;
        }).collect(Collectors.toList());
        return ReturnType.create(itemViews);
    }

    /**
     * 多级缓存-热点缓存
     * @param id
     * @return
     */
    @GetMapping(value = "/getitem")
    @ResponseBody
    public ReturnType getItem(@RequestParam(name="id")Integer id){
        ItemModel itemModel = null;
        //先去本地缓存
        itemModel = (ItemModel) cacheService.getFromCommonCache("item_" + id);

        if(itemModel == null) {
            //根据商品的id去redis获取
            itemModel = (ItemModel) redisTemplate.opsForValue().get("item_" + id);

            //不存在就访问下游service
            if (itemModel == null) {
                itemModel = itemService.getItemById(id);
                //存储到redis
                redisTemplate.opsForValue().set("item_" + id, itemModel);
                redisTemplate.expire("item_" + id, 10, TimeUnit.MINUTES);
            }
            cacheService.setCommonCache("item_" + id, itemModel);
        }
        return ReturnType.create(convertViewFromModel(itemModel));
    }

    /**
     * 发布活动 一般运营后台执行
     * @param id
     * @return
     */
    @GetMapping(value = "/publishpromo")
    @ResponseBody
    public ReturnType publishPromo(@RequestParam(name="id")Integer id){
        promoService.PublishPromo(id);
        return ReturnType.create("活动发布成功");
    }


    @RequestMapping(value = "/create", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public ReturnType createItem(@RequestParam(name="title")String title,
                                 @RequestParam(name="description")String description,
                                 @RequestParam(name="price") BigDecimal price,
                                 @RequestParam(name="stock")Integer stock,
                                 @RequestParam(name="imgUrl")String imgUrl) throws BusinessException {
        //封装service请求用来创建商品
        //尽量让Controller层简单，让Service层负责，把服务逻辑尽可能聚合在Service层内部，实现流转处理
        //创建给service层的
        ItemModel itemModel = new ItemModel();
        itemModel.setTitle(title);
        itemModel.setDescription(description);
        itemModel.setPrice(price);
        itemModel.setStock(stock);
        itemModel.setImgUrl(imgUrl);

        ItemModel itemModelForReturn = itemService.createItem(itemModel);
        ItemView itemView = convertViewFromModel(itemModelForReturn);
        return ReturnType.create(itemView);
    }

    private ItemView convertViewFromModel(ItemModel itemModel){
        if(itemModel == null){
            return null;
        }
        ItemView itemView = new ItemView();
        BeanUtils.copyProperties(itemModel, itemView);
        if(itemModel.getPromoModel() != null){
            itemView.setPromoStatus(itemModel.getPromoModel().getStatus());
            itemView.setPromoId(itemModel.getPromoModel().getId());
            itemView.setStartDate(itemModel.getPromoModel().getStartDate().toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")));
            itemView.setPromoPrice(itemModel.getPromoModel().getPromoItemPrice());
        }else{
            itemView.setPromoStatus(0);
        }
        return itemView;
    }

}
