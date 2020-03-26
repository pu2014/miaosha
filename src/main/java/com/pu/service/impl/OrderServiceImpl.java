package com.pu.service.impl;

import com.pu.dao.OrderMapper;
import com.pu.dao.SequenceMapper;
import com.pu.domain.Item;
import com.pu.domain.Order;
import com.pu.domain.Sequence;
import com.pu.error.BusinessException;
import com.pu.error.EmBusinessError;
import com.pu.service.IItemService;
import com.pu.service.IOrderService;
import com.pu.service.IUserService;
import com.pu.service.model.ItemModel;
import com.pu.service.model.OrderModel;
import com.pu.service.model.UserModel;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.SQLOutput;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Description:
 * Created By @Author my on @Date 2020/3/26 22:27
 */
@Service
public class OrderServiceImpl implements IOrderService {

    @Autowired
    private IItemService itemService;

    @Autowired
    private SequenceMapper sequenceMapper;

    @Autowired
    private IUserService userService;

    @Autowired
    private OrderMapper orderMapper;

    @Override
    @Transactional
    public OrderModel createOrder(Integer userId, Integer itemId, Integer amount) throws BusinessException {
        //校验下单状态，用户是否合法，购买数量是否正确
        ItemModel itemModel = itemService.getItemById(itemId);
        if(itemModel == null){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"商品信息不存在");
        }

        UserModel userModel = userService.getUserById(userId);
        if(userModel == null){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "用户信息异常");
        }

        if(amount <= 0 || amount > 99){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "至少下单数为1，至多下单100");
        }

        //落单减库存 锁单 （另外还有支付减库存）
        boolean result = itemService.decreaseStock(itemId, amount);
        if(!result){
            throw new BusinessException(EmBusinessError.STOCK_NOT_ENOUGH,"商品库存不足");
        }
        //订单入库
        OrderModel orderModel = new OrderModel();
        orderModel.setUserId(userId);
        orderModel.setItemId(itemId);
        orderModel.setAmount(amount);
        orderModel.setItemPrice(itemModel.getPrice());
        orderModel.setOrderAccount(itemModel.getPrice().multiply(new BigDecimal(amount)));

        //生成交易流水号
        orderModel.setId(generateOrderNo());
        Order order = convertFromOrderModel(orderModel);
        orderMapper.insertSelective(order);

        //销量变化
        itemService.increaseSales(itemId,amount);
        return orderModel;
    }

    private Order convertFromOrderModel(OrderModel orderModel){
        if(orderModel == null){
            return null;
        }
        Order order = new Order();
        BeanUtils.copyProperties(orderModel, order);
        order.setItemPrice(orderModel.getItemPrice().doubleValue());
        order.setOrderPrice(orderModel.getOrderAccount().doubleValue());
        return order;
    }

    /**
     * 生成16位订单号 前八位事件信息 年月日
     * 中间6位为自增加序列  后两位为分库分表位（00 - 99） %100 表的压力
     *
     * propagation = Propagation.REQUIRES_NEW
     * 执行完这段代码块，外部的事务成功与否，对于的事务都提交，对于的sequence都被使用掉
     * @return
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    String generateOrderNo(){
        StringBuilder sb = new StringBuilder();
        LocalDateTime now = LocalDateTime.now();
        String nowDate = now.format(DateTimeFormatter.ISO_DATE).replace("-", "");
        sb.append(nowDate);

        //获取当前seq
        int seq = 0;
        Sequence sequence = sequenceMapper.getSeqByName("order_info");
        seq = sequence.getCurrentValue();
        sequence.setCurrentValue(seq + sequence.getStep());
        sequenceMapper.updateByPrimaryKey(sequence);

        //拼接
        String seqStr = String.valueOf(seq);
        //不足0 补零
        for(int i = 0; i < 6 - seqStr.length(); i++){
            sb.append("0");
        }
        sb.append(seqStr);
        sb.append("00");
        return sb.toString();
    }
}
