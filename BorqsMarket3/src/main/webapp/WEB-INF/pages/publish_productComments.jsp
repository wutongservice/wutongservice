<!DOCTYPE html>
<html lang="en">
<head>
  <title><spring:message code="publish_productOrders.page.title"/></title>
  <%@include file="fragments/includeStyleAndScript.jspf" %>
  <script src="/static/borqsmarket/js/bootstrap-paginator.min.js"></script>
  <script src="/static/jquery/js/jquery.rateit.min.js"></script>
  <link href="/static/jquery/css/rateit.css" rel="stylesheet">
  <script module='text/javascript'>
    $(function () {
      var s =
      ${total}%
      ${count};
      var s0 = ${total}/${count};
      var s1 = Math.ceil(s0);
      var p = s > 0 ? s1 : ${total}/${count};

      var options = {
        currentPage: ${pages},
        totalPages: p,
        alignment: "right",
        pageUrl: function (module, page, current) {
          var product_id = $("#product_name").val();
          if (product_id != '')
            idstr = "&id=" + product_id;
          else
            idstr = '';

          var product_version = $("#product_version").val();
          if (product_version != '')
            versionStr = "&product_version=" + product_version;
          else
            versionStr = '';

          var orderStartDate = $("#orderStartDate").val();
          if (orderStartDate != '')
            orderStartDate = "&orderStartDate=" + orderStartDate;
          else
            orderStartDate = '';

          var orderEndDate = $("#orderEndDate").val();
          if (orderEndDate != '')
            orderEndDate = "&orderEndDate=" + orderEndDate;
          else
            orderEndDate = '';
          var orderMonth = $("#orderMonth").val();
          if (orderMonth != '')
            orderMonth = "&orderMonth=" + orderMonth;
          else
            orderMonth = '';

          return "/publish/products/${product['id']}/comments?pages=" + page + idstr + orderStartDate + orderEndDate + versionStr + orderMonth;
        }

      }

      $('#paginator').bootstrapPaginator(options);
    });


  </script>
  <style>
    .comment {
      padding: 10px;
      margin-bottom: 20px;
      border-bottom-style: solid;
      border-bottom-width: 1px;
      border-color: rgba(0, 0, 0, 0.15);
    }

    pre.comment-content {
      min-height: 50px;
      font-family: inherit;
      font-size: 1.0em;
      border-color: rgb(220, 220, 220);
      border-style: dotted;
      color: black;
      /*background-color: rgb(245, 245, 245);*/
      border-bottom-style: none;
      border-left-style: none;
      border-right-style: none;
      margin-bottom: 0;
    }
  </style>
</head>
<body>
<bm:topNavigationBar module="publish"/>
<bm:secondNavigationBar module="publish" currentAt="apps/${product['app_id']}/${product['id']}"/>

<form class="form-search" action="/publish/products/${product['id']}/comments" method="post" accept-charset="UTF-8">
  <div class="container">
    <div class="row">
      <div class="span2 affix left-bar">
        <bm:appNavigationBar module="publish"/>
      </div>
      <div class="span8 offset2" id="content" style="text-align: right">
        <bm:publishProductFeatures productId="${product['id']}" currentFeature="comments"/>

        <input type="hidden" id="product_name" value="${product_name}"/>
        <input type="hidden" id="product_version" value="${product_version}"/>
        <input type="hidden" id="orderStartDate" value="${orderStartDate}"/>
        <input type="hidden" id="orderEndDate" value="${orderEndDate}"/>
        <input type="hidden" id="orderMonth" value="${orderMonth}"/>

        <div class="span8 text-left row-fluid" style="margin-left: 0;">
          <div class="tab-content">
            <c:if test="${not empty records}">
              <c:forEach items="${records}" var="record">
                <div class="comment">
                  <div class="row-fluid" style="padding-bottom: 10px">
                    <strong style="font-size: 1.1em">${record.commenter_name}</strong>
                      <span class="span2 pull-right rateit" data-rateit-value="${record.rating * 5}"
                            data-rateit-ispreset="true"
                            data-rateit-readonly="true"></span>
                  </div>
                  <div class="row-fluid">
                    <pre class="comment-content"><c:out escapeXml="true" value="${record.message}"/></pre>
                  </div>
                  <div class="row-fluid">
                  <span class="pull-left" style="padding:5px;">
                    <strong>@</strong><bm:displayVersion product="${record['product_id']}" version="${record['version']}"
                                       versionName="${record['version_name']}"
                                       versionStatus="${record['version_status']}"
                                       beta="${record['beta']}"/>
                  </span>
                  <span class="pull-right muted" style="padding:5px;">
                  <small><strong><spring:message code="comment.text.commentDate"/></strong>&nbsp;&nbsp;
                      ${bm:datetimeToStr(pageContext, record.updated_at)}</small>
                  </span>
                  </div>
                </div>

              </c:forEach>
            </c:if>
            <c:if test="${empty records}">
              <div class="well">
                <p>
                <spring:message code="comment.text.noComments"/>
                </p>
              </div>
            </c:if>
          </div>
        </div>
        <div id="paginator"></div>
      </div>
    </div>
  </div>
</form>
<bm:bottomFooter/>
</body>
</html>