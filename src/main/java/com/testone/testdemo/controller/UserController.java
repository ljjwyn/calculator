package com.testone.testdemo.controller;

import com.singularsys.jep.JepException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping("/cal")
public class UserController {
    //private CalculateTool cal=new CalculateTool();
    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    @GetMapping("/index")
    public String index(){
        return "index";
    }

    @PostMapping("/config")
    public Map<String, String> config(@RequestBody Map<String, Object> config) throws JepException {

        System.out.println(config);

        Map<String, String> returnObj = new HashMap<>();
        returnObj.put("taskId", "T1");

        return returnObj;
        /*
        log.info("计算过程");
        String fflag1 = "b>a";
        String fresult = "rank=优";
        Map<String, String> conditions2 = new HashMap<>();
        ArrayList<String> formulas2 = new ArrayList<>();
        conditions2.put(fflag1, fresult);
        String input1 = "X=a+b";
        String input2 = "Y=10";
        String input5 = "P=SUM(c)";
        String input3 = "Z=max(a,b)";
        String input6 = "F=Y+P";
        String input4 = "O=X+Y";
        formulas2.add(input1);
        formulas2.add(input2);
        formulas2.add(input5);
        formulas2.add(input3);
        formulas2.add(input4);
        formulas2.add(input6);

        if (formulas.size() == 0) {
            log.info("000000000000");
        }
        log.info("传入的name" + tablename);
        CalculateTool.setDB(tablename);
        CalculateTool.getformulas(formulas, conditions2);
        CalculateTool.createDB(formulas);
        CalculateTool.calculateall();
            */
    }

    @GetMapping("/show{name}")
    public ArrayList<Map> getname(@PathVariable String name) {
        log.info("获取最终结果");
        log.info("传入的name" + name);
        return CalculateTool.showtable(name);
    }

    @GetMapping("/showprocess")
    public String process() {
        log.info("查看进度");
        Double process = CalculateTool.progress();
        return String.valueOf(process);
    }

    @GetMapping("/stopprocess")
    public boolean Stopprocess() {
        return CalculateTool.stopprogress();
    }

}
