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
  <div class="signin-form" style="position: relative">
    <h2 class="text-center" style="margin: 20px"><spring:message code="signup.title.form"/></h2>

    <form id="signinForm" method="POST">
      <fieldset>
        <div class="control-group ${email_errorClass}">
          <input type="text" id="email" name="email" class="controls"
                 placeholder="${bm:springMessage(pageContext, 'signup.placeholder.email')}"
                 value="${email}">
          <span class="help-block">${email_errorMessage}</span>
        </div>
        <div class="control-group ${password_errorClass}">
          <input type="password" id="password" name="password" class="controls"
                 placeholder="${bm:springMessage(pageContext, 'signin.placeholder.password')}">
        </div>

        <div class="control-group ${repassword_errorClass}">
          <input type="password" id="repassword" name="repassword" class="controls"
                 placeholder="${bm:springMessage(pageContext, 'signup.placeholder.repassword')}">
          <span class="help-block">${repassword_errorMessage}</span>
        </div>

        <div class="text-center">
          <button class="btn btn-primary submit-button" type="submit"><i class="icon-user icon-white"></i>
            <spring:message code="signup.button.signup"/>
          </button>
        </div>
      </fieldset>
    </form>
    <div style="right:0;position: absolute;"><a href="/signin"><spring:message code="signup.text.signin"/></a></div>
  </div>
</div>
<bm:bottomFooter/>
</body>
</html>