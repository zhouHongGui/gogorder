package com.ruoyi.system.mapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.BizOrder;
import com.ruoyi.system.domain.dto.AdminOrderDetailView;
import com.ruoyi.system.domain.dto.AdminOrderListItemView;
import com.ruoyi.system.domain.dto.AdminOrderQuery;
import com.ruoyi.system.domain.dto.BOrderListItemView;
import com.ruoyi.system.domain.dto.BOrderQuery;

public interface BizOrderMapper
{
    int insertOrder(BizOrder order);
    BizOrder selectById(Long id);
    BizOrder selectByIdForUpdate(Long id);
    BizOrder selectBySubmitKey(@Param("userId") Long userId, @Param("submitKey") String submitKey);
    BizOrder selectBySubmitKeyForUpdate(@Param("userId") Long userId, @Param("submitKey") String submitKey);
    BizOrder selectByOrderNo(String orderNo);
    int countPendingOrdersByUserId(Long userId);
    List<BizOrder> selectUserOrderList(@Param("userId") Long userId,
            @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime,
            @Param("offset") int offset, @Param("limit") int limit);
    long countUserOrders(@Param("userId") Long userId,
            @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    Integer countQueueAheadOrders(@Param("orderId") Long orderId);
    Integer countQueueAheadCups(@Param("orderId") Long orderId);
    int updateOrderStatus(@Param("id") Long id, @Param("orderStatus") Integer orderStatus,
            @Param("payStatus") Integer payStatus, @Param("refundStatus") Integer refundStatus,
            @Param("payTime") LocalDateTime payTime, @Param("acceptTime") LocalDateTime acceptTime,
            @Param("pickupToken") String pickupToken, @Param("pickupDisplay") String pickupDisplay,
            @Param("pickupDate") LocalDate pickupDate);
    int updateOrderForRefund(@Param("id") Long id, @Param("orderStatus") Integer orderStatus,
            @Param("payStatus") Integer payStatus, @Param("refundStatus") Integer refundStatus,
            @Param("cancelTime") LocalDateTime cancelTime, @Param("cancelReason") String cancelReason);
    int cancelOrder(@Param("id") Long id, @Param("orderStatus") Integer orderStatus,
            @Param("payStatus") Integer payStatus, @Param("cancelTime") LocalDateTime cancelTime,
            @Param("cancelReason") String cancelReason);
    List<BizOrder> selectBBoardOrders(@Param("shopId") Long shopId, @Param("limit") int limit);
    BizOrder selectByScanOutCodeForUpdate(@Param("shopId") Long shopId, @Param("code") String code);
    int updateStartMake(@Param("id") Long id, @Param("shopId") Long shopId, @Param("makeStartTime") LocalDateTime makeStartTime);
    int updateScanOut(@Param("id") Long id, @Param("shopId") Long shopId, @Param("scanOutTime") LocalDateTime scanOutTime);
    int updateCompleteOrder(@Param("id") Long id, @Param("shopId") Long shopId, @Param("completeTime") LocalDateTime completeTime);
    int updateDailyFinalizeOrder(@Param("id") Long id, @Param("completeTime") LocalDateTime completeTime);
    List<BizOrder> selectPendingTimeoutOrders(@Param("deadline") LocalDateTime deadline,
            @Param("minId") Long minId, @Param("limit") int limit);
    List<BizOrder> selectReadyTimeoutOrders(@Param("deadline") LocalDateTime deadline,
            @Param("minId") Long minId, @Param("limit") int limit);
    List<BizOrder> selectDailyFinalizeOrders(@Param("cutoffPickupDate") LocalDate cutoffPickupDate,
            @Param("minId") Long minId, @Param("limit") int limit);
    int allocatePickupDisplay(@Param("shopId") Long shopId, @Param("pickupDate") LocalDate pickupDate);
    int selectPickupSeq(@Param("shopId") Long shopId, @Param("pickupDate") LocalDate pickupDate);
    List<AdminOrderListItemView> selectAdminOrderList(AdminOrderQuery query);
    AdminOrderDetailView selectAdminOrderDetail(@Param("orderId") Long orderId);
    List<BOrderListItemView> selectBOrderList(BOrderQuery query);

    // ===== 制作调度（串行队列，见 ProductionScheduleService / M15 §6.4）=====
    /** 制作中订单数（串行模式下应最多 1）。 */
    int countMakingOrders(@Param("shopId") Long shopId);
    /** 队列中最早可制作的订单（order_status=1，预订单需已到制作窗口），FOR UPDATE。 */
    BizOrder selectNextQueuedForUpdate(@Param("shopId") Long shopId, @Param("makeLeadMinutes") int makeLeadMinutes);
    /** 本单总杯数。 */
    int sumOrderCups(@Param("orderId") Long orderId);
    /** 写预计取餐时间。 */
    int updateEstimatedReadyTime(@Param("id") Long id, @Param("estimatedReadyTime") LocalDateTime estimatedReadyTime);
    /** 有队列订单（order_status=1）的门店列表，供定时任务扫描兜底。 */
    List<Long> selectShopIdsWithQueue();
}
