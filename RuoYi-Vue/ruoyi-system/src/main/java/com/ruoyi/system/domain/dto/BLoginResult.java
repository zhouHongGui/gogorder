package com.ruoyi.system.domain.dto;

import java.util.List;

/** 员工登录成功响应。 */
public class BLoginResult
{
    private String token;
    private BStaffProfileView userInfo;
    private List<BShopContextView> shops;
    private BShopContextView currentShop;

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public BStaffProfileView getUserInfo() { return userInfo; }
    public void setUserInfo(BStaffProfileView userInfo) { this.userInfo = userInfo; }
    public List<BShopContextView> getShops() { return shops; }
    public void setShops(List<BShopContextView> shops) { this.shops = shops; }
    public BShopContextView getCurrentShop() { return currentShop; }
    public void setCurrentShop(BShopContextView currentShop) { this.currentShop = currentShop; }
}
