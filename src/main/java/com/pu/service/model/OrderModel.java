package com.pu.service.model;

import java.math.BigDecimal;

/**
 * Description:用 户下单的交易模型  订单
 * Created By @Author my on @Date 2020/3/26 22:12
 */
public class OrderModel {
    //交易号 2020...
    private String id;

    //单价
    private BigDecimal itemPrice;

    //用户id
    private Integer userId;

    //商品id
    private Integer itemId;

    //数量
    private Integer amount;

    //购买金额
    private BigDecimal orderPrice;

    public BigDecimal getItemPrice() {
        return itemPrice;
    }

    public void setItemPrice(BigDecimal itemPrice) {
        this.itemPrice = itemPrice;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getItemId() {
        return itemId;
    }

    public void setItemId(Integer itemId) {
        this.itemId = itemId;
    }

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    public BigDecimal getOrderAccount() {
        return orderPrice;
    }

    public void setOrderAccount(BigDecimal orderAccount) {
        this.orderPrice = orderAccount;
    }




}
