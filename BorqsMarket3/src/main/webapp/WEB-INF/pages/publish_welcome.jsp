<!DOCTYPE html>
<html lang="en">
<head>
  <title><spring:message code="publish_welcome.page.title"/></title>
  <%@include file="fragments/includeStyleAndScript.jspf" %>
</head>
<body>
<bm:topNavigationBar module="publish"/>
<bm:secondNavigationBar module="publish" currentAt="apps"/>
<div class="container">
  <div class="row">
    <div class="span2 affix left-bar">
      <bm:appNavigationBar module="publish"/>
    </div>
    <div class="span8 offset2 hero-unit" style="text-align: center; line-height: 2em" id="content">
      <h1><spring:message code="publish_welcome.title.welcome"/></h1>

      <p style="padding:30px;">
        <i class="icon-hand-left"></i>&nbsp;<spring:message code="publish_welcome.text.tip"/>
      </p>
    </div>
  </div>
</div>
<bm:bottomFooter/>
</body>
</html>