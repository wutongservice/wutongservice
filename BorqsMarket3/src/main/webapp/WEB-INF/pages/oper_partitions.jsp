<!DOCTYPE html>
<html lang="en">
<head>
  <title><spring:message code="oper_partitions.page.title"/></title>
  <%@include file="fragments/includeStyleAndScript.jspf" %>
  <script type="text/javascript">
    $(function () {
      $('#paginator').bootstrapPaginator({
        currentPage: ${pagination.currentPage},
        totalPages: ${pagination.totalPages},
        pageUrl: function (type, page, current) {
          return '?page=' + (page - 1);
        }
      });
    });

    $(function() {
      $(".copy-id").zclip({
        path: "/static/jquery/js/ZeroClipboard.swf",
        copy: function(){
          return $(this).attr('data-partition-id');
        }
      });
    });
  </script>
</head>
<body>
<bm:topNavigationBar module="oper"/>
<bm:secondNavigationBar module="oper" currentAt="apps/${currentAppId}"/>
<div class="container">
  <div class="row">
    <div class="span2 affix left-bar">
      <bm:appNavigationBar module="oper"/>
    </div>
    <div class="span8 offset2" id="content">
      <bm:operAppFeaturesTag currentFeature="managePartitions" currentCategory="${currentCategory}"
                             appId="${currentAppId}"/>
      <ul class="nav nav-pills">
        <c:forEach items="${categories}" var="category">
          <li class="${currentCategory eq category.category_id ? 'active' : ''}">
            <a href="/oper/apps/${currentAppId}/partitions?category=${category.category_id}">
              <strong>${category.name}</strong>
            </a>
          </li>
        </c:forEach>
      </ul>
      <hr>

      <div class="row-fluid" style="margin-bottom: 20px;">
        <div class="span12">
          <a class="btn btn-success" href="/oper/apps/${currentAppId}/new_partition?category=${currentCategory}">
            <i class="icon-white icon-plus"></i>
            <strong><spring:message code="oper_partitions.button.addPartition"/></strong>
          </a>
        </div>
      </div>
      <div class="row-fluid">

        <c:if test="${pagination.totalPages > 0}">
          <table id="partitionsTable" class="table table-hover table-striped product-table" style="margin-top:30px;">
            <thead>
            <tr>
              <th width="50%"><spring:message code="oper_partitions.text.name"/></th>
              <th width="30%"><spring:message code="oper_partitions.text.createdAt"/></th>
              <th width="20%" style="text-align: right;"><spring:message code="oper_partitions.text.action"/></th>
            </tr>
            </thead>
            <tbody>

            <c:forEach items="${partitions}" var="partition">
              <tr class="partition-row" data-partition-id="${partition.id}">
                <td>
                  <div style="position: relative;float: left;">
                    <a class="copy-id" data-toggle="tooltip" title="Copy" data-partition-id="${partition.id}">
                      <i class="icon-share"></i>
                    </a>
                  </div>
                  <a href="/oper/partitions/${partition.id}">${partition.name}</a>
                </td>
                <td>${bm:dateToStr(pageContext, partition.created_at)}</td>
                <td style="text-align: right;">
                  <a class="btn btn-danger" href="/oper/delete_partition?id=${partition.id}">
                    <i class="icon-white icon-remove"></i>
                    <strong>&nbsp;<spring:message code="oper_partitions.button.deletePartition"/></strong>
                  </a>
                </td>
              </tr>
            </c:forEach>
            </tbody>
          </table>
          <div id="paginator"></div>
        </c:if>
        <c:if test="${pagination.totalPages == 0}">
          <div class="row-fluid">
            <div class="well" style="height:200px;">
              <p>${bm:springMessage(pageContext, 'oper_partitions.text.emptyPartitionsTip')}</p>
            </div>
          </div>
        </c:if>
      </div>
    </div>
  </div>
  <form id="setActivePartitionForm" method="POST" style="display: none;">
    <input type="hidden" id="status" name="status" value=""/>
    <input type="hidden" id="partition_id" name="partition_id" value=""/>
  </form>
</div>
<bm:bottomFooter/>
</body>
</html>