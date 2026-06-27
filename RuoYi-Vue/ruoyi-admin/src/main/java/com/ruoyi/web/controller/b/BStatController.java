package com.ruoyi.web.controller.b;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.system.service.IBStatService;

@RestController
@RequestMapping("/api/b/stat")
public class BStatController
{
    @Autowired
    private IBStatService bStatService;

    @GetMapping("/overview")
    public AjaxResult overview(@RequestHeader("X-Shop-Id") Long shopId,
            @RequestParam(value = "range", required = false) String range)
    {
        return AjaxResult.success(bStatService.getOverview(shopId, range));
    }
}
