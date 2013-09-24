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
</head>
<body>
<bm:topNavigationBar module="publish"/>
<bm:secondNavigationBar module="publish" currentAt="apps/${product['app_id']}/${product['id']}"/>
<div class="container">
  <div class="row">
    <div class="span2 affix left-bar">
      <bm:appNavigationBar module="publish"/>
    </div>
    <div class="span8 offset2" id="content">
      <bm:publishProductFeatures productId="${product['id']}" currentFeature="stat"/>

      <div>
        <div class="row-fluid">
          <div class="span8 pull-deft ">
            <ul id="statTypes" class="nav nav-pills">
              <c:forEach items="${available_stat_types}" var="st">
                <li class="${st.id==current_stat_type?'active':''}">
                  <a href="${bm:stringFormat3('/publish/products/%s/stat?months=%s&stat_type=%s', product['id'], current_months, st.id)}">
                      ${st.name}
                  </a>
                </li>
              </c:forEach>
            </ul>
          </div>
          <div class="btn-group span3 pull-right" style="text-align: right;">
            <a class="btn dropdown-toggle offset2 span10" data-toggle="dropdown" style="text-align: right;">
              ${bm:getStatMonthsLabel(pageContext, current_months)}
              <span class="caret"></span>
            </a>
            <ul id="months" class="dropdown-menu">
              <c:forEach items="${fn:split('1,3,6,12,all', ',')}" var="m">
                <li>
                  <a href="${bm:stringFormat3('/publish/products/%s/stat?months=%s&stat_type=%s', product['id'], m, current_stat_type)}">
                      ${bm:getStatMonthsLabel(pageContext, m)}
                  </a>
                </li>
              </c:forEach>
            </ul>
          </div>
        </div>

        <div>
          <c:if test="${current_stat_type == 'purchase_count'}">
            <h2><spring:message code="publish_productStat.text.totalCount"/></h2>
            <bm:dateBasedLineChart id="totalPerDayLine" graphData="${totalPerDayLine}"/>
            <hr>
            <h2><spring:message code="publish_productStat.text.totalCountByCountry"/></h2>
            <bm:donutChart id="totalPerDayByCountryDonut" graphData="${totalPerDayByCountryDonut}"/>
            <bm:dateBasedLineChart id="totalPerDayByCountryLine" graphData="${totalPerDayByCountryLine}"/>
            <hr>
            <h2><spring:message code="publish_productStat.text.totalCountByVersion"/></h2>
            <bm:donutChart id="totalPerDayByVersionDonut" graphData="${totalPerDayByVersionDonut}"/>
            <bm:dateBasedLineChart id="totalPerDayByVersionLine" graphData="${totalPerDayByVersionLine}"/>
            <hr>
          </c:if>
          <c:if test="${current_stat_type == 'download_count'}">
            <h2><spring:message code="publish_productStat.text.totalCount"/></h2>
            <bm:dateBasedLineChart id="totalPerDayLine" graphData="${totalPerDayLine}"/>
            <hr>
            <h2><spring:message code="publish_productStat.text.totalCountByCountry"/></h2>
            <bm:donutChart id="totalPerDayByCountryDonut" graphData="${totalPerDayByCountryDonut}"/>
            <bm:dateBasedLineChart id="totalPerDayByCountryLine" graphData="${totalPerDayByCountryLine}"/>
            <hr>
            <h2><spring:message code="publish_productStat.text.totalCountByVersion"/></h2>
            <bm:donutChart id="totalPerDayByVersionDonut" graphData="${totalPerDayByVersionDonut}"/>
            <bm:dateBasedLineChart id="totalPerDayByVersionLine" graphData="${totalPerDayByVersionLine}"/>
            <hr>
          </c:if>
        </div>
      </div>
    </div>
  </div>
  <bm:bottomFooter/>
</body>
</html>