<!DOCTYPE html>
<html lang="en">
<head>
  <title><spring:message code="publish_app.page.title"/></title>
  <%@include file="fragments/includeStyleAndScript.jspf" %>
  <style type="text/css">
    .product-table {
      margin-bottom: 40px;
    }

    .product-table .version-td, .product-table .version-th {
      text-align: right ! important;
    }

    .product-table .product-cell {
      text-align: right !important;
    }
  </style>
  <script style="javascript">
    var popoverOpened = null;
    $(function () {
      var versionListButtons = $('[data-toggle=popover]');
      versionListButtons.popover({
        html: true,
        trigger: 'manual'
      });
      versionListButtons.unbind("click");
      versionListButtons.bind("click", function (e) {
        e.stopPropagation();
        if ($(this).equals(popoverOpened)) return;
        if (popoverOpened !== null) {
          popoverOpened.popover("hide");
        }
        $(this).popover('show');
        popoverOpened = $(this);
        e.preventDefault();
      });

      $(document).click(function (e) {
        if (popoverOpened !== null) {
          popoverOpened.popover("hide");
          popoverOpened = null;
        }
      });

      // active tab
      $('ul#productTabs>li:first-child').attr('class', 'active');
    });

  </script>
</head>
<body>
<bm:topNavigationBar module="publish"/>
<bm:secondNavigationBar module="publish" currentAt="apps/${appId}"/>
<div class="container">
  <div class="row">
    <div class="span2 affix" style="margin-left:0;">
      <bm:appNavigationBar module="publish"/>
    </div>
    <div class="span8 offset2" id="content">
      <div class="tabbable">
        <ul id="productTabs" class="nav nav-pills">
          <c:forEach items="${products}" var="products">
            <li class=""><a href="#${products.key}"
                            data-toggle="tab"><strong>${products.value.category_name}</strong></a>
            </li>
          </c:forEach>
        </ul>
        <div class="tab-content">
          <c:forEach items="${products}" var="products" varStatus="idx">
            <c:choose>
              <c:when test="${idx.index==0}">
                <c:set var="tabActive" value="active"/>
              </c:when>
              <c:otherwise>
                <c:set var="tabActive" value=""/>
              </c:otherwise>
            </c:choose>
            <div class="tab-pane ${tabActive}" id="${products.key}">
              <div>
                <a class="btn btn-success btn-large"
                   href="/publish/apps/${currentAppId}/upload?category=${products.key}">
                  <i class="icon-white icon-plus"></i>
                  <strong>${products.value.category_name}</strong>
                </a>
              </div>
              <hr>

              <c:if test="${not empty products.value.products}">
                <table class="table table-hover table-striped product-table">
                  <thead>
                  <tr>
                    <th width="20%"><spring:message code="publish_app.label.name"/></th>
                    <th width="20%" class="product-cell"><spring:message code="publish_app.label.createdAt"/></th>
                    <th width="30%" class="product-cell"><spring:message code="publish_app.label.downDesc"/></th>
                    <th width="10%" class="product-cell"><spring:message code="publish_app.label.price"/></th>
                    <th width="20%" class="version-th"><spring:message
                        code="publish_app.label.lastVersion"/></th>
                  </tr>
                  </thead>
                  <tbody>
                  <c:forEach items="${products.value.products}" var="product">
                    <tr>
                      <td>
                        <div style="display:none;">${product.id}</div>
                        <a href="/publish/products/${product.id}" data-toggle="tooltip"
                           title="${product.id}"><strong>${product.name}</strong></a>
                      </td>
                      <td class="product-cell">
                          ${bm:dateToStr(pageContext, product.create_at)}
                      </td>
                      <td class="product-cell">
                          ${product.today_download}&nbsp;&nbsp;/&nbsp;&nbsp;${product.all_download}
                      </td>
                      <td class="product-cell">
                          ${product.price}
                      </td>
                      <td class="version-td">
                        <bm:displayVersion version="${product.last_version.version}"
                                           versionName="${product.last_version.version_name}"
                                           versionStatus="${product.last_version.status}"
                                           product="${product.last_version.id}"/>
                        <c:set var="version_list">
                          <ul style="list-style-module:none;margin:0;"><c:forEach
                              items="${product.versions}" var="theVer">
                            <li><bm:displayVersion version="${theVer.version}"
                                                   versionName="${theVer.version_name}"
                                                   versionStatus="${theVer.status}"
                                                   product="${theVer.id}"/></li>
                          </c:forEach></ul>
                        </c:set>
                        <a data-toggle="popover" data-placement="right"
                           data-content='${version_list}'>
                          <i class="icon-list" style="margin-left: 5px"></i>
                        </a>
                      </td>
                    </tr>
                  </c:forEach>
                  </tbody>
                </table>
              </c:if>
              <c:if test="${empty products.value.products}">
                <div class="well" style="height:200px;">
                  <p>${bm:stringFormat1(bm:springMessage(pageContext, 'publish_app.text.emptyProductsTip'), products.value.category_name)}</p>
                </div>
              </c:if>
            </div>
          </c:forEach>
        </div>
      </div>
    </div>
  </div>
</div>
<bm:bottomFooter/>
</body>
</html>