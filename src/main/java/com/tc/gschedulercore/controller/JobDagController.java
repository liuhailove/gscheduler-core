package com.tc.gschedulercore.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/jobdag")
public class JobDagController {
    @GetMapping("/add")
    public String jobdagAdd() {
        return "jobdag/add";
    }
}
