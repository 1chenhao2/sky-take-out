package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.mapper.DishMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.WorkSpaceService;
import com.sky.vo.BusinessDataVO;
import com.sky.vo.DishOverViewVO;
import com.sky.vo.OrderOverViewVO;
import com.sky.vo.SetmealOverViewVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@Slf4j
public class WorkSpaceServiceImpl implements WorkSpaceService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;

    /**
     * 工作台今日数据查询
     */
    @Override
    public BusinessDataVO getBusinessData(LocalDateTime begin, LocalDateTime end) {
        log.info("获取工作台数据");

        // 查询指定时间范围内的营业额（状态为5表示已完成订单）
        Double turnover = orderMapper.sumByMap(begin, end, 5);
        turnover = turnover == null ? 0.0 : turnover;

        // 查询指定时间范围内的有效订单数（状态为5的订单）
        Integer validOrderCount = orderMapper.countOrder(begin, end, 5);
        validOrderCount = validOrderCount == null ? 0 : validOrderCount;

        // 查询指定时间范围内的总订单数
        Integer totalOrderCount = orderMapper.countOrder(begin, end, null);
        totalOrderCount = totalOrderCount == null ? 0 : totalOrderCount;

        // 计算订单完成率，避免除以零
        Double orderCompletionRate = totalOrderCount == 0 ? 0.0 : (validOrderCount * 1.0 / totalOrderCount);

        // 计算平均客单价，避免除以零
        Double unitPrice = validOrderCount == 0 ? 0.0 : (turnover / validOrderCount);

        // 查询指定时间范围内的新增用户数
        Integer newUsers = userMapper.countUser(begin, end);
        newUsers = newUsers == null ? 0 : newUsers;

        // 构建并返回业务数据视图对象
        return BusinessDataVO.builder()
                .turnover(turnover)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .unitPrice(unitPrice)
                .newUsers(newUsers)
                .build();
    }
    /**
     * 订单管理数据
     */
    @Override
    public OrderOverViewVO getOverviewOrders() {
        log.info("查询订单管理数据");

        // 统计各状态订单数量，处理可能的空值
        Integer waitingOrders = orderMapper.countStatus(Orders.TO_BE_CONFIRMED);//待接单
        Integer deliveredOrders = orderMapper.countStatus(Orders.DELIVERY_IN_PROGRESS);//派送中
        Integer completedOrders = orderMapper.countStatus(Orders.COMPLETED);//已完成
        Integer cancelledOrders = orderMapper.countStatus(Orders.CANCELLED);//已取消
        Integer allOrders = orderMapper.countStatus(null);//所有订单

        return OrderOverViewVO.builder()
                .waitingOrders(waitingOrders != null ? waitingOrders : 0)
                .deliveredOrders(deliveredOrders != null ? deliveredOrders : 0)
                .completedOrders(completedOrders != null ? completedOrders : 0)
                .cancelledOrders(cancelledOrders != null ? cancelledOrders : 0)
                .allOrders(allOrders != null ? allOrders : 0)
                .build();
    }
    /**
     * 查询菜品总览
     */
    @Override
    public DishOverViewVO getOverviewDishes() {
        log.info("查询菜品总览");

        // 查询已启售的菜品数量（status = 1）
        Integer sold = dishMapper.countByStatus(1);
        sold = sold != null ? sold : 0;

        // 查询已停售的菜品数量（status = 0）
        Integer discontinued = dishMapper.countByStatus(0);
        discontinued = discontinued != null ? discontinued : 0;

        return DishOverViewVO.builder()
                .sold(sold)
                .discontinued(discontinued)
                .build();
    }
    /**
     * 查询套餐总览
     */
    @Override
    public SetmealOverViewVO getOverviewSetmeals() {
        log.info("查询套餐总览");
        // 查询已启售的套餐数量（status = 1）
        Integer sold = setmealMapper.countByStatus(1);
        sold = sold != null ? sold : 0;
        // 查询已停售的套餐数量（status = 0）
        Integer discontinued = setmealMapper.countByStatus(0);
        discontinued = discontinued != null ? discontinued : 0;
        return SetmealOverViewVO.builder()
                .sold(sold)
                .discontinued(discontinued)
                .build();

    }
}
