package com.ruoyi.system.mapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.BizOrder;

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
    BizOrder selectByPickupTokenForUpdate(@Param("pickupToken") String pickupToken);
    int updateStartMake(@Param("id") Long id, @Param("shopId") Long shopId, @Param("makeStartTime") LocalDateTime makeStartTime);
    int updateCompleteMake(@Param("id") Long id, @Param("shopId") Long shopId, @Param("completeMakeTime") LocalDateTime completeMakeTime);
    int updateVerify(@Param("id") Long id, @Param("shopId") Long shopId, @Param("verifyTime") LocalDateTime verifyTime);
    List<BizOrder> selectPendingTimeoutOrders(@Param("deadline") LocalDateTime deadline,
            @Param("minId") Long minId, @Param("limit") int limit);
    int allocatePickupDisplay(@Param("shopId") Long shopId, @Param("pickupDate") LocalDate pickupDate);
    int selectPickupSeq(@Param("shopId") Long shopId, @Param("pickupDate") LocalDate pickupDate);
}
