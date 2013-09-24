<!DOCTYPE html>
<html lang="en">
<head>
  <title><spring:message code="publish_upload.page.title"/></title>
  <%@include file="fragments/includeStyleAndScript.jspf" %>
</head>
<body>
<bm:topNavigationBar module="publish"/>
<c:choose>
  <c:when test="${qualifiedProductId != ''}">
    <bm:secondNavigationBar module="publish" currentAt="apps/${qualifiedAppId}/${qualifiedProductId}"/>
  </c:when>
  <c:otherwise>
    <bm:secondNavigationBar module="publish" currentAt="apps/${qualifiedAppId}"/>
  </c:otherwise>
</c:choose>
<div class="container">
  <div class="row">
    <div class="span2 affix left-bar">
      <bm:appNavigationBar module="publish"/>
    </div>
    <div class="span8 offset2 hero-unit" style="text-align: center; line-height: 2em" id="content">
      <h1>
        <c:choose>
          <c:when test="${mode == 'uploadProduct'}">
            <spring:message code="publish_upload.title.uploadProduct"/>
          </c:when>
          <c:when test="${mode == 'uploadVersion'}">
            <spring:message code="publish_upload.title.uploadVersion"/>
          </c:when>
          <c:otherwise>
            <spring:message code="publish_upload.title.upload"/>
          </c:otherwise>
        </c:choose>
      </h1>

      <p style="padding:30px;">
        <spring:message code="publish_upload.text.tip"/>
      </p>

      <form method="POST" enctype="multipart/form-data" class="single-form">
        <fieldset>
          <input type="hidden" name="qualified_app_id" value="${qualifiedAppId}">
          <input type="hidden" name="qualified_product_id" value="${qualifiedProductId}">
          <input class="input-file input-large" id="file" name="file" type="file">
          <span class="badge badge-warning" title="${bm:springMessage(pageContext, 'publish_upload.text.betaTip')}">
            <input type="checkbox" name="as_beta" id="as_beta" value="1">&nbsp;<strong>BETA</strong>
          </span>
          <br>
          <button type="submit" class="btn btn-success btn-large submit-button">
            <i class="icon-upload icon-white"></i> <spring:message code="publish_upload.button.upload"/>
          </button>
        </fieldset>
      </form>

    </div>
  </div>
</div>
<bm:bottomFooter/>
</body>
</html>