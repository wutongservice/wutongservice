<!DOCTYPE html>
<html lang="en">
<head>
  <title><spring:message code="signin.page.title"/></title>
  <%@include file="fragments/includeStyleAndScript.jspf" %>
</head>
<body>
<bm:topNavigationBar module="signin"/>
<div id="toastPlaceholder"></div>
<div class="container margin-center" id="signin-container">
  <div class="signin-form" style="position: relative;">
    <h2 class="text-center" style="margin: 20px"><spring:message code="signin.title.form"/></h2>

    <form id="signinForm" method="POST">
      <fieldset>
        <div class="control-group ${username_errorClass}">
          <input type="text" id="username" name="username" class="controls"
                 placeholder="${bm:springMessage(pageContext, 'signin.placeholder.username')}"
                 value="${username}">
        </div>
        <div class="control-group ${password_errorClass}">
          <input type="password" id="password" name="password" class="controls"
                 placeholder="${bm:springMessage(pageContext, 'signin.placeholder.password')}">
        </div>

        <div>
          <label for="locale"><spring:message code="signin.label.locale"/></label>
          <select id="locale" name="locale">
            <option value="en_US">English(US)</option>
            <option value="zh_CN">中文(中国)</option>
          </select>
        </div>
        <div class="text-center">
          <button class="btn btn-primary submit-button" type="submit"><i class="icon-user icon-white"></i>
            <spring:message code="signin.button.signin"/>
          </button>
        </div>
      </fieldset>
    </form>
    <div style="right:0;position: absolute;"><a href="/signup"><spring:message code="signin.text.signup"/></a></div>
  </div>
</div>
<bm:bottomFooter/>
</body>
</html>