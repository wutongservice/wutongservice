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
            <div>
                <div id="months" style="text-align: left">
                    <a id="month1" href="/gstat/all?months=1" class="btn ${months==1?'  btn-primary ':''}">1 <spring:message code="publish_productStat.text.month"/></a>
                    <a id="month3" href="/gstat/all?months=3" class="btn${months==3?' btn-primary ':''}">3 <spring:message code="publish_productStat.text.month"/></a>
                    <a id="month6" href="/gstat/all?months=6" class="btn ${months==6?'btn-primary ':''}">6 <spring:message code="publish_productStat.text.month"/></a>
                    <a id="month12" href="/gstat/all?months=12" class="btn ${months==12?'btn-primary ':''}">12 <spring:message code="publish_productStat.text.month"/></a>
                    <a id="monthall" href="/gstat/all?months=100"
                       class="btn ${months==100?' btn-primary ':''}"><spring:message
                            code="publish_productStat.text.all"/></a>
                </div>
                <hr>
                <div><h2><spring:message code="gstat.text.line"/></h2>
                </div>

            </div>
            <div id="countryProductsPerDate" class="graph"></div>
            <hr>
            <div><h2><spring:message code="gstat.text.pie"/></h2>
            </div>
            <div style="width: 300px; text-align: left" id="versionProductsPerDate" class="graph"></div>
            </br>
        </div>
    </div>
</div>
<bm:bottomFooter/>
</body>
</html>