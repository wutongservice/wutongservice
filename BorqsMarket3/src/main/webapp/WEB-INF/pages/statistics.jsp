<!DOCTYPE html>
<html lang="en">
<head>
  <title><spring:message code="publish_productOrders.page.title"/></title>
  <%@include file="fragments/includeStyleAndScript.jspf" %>
  <script src="/static/borqsmarket/js/bootstrap-paginator.min.js"></script>
  <script module='text/javascript'>
    $(function () {
      $(".form_datetime").datepicker({
        format: "yyyy-mm",
        autoclose: true,
        todayBtn: true,
        viewMode: 1,
        minViewMode: 1,
        pickerPosition: "bottom-left"
      });
    });
    /*$(function () {


     $(".tr_menu").click(function () {

     var hideTr = $(this).next();       //得到它的下个标签
     var hideTr2 = hideTr.next();
     if (hideTr.is(":hidden")) {        //如果下个标签是隐藏的
     hideTr.show();
     hideTr2.show();
     } else {
     hideTr.hide();
     hideTr2.hide();
     }
     });


     });*/
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

          return "/publish/products/${product['id']}/orders?pages=" + page + idstr + orderStartDate + orderEndDate + versionStr + orderMonth;
        }

      }

      $('#paginator').bootstrapPaginator(options);
    });


  </script>
</head>
<body>
<bm:topNavigationBar module="publish"/>
<bm:secondNavigationBar module="publish" currentAt="apps/${product['app_id']}/${product['id']}"/>

<form class="form-search" action="/publish/products/${product['id']}/orders" method="post" accept-charset="UTF-8">
  <div class="container">
    <div class="row">
      <div class="span2 affix left-bar">
        <bm:appNavigationBar module="publish"/>
      </div>
      <div class="span8 offset2" id="content" style="text-align: right">
        <bm:publishProductFeatures productId="${product['id']}" currentFeature="orders"/>
        <%--<div class="control-group row-fluid">
                            <div class="span5"><label for="product_name"><spring:message code="order.text.productName"/></label>
                                <input type="text"  name="product_name" value="${product_name}" class="input-medium search-query">
                            </div>

                            <div class="span5">
                                <label for="product_version"><spring:message code="order.text.productVersion"/></label>
                                <input type="text" name="product_version" value="${product_version}"
                                       class="input-medium search-query">
                            </div>
                        </div>--%>

        <%--<div class="control-group row-fluid">
            <div class="span5" style="padding-top: 10px"><label for="orderStartDate"><spring:message
                    code="order.text.orderStartDate"/></label>
                <input type="text" name="orderStartDate" value="${orderStartDate}"
                       class="input-medium search-query form_datetime" >
            </div>

            <div class="span5" style="padding-top: 10px">
                <label for="orderEndDate"><spring:message code="order.text.orderEndDate"/></label>
                <input type="text" name="orderEndDate" value="${orderEndDate}"
                       class="input-medium search-query form_datetime" >
            </div>
        </div>--%>
        <div class="control-group row-fluid">
          <div class="span5" style="padding-top: 10px">
            <label for="orderMonth"><spring:message code="order.text.orderMonth"/></label>
            <input type="text" name="orderMonth" value="${orderMonth}"
                   class="input-medium search-query form_datetime">
          </div>
        </div>

        <div class="span8 text-center">
          <button type="submit" name="search" value="search" class="btn"><spring:message
              code="order.text.search"/></button>

          <input type="hidden" id="product_name" value="${product_name}"/>
          <input type="hidden" id="product_version" value="${product_version}"/>
          <input type="hidden" id="orderStartDate" value="${orderStartDate}"/>
          <input type="hidden" id="orderEndDate" value="${orderEndDate}"/>
          <input type="hidden" id="orderMonth" value="${orderMonth}"/>
          <button type="submit" name="export" value="export" class="btn"><spring:message
              code="order.text.export"/></button>
        </div>
        <hr>
        <div class="tabbable">
          <div class="tab-content">

            <table class="table table-hover table-striped product-table">
              <thead>
              <tr>
                <th width="30%"><spring:message code="order.text.productName"/></th>
                <th width="10%"><spring:message code="order.text.productVersion"/></th>
                <th width="10%"><spring:message code="order.text.productCategory"/></th>
                <th width="20%"><spring:message code="order.text.orderDate"/></th>
                <th width="20%"><spring:message code="order.text.purchaser"/></th>
                <th width="10%"><spring:message code="order.text.purchaserCountry"/></th>

              </tr>
              </thead>
              <tbody>
              <c:forEach items="${records}" var="record">
                <tr class="tr_menu">
                  <td>${record.name}</td>
                  <td>${record.product_version}</td>
                  <td>${record.product_category_id}</td>
                  <td>
                      ${bm:datetimeToStr(pageContext, record.created_at)}
                  <td>${record.purchaser_name}</td>
                  <td>${record.purchaser_locale}</td>
                </tr>

                <%--<tr style="display: none" >
                    <th colspan="6"><spring:message code="order.text.orderId"/></th>
                </tr>

                <tr style="display: none">
                    <td colspan="6">${record.id}</td>

                </tr>--%>
              </c:forEach>
              </tbody>
            </table>

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