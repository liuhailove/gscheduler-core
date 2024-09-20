
package com.tc.gschedulercore.controller;

import com.tc.gschedulercore.controller.auth.AuthAction;
import com.tc.gschedulercore.core.dto.ReturnT;
import com.tc.gschedulercore.core.model.NotifyInfo;
import com.tc.gschedulercore.service.ProtectService;
import com.tc.gschedulercore.util.DateUtil;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 防护事件Controller
 *
 * @author honggang.liu
 */
@Controller
@RequestMapping("/protect")
public class ProtectController {

    /**
     * 告警服务
     */
    @Resource
    private ProtectService protectService;


    /**
     * 移除通知事件
     *
     * @param id 事件ID
     * @return 影响行数
     */
    @AuthAction()
    @GetMapping("/remove")
    @ResponseBody
    public ReturnT<Integer> remove(Long id) {
        return ReturnT.ofSuccess(protectService.remove(id));
    }


    /**
     * 分页查找
     *
     * @param page           偏移量
     * @param limit          分页大小
     * @param app            应用名称
     * @param alarmType      告警类别
     * @param alarmName      告警名称
     * @param alarmLevel     告警级别
     * @param permissionApps 授权应用
     * @return 记录数
     */
    @AuthAction()
    @GetMapping("/events")
    @ResponseBody
    public Map<String, Object> events(@RequestParam(required = false, defaultValue = "1") Integer page,
                                      @RequestParam(required = false, defaultValue = "10") Integer limit,
                                      @RequestParam(required = false) String app,
                                      @RequestParam(required = false) String alarmName,
                                      @RequestParam(required = false) Integer alarmType,
                                      @RequestParam(required = false) Integer alarmLevel,
                                      @RequestParam(required = false, defaultValue = "0") String filterTime,
                                      @RequestParam(required = false) List<String> permissionApps) {
        // parse param
        Date alarmStartTime = null;
        Date alarmEndTime = null;
        if (filterTime != null && filterTime.trim().length() > 0) {
            String[] temp = filterTime.split(" - ");
            if (temp.length == 2) {
                alarmStartTime = DateUtil.parseDateTime(temp[0]);
                alarmEndTime = DateUtil.parseDateTime(temp[1]);
            }
        }
        List<NotifyInfo> list = protectService.pageList((page - 1) * limit, limit, app, alarmName, alarmType, alarmLevel, alarmStartTime, alarmEndTime, permissionApps);
        int listCount = protectService.pageListCount((page - 1) * limit, limit, app, alarmName, alarmType, alarmLevel, alarmStartTime, alarmEndTime, permissionApps);
        return ReturnT.ofMap(list, listCount);
    }

    /**
     * 告警ID
     *
     * @param id 主键ID
     * @return 告警明细
     */
    @AuthAction()
    @GetMapping("/load")
    @ResponseBody
    public ReturnT<NotifyInfo> load(Long id) {
        return ReturnT.ofSuccess(protectService.load(id));
    }


    /************* list *******************/
    @GetMapping("/list")
    public String eventList(@RequestParam(required = false) String app, RedirectAttributes attributes) {
        if (!StringUtils.isEmpty(app)) {
            attributes.addAttribute("app", app);
        }
        return "protect/event_list";
    }


}
