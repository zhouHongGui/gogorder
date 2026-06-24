package com.ruoyi.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.ShopStaff;

/** 独立门店员工账号 Mapper。 */
public interface ShopStaffMapper
{
    List<ShopStaff> selectList(ShopStaff condition);
    ShopStaff selectById(Long id);
    ShopStaff selectByAccount(String account);
    ShopStaff selectByPhone(String phone);
    int insert(ShopStaff staff);
    int update(ShopStaff staff);
    int updateLoginInfo(@Param("id") Long id, @Param("ip") String ip);
    int deleteById(Long id);
}
