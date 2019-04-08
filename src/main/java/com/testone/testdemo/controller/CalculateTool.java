package com.testone.testdemo.controller;

import com.singularsys.jep.Jep;
import com.singularsys.jep.JepException;
import com.singularsys.jep.ParseException;

import java.sql.*;
import java.util.*;

public class CalculateTool {
    static int taskid=getTaskid();
    static int sum=0;
    static double progress=0.00;
    static boolean go=true;
    static String DBname="";
    static Map<String, Double> singlemap = new HashMap<>();
    static Map<String, Double> quanmap = new HashMap<>();
    static Set<String> originset = new HashSet<String>() {
        {
            add("a");
            add("b");
            add("c");
        }
    };
    static Map<String, Double> orginmap = new HashMap<>();
    static ArrayList<String> formulas = new ArrayList<>();
    static Map<String, String> conditions = new HashMap<>();
    static boolean fquan=true;

//    public static void main(String[] args) throws JepException, SQLException {
//        //连接两个数据库
//        //String DBname=link("test1","test2");
//        //选择数据表
//        setDB("user");
//        showorgin();
//        //获取公式
//        //getformulas();
//        //解析公式，创建数据库
//        //createDB(formulas);
//        //计算公式并中间结果存入数据库
//        //calculateall();
//        //返回源数据表
//        //showorgin();
//        //进度
//        //progress();
//        //停止
//        //stopprogress();
//    }
   
    public static void setDB(String name) {
        DBname=name;
        sum=getSum();
    }

    public static boolean stopprogress() {
        go=false;
        return true;
    }


    //返回进度
    public static Double progress() {
        return progress;
    }


    public static ArrayList<Map> showtable(String name){
        String sql="select * from "+name;
        ArrayList<Map> list=new ArrayList<>();
        Connection conn = DataBaseUtils.getConnection();
        try {
            Statement stmt = conn.createStatement();
            System.out.println(sql);
            ResultSet rs = stmt.executeQuery(sql);
            ResultSetMetaData meta1 = rs.getMetaData();
            int cl = meta1.getColumnCount();
            while (rs.next()) {
                Map<String, String> maps = new HashMap<>();
                for (int i = 1; i < cl + 1; i++) {
                    String colname=meta1.getColumnName(i);
                    maps.put(colname, String.valueOf(rs.getObject(colname)));
                }
                list.add(maps);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {//关闭数据库链接
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return list;
    }

    public static void calculateall() throws JepException {
        Connection conn = DataBaseUtils.getConnection();
        String sql="select * from "+DBname;
        try {
            Statement stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY,
                    ResultSet.CONCUR_READ_ONLY);
            stmt.setFetchSize(Integer.MIN_VALUE);
            ResultSet rs = stmt.executeQuery(sql);
            int count=0;
            //遍历每条记录
            while (rs.next()&&go) {
                //先存储需要的原变量的信息
                int idd=rs.getInt("id");
                Double didd=idd+0.00;
                orginmap.put("id", didd);
                String need = orginmap.keySet().toString();
                String need2 = need.substring(1, need.length() - 1);
                String[] vaA = need2.split(", ");
                //取得需要的属性值
                for (int t = 0; t < vaA.length; t++) {
                    String needone = vaA[t];
                    orginmap.put(needone,rs.getDouble(needone));
                }
                //对单条记录进行计算
                calculate(idd);
                //对单条记录进行规则判断
                dorule(idd);
                count++;
                progress=count/sum;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 单个记录的map存入数据库
     */
    public static void saveDB(int idd) {
        System.out.println("编号"+idd+"单次变量如下：");
        for (String key : singlemap.keySet()) {
            System.out.println("Key: " + key + " Value: " + singlemap.get(key));
        }
        singleDB(idd);
        System.out.println("全局变量如下：");
        for (String key : quanmap.keySet()) {
            System.out.println("Key: " + key + " Value: " + quanmap.get(key));
        }

        //第一次存全局变量，之后不存
        if (fquan) {
            quanDB();
            fquan=false;
        }
    }

    /**
     * 规则判断
     * @throws JepException
     */
    public static void dorule(int idd) throws JepException {
        //对规则中每条变量注入值并判断
        for(String s:conditions.keySet()){
            String fflag=s;
            boolean flag=setFlag(idd, fflag);
            if(flag){
                //判断成功，则添加标签
                insertconDB(conditions.get(fflag),idd);
            }
        }

    }

    /**
     * 公式计算并把中间结果存储
     * @throws JepException
     */
    public static void calculate(int idd) throws JepException {

        //对记录逐一运用公式
        for (int i = 0; i < formulas.size(); i++) {
            String input = formulas.get(i);
            String[] nameStrArray = input.split("=");
            String p = nameStrArray[0];
            String formula = nameStrArray[1];
            Jep jep = new Jep(); //一个数学表达式
            jep.setAllowUndeclared(true);
            jep.getVariableTable().remove("e");
            jep.getVariableTable().remove("true");
            jep.getVariableTable().remove("false");
            jep.getVariableTable().remove("pi");
            jep.getVariableTable().remove("i");
            jep.parse(formula);
            boolean isquan=isquan(formula);
            //判断是否是全局变量
            if (isquan) {
                if (quanmap.get(p) != 0.00) {
                    //全局变量已经计算，跳过
                } else {
                    //全局变量计算一次
                    System.out.println("全局变量"+p+"计算了一次");
                    if(formula.contains("MAX")||formula.contains("MIN")||formula.contains("SUM")||formula.contains("AVG")){
                        Double re_calone = calone(formula);
                        quanmap.put(p, re_calone);
                    }else {
                        String re_ana=analysis(formula);
                        String[] vaArray=re_ana.split(", ");
                        for (int j = 0; j < vaArray.length; j++) {
                            for (String s : quanmap.keySet()) {
                                if (vaArray[j].equals(s)) {
                                    jep.addVariable(s, quanmap.get(s));
                                }
                            }
                        }
                        Double result = (Double) jep.evaluate();
                        quanmap.put(p, result);
                    }
                }

            } else {
                try {
                    String v = jep.getVariableTable().keySet().toString();
                    String v2 = v.substring(1, v.length() - 1);
                    String[] vaArray = v2.split(", ");
                    for (int j = 0; j < vaArray.length; j++) {
                        String s=vaArray[j];
                        if(inorigin(s)){
                            jep.addVariable(s, orginmap.get(s));
                        }
                        else if (insingle(s)){
                            jep.addVariable(s, singlemap.get(s));
                        }
                        else if(inquan(s)){
                            jep.addVariable(s, quanmap.get(s));
                        }
                    }
                    Double result = (Double) jep.evaluate();
                    singlemap.put(p, result);
                } catch (JepException e) {
                    e.printStackTrace();
                }
            }
        }
        //把中间变量持久化
        saveDB(idd);
    }

    /**
     * 判断是否是全局变量，如：MAX(a),两个全局变量的四则运算，常数
     * @param formula
     * @return
     * @throws ParseException
     */
    public static boolean isquan(String formula) throws ParseException {
        Jep jep = new Jep(); //一个数学表达式
        jep.setAllowUndeclared(true);
        jep.getVariableTable().remove("e");
        jep.getVariableTable().remove("true");
        jep.getVariableTable().remove("false");
        jep.getVariableTable().remove("pi");
        jep.getVariableTable().remove("i");
        jep.parse(formula);
        boolean find=false;
        int length=jep.getVariableTable().keySet().size();
        if(length==0){
            find=true;
        }else if(formula.contains("MAX")||formula.contains("MIN")||formula.contains("SUM")||formula.contains("AVG"))
        {
            find=true;
        }else {
            String v = jep.getVariableTable().keySet().toString();
            String v2 = v.substring(1, v.length() - 1);
            String[] vaArray = v2.split(", ");
            for (int i = 0; i < vaArray.length; i++) {
                if(inquan(vaArray[i])){
                    find=true;
                }else {
                    find=false;
                    break;
                }
            }
        }
        return find;
    }

    /**
     * 判断是否在单次记录map中
     * @param ss
     * @return
     */
    public static boolean insingle(String ss){
        boolean find =false;
        for (String s : singlemap.keySet()) {
            if (ss.equals(s)) {
                find=true;
            }
        }
        return  find;
    }

    /**
     * 判断是否在全局map中
     * @param ss
     * @return
     */
    public static boolean inquan(String ss){
        boolean find =false;
        for (String s : quanmap.keySet()) {
            if (ss.equals(s)) {
                find=true;
            }
        }
        return  find;
    }

    /**
     * 判断是否在原来数据中
     * @param ss
     * @return
     */
    public static boolean inorigin(String ss){

        boolean find =false;
        for (String s : orginmap.keySet()) {
            if (ss.equals(s)) {
                find=true;
            }
        }
        return  find;

    }
    /**
     *
     * 定义公式集合和判断条件
     *
     */
    public static void getformulas(ArrayList<String> formulas1,Map<String,String> condition1) {
//        String input1 = "X=a+b";
//        String input2 = "Y=10";
//        String input5 = "P=SUM(c)";
//        String input3 = "Z=max(a,b)";
//        String input6 = "F=Y+P";
//        String input4 = "O=X+Y";
//        String fflag1="O>P&&b>a";
//        String fresult="rank=优";
//        //String fflag2="X+Z<=O";
//        //String fresult2="rank=良";
//        conditions.put(fflag1, fresult);
//        //conditions.put(fflag2, fresult2);
//        formulas.add(input1);
//        formulas.add(input2);
//        formulas.add(input5);
//        formulas.add(input3);
//        formulas.add(input4);
//        formulas.add(input6);
          formulas=formulas1;
          conditions=condition1;
//          for(int i=0;i<condition1.size();i++){
//              String key=condition1.get(i).keySet().toString();
//              key=key.substring(1, key.length()-1);
//              System.out.println("key:"+key);
//              String value=condition1.get(i).get(key).toString();
//              conditions.put(key, value);
//              System.out.println("value:"+value);
//          }
    }

    /**
     * 连接两个数据库
     * @param db1
     * @param db2
     * @return 合并后的数据库名称
     *
     */
    public static String link(String db1,String db2) {

        return "user";
    }

    /**
     * 判断规则条件是否为真
     * @param id
     * @param fflag
     * @return
     * @throws JepException
     */
    public static boolean setFlag(int id,String fflag) throws JepException {
        Jep jep = new Jep(); //一个数学表达式
        jep.setAllowUndeclared(true);
        String r=analysis(fflag);
        String[] fva=r.split(", ");
        //System.out.println("表达式"+fflag);
        jep.parse(fflag);
        for (int j = 0; j < fva.length; j++) {
            //System.out.println();
            boolean find = false;
            for (String s : orginmap.keySet()) {
                if (fva[j].equals(s)) {
                    Double re=calsingle(s, id);
                    jep.addVariable(s, re);
                    find = true;
                    break;
                }
            }
            if (!find) {
                for (String s : singlemap.keySet()) {
                    if (fva[j].equals(s)) {
                        Double re=calsinff(s, id);
                        jep.addVariable(s, re);
                        find = true;
                        break;
                    }
                }
            }
            if (!find) {
                for (String s : quanmap.keySet()) {
                    if (fva[j].equals(s)) {
                        jep.addVariable(s, quanmap.get(s));
                        break;
                    }
                }
            }
        }
        boolean flag=(Boolean) jep.evaluate();
        //System.out.println("flag:"+flag);
        return flag;
    }

    /**
     * 往单次变量库写入中间变量
     * @param id
     */
    public static void singleDB(int id) {
        String key = singlemap.keySet().toString();
        String sql = "";
        String values = id + ","+taskid+",";
        String insert = "insert into single"+taskid+"(";
        String key2 = "id,taskid," + key.substring(1, key.length() - 1);
        String val = ") values(";
        for (String s : singlemap.keySet()) {
            values = values + singlemap.get(s) + ",";
        }
        values = values.substring(0, values.length() - 1) + ")";
        sql = insert + key2 + val + values;
        //System.out.println(sql);
        dosql(sql);
    }

    public static int getTaskid() {
        String sql="select taskid from Task where id=1";
        Double resutl=dosqlD(sql);
        int r=resutl.intValue();
        int rr=r+1;
        String sql2="update Task set taskid="+rr;
        dosql(sql2);
        return r;
    }

    public static int getSum() {
        String sql="select count(*) from "+DBname;
        //System.out.println(sql);
        Double resutl=dosqlD(sql);
        int r=resutl.intValue();
        //System.out.println("count "+r);
        return r;
    }
    /**
     * 执行sql
     * @param sql
     */
    public static void dosql(String sql){
        Connection conn = DataBaseUtils.getConnection();
        try {
            Statement stmt = conn.createStatement();
            //System.out.println("sql:"+sql);
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    /**
     * 往全局变量库写入全局变量
     */
    public static void quanDB() {
        String key = quanmap.keySet().toString();
        String sql = "";
        String values = "";
        String insert = "insert into quan"+taskid+"(taskid,";
        String key2 = key.substring(1, key.length() - 1);
        String val = ") values("+taskid+",";
        for (String s : quanmap.keySet()) {
            values = values + quanmap.get(s) + ",";
        }
        values = values.substring(0, values.length() - 1) + ")";
        sql = insert + key2 + val + values;
        dosql(sql);
    }

    /**
     * 往源数据表添加一条标签列
     * @param fresult
     * @return
     */
    public static String addconDB(String fresult) {
        String[] nameStrArray = fresult.split("=");
        String p = nameStrArray[0];
        String alter = "alter table "+DBname+" add ";
        String length = " varchar(255) not null default ";
        String end="' '";
        String sql3 = alter + p + length +end;
        dosql(sql3);
        return p;
    }

    /**
     * 写入每条记录的标签值
     * @param fresult
     * @param id
     */
    public static void insertconDB(String fresult,int id) {
        String[] nameStrArray = fresult.split("=");
        String p = nameStrArray[0];
        String formula = "'"+nameStrArray[1]+"'";
        String alter = "update "+DBname+" set ";
        String length = p+"="+formula;
        String where="  where id=";
        Connection conn = DataBaseUtils.getConnection();
        try {
            Statement stmt = conn.createStatement();
            String sql3 = alter + length+where+id;
            stmt.execute(sql3);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {//关闭数据库链接
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    /**
     * 解析公式，创建两个数据库和map，存放单次变量和全局变量，
     * @param formulas
     */
    public static void createDB(ArrayList<String> formulas) throws ParseException {
        for (int i = 0; i < formulas.size(); i++) {
            String input = formulas.get(i);
            String[] nameStrArray = input.split("=");
            String p = nameStrArray[0];
            String formula = nameStrArray[1];
            if (isquan(formula)) {
                //全局变量
                quanmap.put(p, 0.00);
            } else {
                //单行变量
                singlemap.put(p, 0.00);
            }
            //解析公式，把需要的原生的变量存入orginmap
            String re = analysis(formula);
            String[] vaArray = re.split(", ");
            for (int q = 0; q < vaArray.length; q++) {
                for (String s : originset) {
                    if (vaArray[q].equals(s)) {
                        orginmap.put(s, 0.00);
                    }
                }
            }
        }
        //给源表添加列
        String fresult=conditions.get(conditions.keySet().toArray()[0]);
        //System.out.println("要创建的列是"+fresult);
        addconDB(fresult);
        System.out.println("单行变量有");
        System.out.println(singlemap.keySet());
        System.out.println("全局变量有");
        System.out.println(quanmap.keySet());

        String sql1 = "CREATE TABLE single"+taskid+" (id int PRIMARY KEY,taskid double,";
        for (String str : singlemap.keySet()) {
            sql1 = sql1 + str + " double,";
        }
        sql1 = sql1.substring(0, sql1.length() - 1) + ")";
        //System.out.println("single sql "+sql1);
        String sql2 = "CREATE TABLE quan"+taskid+" (id int default 1,taskid double,";
        for (String str : quanmap.keySet()) {
            sql2 = sql2 + str + " double,";
        }
        sql2 = sql2.substring(0, sql2.length() - 1) + ")";
        String sql3="insert into DBdetail(taskid,DBname,tablename) values("+taskid+",'test','single"+taskid+"')";
        String sql4="insert into DBdetail(taskid,DBname,tablename) values("+taskid+",'test','quan"+taskid+"')";
        Connection conn = DataBaseUtils.getConnection();
        try {
            Statement stmt = conn.createStatement();
            stmt.execute(sql1);
            stmt.execute(sql3);
            stmt.execute(sql2);
            stmt.execute(sql4);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {//关闭数据库链接
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 获取全局变量值，如MAX（a）
     * @param formula
     * @return
     */
    public static Double calone(String formula) {
        Double result = 0.00;
        String select = "select ";
        String from = " from "+DBname;
        String sql = "";
        sql = select + formula + from;
        result=dosqlD(sql);
        return result;

    }

    /**
     * 查询SQL
     * @param sql
     * @return
     */
    public static Double dosqlD(String sql){
        Double result=0.00;
        Connection conn = DataBaseUtils.getConnection();
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                result = rs.getDouble(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {//关闭数据库链接
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    /**
     * 获取单次记录的值，如a+b
     */
    public static Double calsingle(String formula, int id) {
        Double result = 0.00;
        String select = "select ";
        String from = " from "+DBname+" where id =";
        String sql = "";
        sql = select + formula + from + id;
        result=dosqlD(sql);
        return result;
    }

    /**
     * 获取单次记录中间变量的值
     * @param formula
     * @param id
     * @return
     */
    public static Double calsinff(String formula, int id) {
        Double result = 0.00;
        String select = "select ";
        String from = " from single"+taskid+" where id =";
        String sql = "";
        sql = select + formula + from + id;
        //System.out.println(sql);
        result=dosqlD(sql);
        return result;
    }

    /**
     * jep解析
     * @param formula
     * @return "a,b,c"
     */
    public static String analysis(String formula) {
        Jep jep = new Jep(); //一个数学表达式
        String v2 = "";
        jep.setAllowUndeclared(true);
        jep.getVariableTable().remove("e");
        jep.getVariableTable().remove("true");
        jep.getVariableTable().remove("false");
        jep.getVariableTable().remove("pi");
        jep.getVariableTable().remove("i");
        try {
            jep.parse(formula);
            String v = jep.getVariableTable().keySet().toString();
            v2 = v.substring(1, v.length() - 1);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return v2;
    }

}