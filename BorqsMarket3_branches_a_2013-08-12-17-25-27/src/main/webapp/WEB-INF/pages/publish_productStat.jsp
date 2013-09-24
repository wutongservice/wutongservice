<!DOCTYPE html>
<html lang="en">
<head>
  <title><spring:message code="publish_productStat.page.title"/></title>
  <%@include file="fragments/includeStyleAndScript.jspf" %>
  <script src="/static/borqsmarket/js/morris.min.js"></script>
  <script src="/static/borqsmarket/js/raphael-min.js"></script>
  <link href="/static/borqsmarket/css/morris.css" rel="stylesheet">

  <style>
    i.star {
      background-image: url(/static/borqsmarket/image/image.axd.png);
      display: inline-block;
      height: 16px;
      width: 80px;
      background-position-x: -16px;
      background-position-y: -48px;
    }

    star-1 {
      background-position-x: -64px;
    }

    star-2 {
      background-position-x: -48px;
    }

    star-3 {
      background-position-x: -32px;
    }

    star-4 {
      background-position-x: -16px;
    }

    star-5 {
      background-position-x: 0;
    }

    star-qtr {
      background-position-y: -32px;
    }

    star-half {
      background-position-y: -16px;
    }

    star-3qtr {
      background-position-y: 0;
    }
  </style>
  <script>




//    $(function () {
//      $("#tab1").show();
//      $("#tab2").hide();
//
//      $("#version_download").click(function (e) {
//        $("#tab2").show();
//        $("#version_download").addClass("btn-primary")
//        $("#tab1").hide();
//        $("#country_download").removeClass("btn-primary")
//
//      });
//      $("#country_download").click(function (e) {
//        $("#tab1").show();
//        $("#country_download").addClass("btn-primary")
//        $("#tab2").hide();
//        $("#version_download").removeClass("btn-primary")
//      });
//    });

  </script>

</head>
<body>
<bm:topNavigationBar module="publish"/>
<bm:secondNavigationBar module="publish" currentAt="apps/${product['app_id']}/${product['id']}"/>
<div class="container">
  <div class="row">
    <div class="span2 affix" style="margin-left:0;">
      <bm:appNavigationBar module="publish"/>
    </div>
    <div class="span8 offset2" id="content">
      <bm:publishProductFeatures productId="${product['id']}" currentFeature="stat"/>

      <div>
        <div>
          <div id="months" style="text-align: left">
            <c:forEach items="${fn:split('1,3,6,12,100', ',')}" var="m">
              <a id="month${m}"
                 href="${bm:stringFormat3('/publish/products/%s/stat?months=%s&stat_type=%s', product['id'], m, current_stat_type)}"
                 class="btn ${m==current_months?'  btn-primary ':''}">
                <c:choose>
                  <c:when test="${m ne '100'}">
                    ${m} <spring:message code="publish_productStat.text.month"/>
                  </c:when>
                  <c:otherwise>
                    <spring:message code="publish_productStat.text.all"/>
                  </c:otherwise>
                </c:choose>
              </a>
            </c:forEach>
            <br/>
            <c:forEach items="${available_stat_types}" var="st">
              <a
                 href="${bm:stringFormat3('/publish/products/%s/stat?months=%s&stat_type=%s', product['id'], current_months, st.id)}"
                 class="btn ${st.id==current_stat_type?'  btn-primary ':''}">
                ${st.name}
              </a>
            </c:forEach>
          </div>
          <hr>
          <div><h2><spring:message code="publish_productStat.title.purchaseCountPerdate"/></h2>
          </div>

        </div>
          <c:if test="${current_stat_type eq 'purchase_count' || current_stat_type eq 'download_count'}">
            <bm:dateBasedLineChart id="totalPerDayLine" graphData="${totalPerDayLine}" namedYKeys="count=>Downloads"/>
            <hr>
            <bm:donutChart id="totalPerDayByCounterDonut" graphData="${totalPerDayByCounterDonut}"/>
            <hr>
            <bm:dateBasedLineChart id="totalPerDayByCountryLine" graphData="${totalPerDayByCountryLine}"/>
            <hr>
          </c:if>
        <%--<bm:dateBasedLineChart id="stat1a" graphData="${stat1a}" namedYKeys="count=>downloads"/>--%>
        <%--<bm:donutChart id="stat2" graphData="${stat2}"/>--%>
        <%--<hr style="color:red"/>--%>

        <%--<div id="purchaseCountPerdateGraph" class="graph"></div>--%>
        <%--<hr>--%>

        <%--<div style="text-align: left; margin-top:15px; margin-bottom: 15px;">--%>
          <%--<button class="btn btn-primary" type="button" id="country_download"><spring:message--%>
              <%--code="publish_productStat.text.countryDownload"/></button>--%>
          <%--<button class="btn" type="button" id="version_download"><spring:message--%>
              <%--code="publish_productStat.text.versionDownload"/></button>--%>
        <%--</div>--%>

        <%--<div class="tab-pane" id="tab1">--%>
          <%--<div style="height: 200px; text-align: left;" id="countryProductsPerDateDonut" class="graph"></div>--%>
          <%--<div id="countryProductsPerDate" class="graph"></div>--%>
        <%--</div>--%>
        <%--<div id="tab2" class="tab-pane">--%>
          <%--<div id="versionProductsPerDate" class="graph"></div>--%>
        <%--</div>--%>
        <%--<hr>--%>
      </div>
    </div>
  </div>
  <bm:bottomFooter/>
</body>
</html>