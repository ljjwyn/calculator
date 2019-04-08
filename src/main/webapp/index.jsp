<%@ page import="java.util.Map" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.ArrayList" %><%--
  Created by IntelliJ IDEA.
  User: Administrator
  Date: 2019/4/4
  Time: 16:24
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>Title</title>
</head>
<body>
<%
    String tablename="user";
    String fflag1="b>a";
    String fresult="rank=优";
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
%>

<a href="/users/showprocess">进度</a>
<a href="/users/stopprocess">停止</a>
<a href="/users/show<%=tablename%>">查看<%=tablename%></a>
<a href="/users/cal">查看<%=tablename%></a>
<h1 style="color: red">Hello World</h1>
</body>
</html>
