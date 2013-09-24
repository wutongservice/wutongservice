<!DOCTYPE html>
<html lang="en">
<head>
  <title><spring:message code="publish_selectPrice.page.title"/></title>
  <%@include file="fragments/includeStyleAndScript.jspf" %>
  <style>
    .disabled-price-cell {
      background-color: rgb(223, 223, 223);
    }
  </style>
</head>
<body>
<bm:topNavigationBar module="publish"/>
<bm:secondNavigationBar module="publish" currentAt="apps/${product['app_id']}/${product['id']}"/>
<div class="container">
  <div class="row-fluid">
    <div class="span2 affix left-bar">
      <bm:appNavigationBar module="publish"/>
    </div>
    <div class="span8 offset2 hero-unit" style="text-align: center; line-height: 2em" id="content">
      <h1><spring:message code="publish_selectPrice.title.selectPrice"/></h1>

      <p style="padding: 30px;">
        <spring:message code="publish_selectPrice.text.tip"/>
      </p>

      <form method="POST" class="single-form">
        <fieldset>
          <div class="span10 offset1">
            <table class="table table-hover">
              <thead>
              <tr>
                <th style="width:10%">#</th>
                <th style="width:45%">Google Play</th>
                <th style="width:45%">CMCC MM Market</th>
              </tr>
              </thead>
              <tbody>
              <c:forEach items="${pricetags}" var="pricetag">
                <tr>
                  <td>
                    <input type="radio" name="pricetag_id" value="${pricetag['pricetag_id']}"
                           <c:if test="${current_pricetag_id==pricetag['pricetag_id']}">checked</c:if>>
                  </td>
                  <td class="<c:if test="${pricetag['google_price_display'] eq ''}">disabled-price-cell</c:if>">
                      ${pricetag['google_price_display']}
                  </td>
                  <td class="<c:if test="${pricetag['cmcc_mm_price_display'] eq ''}">disabled-price-cell</c:if>">
                      ${pricetag['cmcc_mm_price_display']}
                  </td>
                </tr>
              </c:forEach>
              </tbody>
            </table>
          </div>
          <div class="span12">
            <button class="btn btn-success btn-large submit-button" type="submit"><i class="icon-ok icon-white"></i>
              <spring:message code="publish_selectPrice.button.set"/>
            </button>
            <button class="btn btn-large submit-button" type="reset">
              <spring:message code="publish_selectPrice.button.reset"/>
            </button>
            <c:if test="${not first}">
            <a class="btn btn-large btn-warning submit-button" href="/publish/products/${product['id']}${version != '' ? '/'.concat(version) : ''}">
              <i class="icon-backward icon-white"></i>
              <spring:message code="publish_selectPrice.button.back"/>
            </a>
            </c:if>
          </div>
        </fieldset>
      </form>
    </div>
  </div>
</div>
<bm:bottomFooter/>
</body>
</html>