<!DOCTYPE html>
<html lang="en">
<head>
  <title><spring:message code="error.page.title"/></title>
  <%@include file="fragments/includeStyleAndScript.jspf" %>
  <style>
    .error-message {
      position: absolute;
      width: 400px;
      height: 200px;
      left: 50%;
      top: 200px;
      margin-left: -200px;
      margin-top: -100px;
      vertical-align: middle;
    }
  </style>
</head>
<body>
<bm:topNavigationBar module="error"/>
<div class="alert alert-error error-message" style="overflow: scroll">
  <h2 class="alert-heading">Error(${code})</h2>

  <p>${error_msg}</p>
</div>
<bm:bottomFooter/>
</body>
</html>