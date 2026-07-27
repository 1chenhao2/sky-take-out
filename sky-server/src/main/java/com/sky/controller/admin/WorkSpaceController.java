package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.service.WorkSpaceService;
import com.sky.vo.BusinessDataVO;
import com.sky.vo.DishOverViewVO;
import com.sky.vo.OrderOverViewVO;
import com.sky.vo.SetmealOverViewVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.LocalTime;

@RestController
@RequestMapping("/admin/workspace")
@Slf4j
public class WorkSpaceController {
    @Autowired
    private WorkSpaceService workSpaceService;
    /**
     * 工作台今日数据查询
     */
    @GetMapping("/businessData")
    public Result<BusinessDataVO> getBusinessData(){
        log.info("获取工作台数据");
        //获得当天的开始时间
        LocalDateTime begin = LocalDateTime.now().with(LocalTime.MIN);
        //获得当天的结束时间
        LocalDateTime end = LocalDateTime.now().with(LocalTime.MAX);
        BusinessDataVO businessDataVO =workSpaceService.getBusinessData(begin, end);
        return Result.success(businessDataVO);
    }
    /**
     * 查询订单管理数据
     */
    @GetMapping("/overviewOrders")
    public Result<OrderOverViewVO> getOverviewOrders(){
        log.info("查询订单管理数据");
        return Result.success(workSpaceService.getOverviewOrders());
    }
    /**
     * 查询菜品总览
     */
    @GetMapping("/overviewDishes")
    public Result<DishOverViewVO> overviewDishes(){
        log.info("查询菜品总览");
        return Result.success(workSpaceService.getOverviewDishes());
    }
    /**
     * 查询套餐总览
     */
    @GetMapping("/overviewSetmeals")
    public Result<SetmealOverViewVO> overviewSetmeals(){
        log.info("查询套餐总览");
        return Result.success(workSpaceService.getOverviewSetmeals());
    }


}
