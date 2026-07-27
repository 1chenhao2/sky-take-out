package com.sky.service;

import com.sky.vo.BusinessDataVO;
import com.sky.vo.DishOverViewVO;
import com.sky.vo.OrderOverViewVO;
import com.sky.vo.SetmealOverViewVO;

import java.time.LocalDateTime;

public interface WorkSpaceService {
    /**
     * 根据时间段查询营业数据
     * @param begin
     * @param end
     * @return
     */
    BusinessDataVO getBusinessData(LocalDateTime begin, LocalDateTime end);


    /**
     * 查询订单管理数据
     * @return
     */
    OrderOverViewVO getOverviewOrders();

    /**
     * 菜品总览数据
     * @return
     */
    DishOverViewVO getOverviewDishes();

    /**
     * 套餐总览数据
     * @return
     */
    SetmealOverViewVO getOverviewSetmeals();
}
