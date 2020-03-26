package com.pu.service.model;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Description:
 * 1.@NotNull
 * 不能为null，但可以为empty (""," "," ")
 * 2.@NotEmpty
 * 不能为null，而且长度必须大于0 (" "," ") 只能使用在字符串上
 * 3.@NotBlank
 * 只能作用在String上，不能为null，而且调用trim()后，长度必须大于0 ("test") 即：必须有实际字符
 * Created By @Author my on @Date 2020/3/26 10:48
 */
public class ItemModel {

    private Integer id;
    //name
    @NotBlank(message = "商品名称不能为空")
    private String title;

    @NotNull(message = "商品价格不能为空")
    @Min(value = 0,message = "商品价格必须大于0")
    private BigDecimal price;

    //stock
    @NotNull(message = "库存不能不能不填")
    private Integer stock;
    //des
    @NotNull(message = "商品描述不能为空")
    private String description;

    //销量
    private Integer sales;

    //图片url
    @NotNull(message = "图片信息不能为空")
    private String imgUrl;


    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getSales() {
        return sales;
    }

    public void setSales(Integer sales) {
        this.sales = sales;
    }

    public String getImgUrl() {
        return imgUrl;
    }

    public void setImgUrl(String imgUrl) {
        this.imgUrl = imgUrl;
    }
}
