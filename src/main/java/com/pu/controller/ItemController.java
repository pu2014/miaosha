package com.pu.controller;

import com.pu.commom.ReturnType;
import com.pu.controller.view.ItemView;
import com.pu.domain.Item;
import com.pu.error.BusinessException;
import com.pu.service.IItemService;
import com.pu.service.impl.ItemServiceImpl;
import com.pu.service.model.ItemModel;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collector;
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

    @GetMapping(value = "/getitem")
    @ResponseBody
    public ReturnType getItem(@RequestParam(name="id")Integer id){
        return ReturnType.create(itemService.getItemById(id));
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
        System.out.println("dad???");
        ItemModel itemModel = new ItemModel();
        itemModel.setTitle(title);
        System.out.println("dad???");
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
        return itemView;
    }

}
