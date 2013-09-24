<!DOCTYPE html>
<html lang="en">
<head>
  <title><spring:message code="oper_welcome.page.title"/></title>
  <%@include file="fragments/includeStyleAndScript.jspf" %>
</head>
<body>
<bm:topNavigationBar module="oper"/>
<bm:secondNavigationBar module="oper" currentAt="apps"/>
<div class="container">
  <div class="row">
    <div class="span2 affix left-bar">
      <bm:appNavigationBar module="oper"/>
    </div>
    <div class="span8 offset2 hero-unit" style="text-align: center; line-height: 2em" id="content">
      <h1><spring:message code="oper_welcome.title.welcome"/></h1>

      <p style="padding:30px;">
        <i class="icon-hand-left"></i>&nbsp;<spring:message code="oper_welcome.text.tip"/>
      </p>
    </div>
  </div>
</div>
<bm:bottomFooter/>
</body>
</html>