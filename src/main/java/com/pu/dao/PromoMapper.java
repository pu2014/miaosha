package com.pu.dao;

import com.pu.domain.Promo;

public interface PromoMapper {
    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table promo
     *
     * @mbg.generated Fri Mar 27 10:50:01 CST 2020
     */
    int deleteByPrimaryKey(Integer id);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table promo
     *
     * @mbg.generated Fri Mar 27 10:50:01 CST 2020
     */
    int insert(Promo record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table promo
     *
     * @mbg.generated Fri Mar 27 10:50:01 CST 2020
     */
    int insertSelective(Promo record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table promo
     *
     * @mbg.generated Fri Mar 27 10:50:01 CST 2020
     */
    Promo selectByPrimaryKey(Integer id);

    Promo selectByItemId(Integer itemId);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table promo
     *
     * @mbg.generated Fri Mar 27 10:50:01 CST 2020
     */
    int updateByPrimaryKeySelective(Promo record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table promo
     *
     * @mbg.generated Fri Mar 27 10:50:01 CST 2020
     */
    int updateByPrimaryKey(Promo record);
}