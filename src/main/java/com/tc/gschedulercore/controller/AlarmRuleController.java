package com.tc.gschedulercore.controller;

import com.tc.gschedulercore.controller.auth.AuthAction;
import com.tc.gschedulercore.core.dto.ReturnT;
import com.tc.gschedulercore.core.model.AlarmItem;
import com.tc.gschedulercore.core.model.AlarmRule;
import com.tc.gschedulercore.core.model.AlarmScript;
import com.tc.gschedulercore.service.AlarmRuleService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

/**
 * 告警规则Controller
 *
 * @author honggang.liu
 */
@Controller
@RequestMapping("/alarm")
public class AlarmRuleController {

    /**
     * 告警服务
     */
    @Resource
    private AlarmRuleService alarmRuleService;

    /**
     * 规则保存
     *
     * @param alarmRule 告警规则
     * @return 影响行数
     */
    @AuthAction()
    @PostMapping("/add")
    @ResponseBody
    public ReturnT<Integer> add(@RequestBody AlarmRule alarmRule) {
        return alarmRuleService.add(alarmRule);
    }

    /**
     * 规则更新
     *
     * @param alarmRule 告警规则
     * @return 影响行数
     */
    @AuthAction()
    @PostMapping("/update")
    @ResponseBody
    public ReturnT<Integer> update(@RequestBody AlarmRule alarmRule) {
        return alarmRuleService.update(alarmRule);
    }

    /**
     * 移除规则
     *
     * @param id 规则ID
     * @return 影响行数
     */
    @AuthAction()
    @GetMapping("/remove")
    @ResponseBody
    public ReturnT<Integer> remove(Long id) {
        return alarmRuleService.remove(id);
    }

    /**
     * 移除规则
     *
     * @param id 规则ID
     * @return 影响行数
     */
    @AuthAction()
    @GetMapping("/removeItem")
    @ResponseBody
    public ReturnT<Integer> removeItem(Long id) {
        return alarmRuleService.removeItem(id);
    }

    /**
     * 分页查找
     *
     * @param page       偏移量
     * @param limit      分页大小
     * @param alarmName  告警名称
     * @param alarmLevel 告警级别
     * @param jobGroups  授权应用
     * @return 记录数
     */
    @AuthAction()
    @GetMapping("/rules")
    @ResponseBody
    public Map<String, Object> queryRules(@RequestParam(required = false, defaultValue = "1") Integer page,
                                          @RequestParam(required = false, defaultValue = "10") Integer limit,
                                          @RequestParam(required = false) Integer resourceType,
                                          @RequestParam(required = false) Integer alarmLevel,
                                          @RequestParam(required = false) String alarmName,
                                          @RequestParam(required = false) List<Integer> jobGroups) {
        List<AlarmRule> list = alarmRuleService.pageList((page - 1) * limit, limit, resourceType, alarmLevel, alarmName, jobGroups);
        int listCount = alarmRuleService.pageListCount((page - 1) * limit, limit, resourceType, alarmLevel, alarmName, jobGroups);
        return ReturnT.ofMap(list, listCount);
    }

    /**
     * 分页查找Itms
     *
     * @param page        偏移量
     * @param limit       分页大小
     * @param alarmRuleId 告警规则ID
     * @return 记录数
     */
    @AuthAction()
    @GetMapping("/items")
    @ResponseBody
    public Map<String, Object> queryItems(@RequestParam(required = false, defaultValue = "1") Integer page,
                                          @RequestParam(required = false, defaultValue = "10") Integer limit,
                                          @RequestParam(required = false) Long alarmRuleId) {
        List<AlarmItem> list = alarmRuleService.itemsPageList((page - 1) * limit, limit, alarmRuleId);
        int listCount = alarmRuleService.itemsPageListCount((page - 1) * limit, limit, alarmRuleId);
        return ReturnT.ofMap(list, listCount);
    }


    /**
     * 规则ID
     *
     * @param id 主键ID
     * @return 告警规则
     */
    @AuthAction()
    @GetMapping("/load")
    @ResponseBody
    public ReturnT<AlarmRule> load(Long id) {
        return ReturnT.ofSuccess(alarmRuleService.load(id));
    }


    /**
     * 规则保存
     *
     * @param alarmItem 告警规则明细
     * @return 影响行数
     */
    @AuthAction()
    @PostMapping("/addItem")
    @ResponseBody
    public ReturnT<Integer> addItem(@RequestBody AlarmItem alarmItem) {
        return alarmRuleService.addItem(alarmItem);
    }

    /**
     * 规则更新
     *
     * @param alarmItem 告警规则明细
     * @return 影响行数
     */
    @AuthAction()
    @PostMapping("/updateItem")
    @ResponseBody
    public ReturnT<Integer> updateItem(@RequestBody AlarmItem alarmItem) {
        return alarmRuleService.updateItem(alarmItem);
    }

    /**
     * 规则项ID
     *
     * @param id 主键ID
     * @return 告警规则
     */
    @AuthAction()
    @GetMapping("/loadItem")
    @ResponseBody
    public ReturnT<AlarmItem> loadItem(Long id) {
        return ReturnT.ofSuccess(alarmRuleService.loadItem(id));
    }

    /**
     * 创建关联
     *
     * @param ruleId  规则ID
     * @param jobIdes 任务集合
     * @return 告警规则
     */
    @AuthAction()
    @GetMapping("/createRelation")
    @ResponseBody
    public ReturnT<Integer> createRelation(@RequestParam("ruleId") Long ruleId, @RequestParam(value = "jobIdes",required = false) Integer[] jobIdes) throws ParseException {
        return alarmRuleService.createRelation(ruleId, jobIdes);
    }

    /**
     * 脚本加载
     *
     * @param alarmScriptId 告警监控脚本Id
     * @return 影响行数
     */
    @AuthAction()
    @GetMapping("/loadScript")
    @ResponseBody
    public ReturnT<AlarmScript> loadScript(@RequestParam Long alarmScriptId) {
        return ReturnT.ofSuccess(alarmRuleService.loadScript(alarmScriptId));
    }

    /**
     * 规则保存
     *
     * @param alarmScript 告警监控脚本
     * @return 影响行数
     */
    @AuthAction()
    @PostMapping("/addScript")
    @ResponseBody
    public ReturnT<Integer> addScript(@RequestBody AlarmScript alarmScript) throws ParseException {
        return alarmRuleService.addScript(alarmScript);
    }

    /**
     * 规则更新
     *
     * @param alarmScript 告警监控脚本
     * @return 影响行数
     */
    @AuthAction()
    @PostMapping("/updateScript")
    @ResponseBody
    public ReturnT<Integer> updateScript(@RequestBody AlarmScript alarmScript) throws ParseException {
        return alarmRuleService.updateScript(alarmScript);
    }


    /**
     * 移除脚本规则
     *
     * @param alarmScriptId 规则ID
     * @return 影响行数
     */
    @AuthAction()
    @GetMapping("/removeScript")
    @ResponseBody
    public ReturnT<Integer> removeScript(@RequestParam("alarmScriptId") Long alarmScriptId) {
        return alarmRuleService.removeScript(alarmScriptId);
    }

    /**
     * 分页查找Scripts
     *
     * @param page        偏移量
     * @param limit       分页大小
     * @param alarmRuleId 告警规则ID
     * @return 记录数
     */
    @AuthAction()
    @GetMapping("/queryScripts")
    @ResponseBody
    public Map<String, Object> queryScripts(@RequestParam(required = false, defaultValue = "1") Integer page,
                                            @RequestParam(required = false, defaultValue = "10") Integer limit,
                                            @RequestParam(required = false) Long alarmRuleId) {
        List<AlarmScript> list = alarmRuleService.scriptsPageList((page - 1) * limit, limit, alarmRuleId);
        int listCount = alarmRuleService.scriptsPageListCount((page - 1) * limit, limit, alarmRuleId);
        return ReturnT.ofMap(list, listCount);
    }


    /************* list *******************/
    @GetMapping("/list")
    public String rulesList(RedirectAttributes attributes) {
        return "alarm/rules_list";
    }

    @GetMapping("/add")
    public String addAlarm(RedirectAttributes attributes) {
        return "alarm/add";
    }

    @GetMapping("/edit")
    public String edit(@RequestParam Long id, @RequestParam Integer resourceType, RedirectAttributes attributes) {
        attributes.addAttribute("id", id);
        attributes.addAttribute("resourceType", resourceType);
        return "alarm/edit";
    }

    @GetMapping("/relatedResource")
    public String relatedResource(@RequestParam Long id, @RequestParam Integer jobGroupId, @RequestParam String jobIdes, RedirectAttributes attributes) {
        attributes.addAttribute("id", id);
        attributes.addAttribute("jobGroupId", jobGroupId);
        attributes.addAttribute("jobIdes", jobIdes);
        return "alarm/related_resource_list";
    }

    @GetMapping("/alarmItem")
    public String alarmItem(@RequestParam Long id, @RequestParam Integer resourceType, RedirectAttributes attributes) {
        attributes.addAttribute("id", id);
        attributes.addAttribute("resourceType", resourceType);
        return "alarm/alarm_item_list";
    }

    @GetMapping("/alarmItemAdd")
    public String alarmItemAdd(@RequestParam Long alarmRuleId, @RequestParam Integer resourceType, RedirectAttributes attributes) {
        attributes.addAttribute("alarmRuleId", alarmRuleId);
        attributes.addAttribute("resourceType", resourceType);
        return "alarm/alarm_item_add";
    }

    @GetMapping("/alarmItemEdit")
    public String alarmItemEdit(@RequestParam Long alarmItemId, @RequestParam Integer resourceType, RedirectAttributes attributes) {
        attributes.addAttribute("alarmItemId", alarmItemId);
        attributes.addAttribute("resourceType", resourceType);
        return "alarm/alarm_item_edit";
    }

    @GetMapping("/alarmScriptList")
    public String alarmScriptList(@RequestParam Long id, RedirectAttributes attributes) {
        attributes.addAttribute("id", id);
        return "alarm/alarm_script_list";
    }

    @GetMapping("/alarmScriptAdd")
    public String alarmScriptAdd(@RequestParam Long alarmRuleId, RedirectAttributes attributes) {
        attributes.addAttribute("alarmRuleId", alarmRuleId);
        return "alarm/alarm_script_add";
    }

    @GetMapping("/alarmScriptEdit")
    public String alarmScriptEdit(@RequestParam Long alarmScriptId, RedirectAttributes attributes) {
        attributes.addAttribute("alarmScriptId", alarmScriptId);
        return "alarm/alarm_script_edit";
    }

}
