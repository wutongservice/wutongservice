<!DOCTYPE html>
<html lang="en">
<head>
  <title>Global stat</title>
  <%@include file="fragments/includeStyleAndScript.jspf" %>
  <script src="/static/borqsmarket/js/morris.min.js"></script>
  <script src="/static/borqsmarket/js/raphael-min.js"></script>
  <link href="/static/borqsmarket/css/morris.css" rel="stylesheet">

  <script>


    $(function () {

      var quarter_data = ${records};

      Morris.Line({
        element: 'countryProductsPerDate',
        data: quarter_data,
        xkey: 'dates',
        ykeys: ${appList},
        labels: ${appList}

      });
    });


    $(function () {
      Morris.Donut({
        element: 'versionProductsPerDate',
        data: ${rsPie}

      });
    });
  </script>
</head>
<body>
<bm:topNavigationBar module="gstat"/>
<bm:secondNavigationBar module="gstat" currentAt="all"/>
<div class="container">
  <div class="row">
    <div class="span8 offset2" id="content">
      <h1 style="margin-top: 40px;margin-bottom: 40px;">
        <spring:message code="gstat.text.accountsCount"/>
        <strong style="text-indent: 30px;color: #41bb19;font-style:italic">${accountsCount}</strong>
      </h1>
      <hr>

      <div class="row-fluid">
        <div class="span8 pull-deft ">
          <ul id="statTypes" class="nav nav-pills">
            <c:forEach items="${available_stat_types}" var="st">
              <li class="${st.id==current_stat_type?'active':''}">
                <a href="${bm:stringFormat2('/gstat/all?months=%s&stat_type=%s', current_months, st.id)}">
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
                <a href="${bm:stringFormat2('/gstat/all?months=%s&stat_type=%s', m, current_stat_type)}">
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