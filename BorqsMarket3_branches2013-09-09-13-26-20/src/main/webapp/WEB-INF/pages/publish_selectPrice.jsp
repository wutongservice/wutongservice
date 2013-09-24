<!DOCTYPE html>
<html lang="en">
<head>
  <title><spring:message code="publish_selectPrice.page.title"/></title>
  <%@include file="fragments/includeStyleAndScript.jspf" %>
</head>
<body>
<bm:topNavigationBar module="publish"/>
<bm:secondNavigationBar module="publish" currentAt="apps/${product['app_id']}/${product['id']}"/>
<div class="container">
  <div class="row">
    <div class="span2 affix" style="margin-left:0;">
      <bm:appNavigationBar module="publish"/>
    </div>
    <div class="span8 offset2 hero-unit" style="text-align: center; line-height: 2em" id="content">
      <h1><spring:message code="publish_selectPrice.title.selectPrice"/></h1>

      <p style="padding: 30px;">
        <spring:message code="publish_selectPrice.text.tip"/>
      </p>

      <form method="POST" class="single-form">
        <fieldset>
          <select id="pricetag_id" name="pricetag_id" class="input-large">
            <c:forEach items="${pricetags}" var="pricetag">
              <option value="${pricetag['pricetag_id']}"
                      <c:if
                          test="${current_pricetag_id==pricetag['pricetag_id']}">selected</c:if>  >${pricetag['display']}</option>
            </c:forEach>
          </select>
          <br>
          <button class="btn btn-success btn-large submit-button" type="submit"><i class="icon-ok icon-white"></i>
            <spring:message code="publish_selectPrice.button.set"/>
          </button>
          <button class="btn btn-large submit-button" type="reset">
            <spring:message code="publish_selectPrice.button.reset"/>
          </button>
        </fieldset>
      </form>
    </div>
  </div>
</div>
<bm:bottomFooter/>
</body>
</html>