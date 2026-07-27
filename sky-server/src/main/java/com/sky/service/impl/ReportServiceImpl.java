package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.ReportMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkSpaceService;
import com.sky.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.System.out;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {
    @Autowired
    private ReportMapper reportMapper;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WorkSpaceService workSpaceService;
    /**
     * 统计营业额数据
     * @param begin
     * @param end
     * @return
     */
    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        log.info("统计营业额数据：{}到{}",begin,end);
        //当前集合中存放从begin到end的每一天的日期
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)) {
            //日期计算，计算指定日期之后的一天
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        //创建一个list集合，用于存放从begin到end的每天对应的营业额
        List<Double> turnoverList = new ArrayList<>();
        for (LocalDate date : dateList){
            //获取指定日期的营业额数据,营业额数据:状态为“完成”的订单金额
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            //select sum(order_amount) from orders where order_time >= beginTime and order_time < endTime and status = 5
            Map map = new HashMap<>();
            map.put("begin", beginTime);
            map.put("end", endTime);
            map.put("status", 5);
            Double turnover = orderMapper.sumByMap(beginTime, endTime, 5);
            turnover = turnover == null ? 0.0 : turnover;
            turnoverList.add(turnover);
        }
        //组装返回结果
        return TurnoverReportVO
                .builder()
                .dateList(StringUtils.join(dateList, ","))
                .turnoverList(StringUtils.join(turnoverList, ","))
                .build();
     }
     /**
     * 统计用户数据
     * @param begin
     * @param end
     * @return
     */
     @Override
     public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        log.info("统计用户数据：{}到{}",begin,end);
        //当前集合中存放从begin到end的每一天的日期
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)) {
            //日期计算，计算指定日期之后的一天
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        //创建一个list集合，用于存放从begin到end的每天
         //存放新增用户数select count(id) from user where create_time >= beginTime and create_time < endTime
         List<Integer> newUserList = new ArrayList<>();
         //存放总用户数select count(id) from user where create_time < endTime
         List<Integer> totalUserList = new ArrayList<>();
         for (LocalDate date : dateList){
             LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
             LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

             Integer totalUser = userMapper.countUser(beginTime, endTime);
             totalUser = totalUser == null ? 0 : totalUser;
             totalUserList.add(totalUser);
             Integer newUser = userMapper.countUser(null, beginTime);
             newUser = newUser == null ? 0 : newUser;
             newUserList.add(newUser);
         }
         //组装返回结果
         return UserReportVO
                 .builder()
                 .dateList(StringUtils.join(dateList, ","))
                 .totalUserList(StringUtils.join(totalUserList, ","))
                 .newUserList(StringUtils.join(newUserList, ","))
                 .build();
     }
     /**
     * 统计订单数据
     * @param begin
     * @param end
     * @return
     */
     @Override
     public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
         log.info("统计订单数据：{}到{}",begin,end);
         //当前集合中存放从begin到end的每一天的日期
         List<LocalDate> dateList = new ArrayList<>();
         LocalDate currentDate = begin;
         dateList.add(currentDate);
         while (!currentDate.equals(end)) {
             //日期计算，计算指定日期之后的一天
             currentDate = currentDate.plusDays(1);
             dateList.add(currentDate);
         }
         //存放总订单数select count(id) from orders where order_time >= beginTime and order_time < endTime
         List<Integer> orderCountList = new ArrayList<>();
         //存放有效订单数select count(id) from orders where order_time >= beginTime and order_time < endTime and status = 5
         List<Integer> validOrderCountList = new ArrayList<>();
         for (LocalDate date : dateList){
             //获取指定日期的订单统计数据
             LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
             LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

             //获取指定日期的订单总数
             Integer orderCount = orderMapper.countOrder(beginTime, endTime, null);
             orderCount = orderCount == null ? 0 : orderCount;
             //添加进集合
             orderCountList.add(orderCount);

             Integer validOrderCount = orderMapper.countOrder(beginTime, endTime, 5);
             validOrderCount = validOrderCount == null ? 0 : validOrderCount;
             validOrderCountList.add(validOrderCount);
         }

         //计算总订单数
         Integer totalOrderCount = orderCountList.stream().mapToInt(Integer::intValue).sum();
         //计算有效订单数
         Integer validOrderCount = validOrderCountList.stream().mapToInt(Integer::intValue).sum();
         //计算订单完成率
         Double orderCompletionRate = totalOrderCount == 0 ? 0.0 : (validOrderCount * 1.0 / totalOrderCount);

         //组装返回结果
         return OrderReportVO
                 .builder()
                 .dateList(StringUtils.join(dateList, ","))
                 .orderCountList(StringUtils.join(orderCountList, ","))
                 .validOrderCountList(StringUtils.join(validOrderCountList, ","))
                 .totalOrderCount(totalOrderCount)
                 .validOrderCount(validOrderCount)
                 .orderCompletionRate(orderCompletionRate)
                 .build();
     }
     /**
     * 统计商品销量top10
     * @param begin
     * @param end
     * @return
     */
     @Override
      public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {
         log.info("统计商品销量top10：{}到{}",begin,end);
         //获取指定时间区间内的销量top10
         LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
         LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);
         //获取top10
         List<GoodsSalesDTO> salesTop10 = orderMapper.getSalesTop10(beginTime, endTime);
         //获取top10对应的商品名称和数量
         List<String> names = salesTop10.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
         String nameList = StringUtils.join(names, ",");
         List<Integer> numbers = salesTop10.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());
         String numberList = StringUtils.join(numbers, ",");
         return SalesTop10ReportVO
                 .builder()
                 .nameList(nameList)
                 .numberList(numberList)
                 .build();
     }
    /**
     * 导出营业数据
     * @param response
     */
    @Override
    public void exportBusinessData(HttpServletResponse response) {
        log.info("导出营业数据");
        //查询数据库中营业数据
        LocalDate begin = LocalDate.now().minusDays(30);
        LocalDate end = LocalDate.now().minusDays(1);
        //获取指定时间区间内的营业数据
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);
        //封装查询结果
        BusinessDataVO businessDataVO = workSpaceService.getBusinessData(beginTime, endTime);
        //通过POI 将数据写入到Excel文件中
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");
        try {
            //创建Excel表格对象
            XSSFWorkbook excel = new XSSFWorkbook(inputStream);
            //填充 数据
            XSSFSheet sheet = excel.getSheet("Sheet1");
            sheet.getRow(1).getCell(1).setCellValue(begin + "至" + end);
            sheet.getRow(3).getCell(2).setCellValue(businessDataVO.getTurnover());
            sheet.getRow(3).getCell(4).setCellValue(businessDataVO.getValidOrderCount());
            sheet.getRow(3).getCell(6).setCellValue(businessDataVO.getNewUsers());
            sheet.getRow(4).getCell(2).setCellValue(businessDataVO.getOrderCompletionRate());
            sheet.getRow(4).getCell(4).setCellValue(businessDataVO.getUnitPrice());
            //填充明细数据
            for (int i = 0; i < 30; i++) {
                //获取日期
                LocalDate date = begin.plusDays(i);
                //获取指定日期的营业数据
                BusinessDataVO dayBusinessData = workSpaceService.getBusinessData(LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));
                sheet.getRow(7 + i).getCell(1).setCellValue(date.toString());
                sheet.getRow(7 + i).getCell(2).setCellValue(dayBusinessData.getTurnover());
                sheet.getRow(7 + i).getCell(3).setCellValue(dayBusinessData.getValidOrderCount());
                sheet.getRow(7 + i).getCell(4).setCellValue(dayBusinessData.getOrderCompletionRate());
                sheet.getRow(7 + i).getCell(5).setCellValue(dayBusinessData.getUnitPrice());
                sheet.getRow(7 + i).getCell(6).setCellValue(dayBusinessData.getNewUsers());
            }

            //通过HttpServletResponse将Excel文件下载到客户端浏览器
            ServletOutputStream outputStream = response.getOutputStream();
            excel.write(outputStream);

            //关闭流
            out.close();
            excel.close();
        }catch (IOException e){
            e.printStackTrace();
        }


    }


}
