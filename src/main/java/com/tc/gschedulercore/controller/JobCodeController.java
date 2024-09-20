package com.tc.gschedulercore.controller;


import com.tc.gschedulercore.controller.auth.AuthAction;
import com.tc.gschedulercore.core.dto.ReturnT;
import com.tc.gschedulercore.core.model.JobInfo;
import com.tc.gschedulercore.core.model.JobLogGlue;
import com.tc.gschedulercore.core.util.I18nUtil;
import com.tc.gschedulercore.dao.JobInfoDao;
import com.tc.gschedulercore.dao.JobLogGlueDao;
import com.tc.gschedulercore.enums.GlueTypeEnum;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.Integer.parseInt;

/**
 * job code controller
 *
 * @author honggang.liu 2015-12-19 16:13:16
 */
@Controller
@RequestMapping("/jobcode")
public class JobCodeController {

    @Resource
    private JobInfoDao jobInfoDao;
    @Resource
    private JobLogGlueDao jobLogGlueDao;

    @AuthAction()
    @GetMapping
    @ResponseBody
    public Map<String, Object> index(@RequestParam int jobId) {
        JobInfo jobInfo = jobInfoDao.loadById(jobId);
        List<JobLogGlue> jobLogGlues = jobLogGlueDao.findByJobId(jobId);
        if (jobInfo == null) {
            throw new RuntimeException(I18nUtil.getString("jobinfo_glue_jobid_unvalid"));
        }
        if (GlueTypeEnum.BEAN == GlueTypeEnum.match(jobInfo.getGlueType())) {
            throw new RuntimeException(I18nUtil.getString("jobinfo_glue_gluetype_unvalid"));
        }

        Map<String, Object> map = new HashMap<>();
        // Glue类型-字典
        map.put("GlueTypeEnum", GlueTypeEnum.values());
        map.put("jobInfo", jobInfo);
        map.put("jobLogGlues", jobLogGlues);
        return map;
    }

    @AuthAction()
    @PostMapping("/save")
    @ResponseBody
    public ReturnT<String> save(@RequestBody Map<String, String> map) {
        int id = parseInt(map.get("id"));
        String glueSource = map.get("glueSource");
        String glueRemark = map.get("glueRemark");
        // valid
        if (glueRemark == null) {
            return new ReturnT<>(500, (I18nUtil.getString("system_please_input") + I18nUtil.getString("jobinfo_glue_remark")));
        }
        if (glueRemark.length() < 4 || glueRemark.length() > 100) {
            return new ReturnT<>(500, I18nUtil.getString("jobinfo_glue_remark_limit"));
        }
        JobInfo existsJobInfo = jobInfoDao.loadById(id);
        if (existsJobInfo == null) {
            return new ReturnT<>(500, I18nUtil.getString("jobinfo_glue_jobid_unvalid"));
        }

        // update new code
        existsJobInfo.setGlueSource(glueSource);
        existsJobInfo.setGlueRemark(glueRemark);
        existsJobInfo.setGlueUpdatetime(new Date());

        existsJobInfo.setUpdateTime(new Date());
        jobInfoDao.update(existsJobInfo);

        // log old code
        JobLogGlue jobLogGlue = new JobLogGlue();
        jobLogGlue.setJobId(existsJobInfo.getId());
        jobLogGlue.setGlueType(existsJobInfo.getGlueType());
        jobLogGlue.setGlueSource(glueSource);
        jobLogGlue.setGlueRemark(glueRemark);

        jobLogGlue.setAddTime(new Date());
        jobLogGlue.setUpdateTime(new Date());
        jobLogGlueDao.save(jobLogGlue);

        // remove code backup more than 30
        jobLogGlueDao.removeOld(existsJobInfo.getId(), 30);
        return ReturnT.SUCCESS;
    }

}
