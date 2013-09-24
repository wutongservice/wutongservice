<!DOCTYPE html>
<html lang="en">
<head>
  <title><spring:message code="oper_partitions.page.title"/></title>
  <%@include file="fragments/includeStyleAndScript.jspf" %>
  <style>


    div.active-partition {
      width: 50%;
      height: 50%;
      position: absolute;
      text-align: center;
      box-sizing: border-box;
      padding: 10px;
      -webkit-background-size: contain;
      -moz-background-size: contain;
      -o-background-size: contain;
      background-repeat:no-repeat;
    }

    div.active-partition > .lead {
      margin-bottom: 10px;
    }

    div.active-partition-color1 {
      background-color: rgb(220, 220, 220);
    }

    div.active-partition-color2 {
      background-color: rgb(200, 200, 200);
    }

    div.active-partition-dragging {
      background-color: rgb(255, 255, 255);
    }
  </style>
  <script type="text/javascript">
    var ACTIVE_PARTITIONS = ${bm:arrayToJson(activePartitions, true)};

    BorqsMarket.setActivePartition = function (status, partitionId) {
      $('form#setActivePartitionForm > #status').val(status);
      $('form#setActivePartitionForm > #partition_id').val(partitionId);
      $('form#setActivePartitionForm').submit();
    };

    $(function () {
      $('.partition-row')
          .attr('draggable', 'true')
          .on('dragstart', function (ev) {
            var partitionId = $(ev.currentTarget).attr('data-partition-id');
            ev.originalEvent.dataTransfer.setData('partitionId', partitionId);
          });

      $('div.active-partition')
          .on('dragenter', function(ev) {
            ev.preventDefault();
            $(ev.currentTarget).addClass('active-partition-dragging');
          })
          .on('dragleave', function(ev) {
            ev.preventDefault();
            $(ev.currentTarget).removeClass('active-partition-dragging');
          })
          .on('dragover', function (ev) {
            ev.preventDefault();
            var target = $(ev.currentTarget);
            if (!target.hasClass('active-partition-dragging')) {
              target.addClass('active-partition-dragging');
            }
          })
          .on('drop', function (ev) {
            ev.preventDefault();
            $(ev.currentTarget).removeClass('active-partition-dragging');
            var activeId = $(ev.currentTarget).attr('data-active');
            var partitionId = ev.originalEvent.dataTransfer.getData('partitionId');
            BorqsMarket.setActivePartition(activeId, partitionId);
          });
    });

    $(function () {
      $('.partition-select').change(function() {
        var select = $(this);
        var activeId = select.children('option:selected').val();
        var partitionId = select.parents('.partition-row').attr('data-partition-id');
        BorqsMarket.setActivePartition(activeId, partitionId);
      });
    });

    $(function() {
      $('div.active-partition').each(function() {
        var p = $(this);
        var activeId = p.attr('data-active');
        for (var i = 0; i < ACTIVE_PARTITIONS.length; i++) {
          var activePartition = ACTIVE_PARTITIONS[i];
          if (activePartition.status == activeId) {
            p.children('.active-partition-name').html(activePartition.name);
          }
        }
      })
    });

    $(function() {
      $('#paginator').bootstrapPaginator({
        currentPage: ${pagination.currentPage},
        totalPages: ${pagination.totalPages},
        pageUrl: function(type, page, current) {
          return '?page=' + (page - 1);
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
    <div class="span2 affix" style="margin-left:0;">
      <bm:appNavigationBar module="oper"/>
    </div>
    <div class="span8 offset2" id="content">
      <bm:operAppFeaturesTag currentFeature="managePartitions" appId="${currentAppId}"/>
      <ul class="nav nav-pills">
        <c:forEach items="${categories}" var="category">
          <li class="${currentCategory eq category.category_id ? 'active' : ''}">
            <a href="/oper/apps/${currentAppId}/partitions/${category.category_id}">
              <strong>${category.name}</strong>
            </a>
          </li>
        </c:forEach>
      </ul>
      <div class="row-fluid">
        <h2><spring:message code="oper_partitions.title.activePartitions"/></h2>

        <div class="offset2 span8" style="position: relative; height:160px;">
          <div class="active-partition active-partition-color1" style="left:0;top:0;" data-active="1">
            <p class="lead">1</p>
            <p class="active-partition-name text-info"></p>
          </div>
          <div class="active-partition active-partition-color2" style="right:0;top:0;" data-active="2">
            <p class="lead">2</p>
            <p class="active-partition-name text-info"></p>
          </div>
          <div class="active-partition active-partition-color2" style="left:0;bottom:0;" data-active="3">
            <p class="lead">3</p>
            <p class="active-partition-name text-info"></p>
          </div>
          <div class="active-partition active-partition-color1" style="right:0;bottom:0;" data-active="4">
            <p class="lead">4</p>
            <p class="active-partition-name text-info"></p>
          </div>
        </div>
      </div>
      <hr>
      <div class="row-fluid">
        <div class="span12">
          <h2><spring:message code="oper_partitions.title.partitions"/></h2>
          <a class="btn btn-success pull-right" href="/oper/apps/${currentAppId}/new_partition?category=${currentCategory}">
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
              <th width="30%"><spring:message code="oper_partitions.text.name"/></th>
              <th width="30%"><spring:message code="oper_partitions.text.createdAt"/></th>
              <th width="20%"><spring:message code="oper_partitions.text.status"/></th>
              <th width="20%" style="text-align: right;"><spring:message code="oper_partitions.text.action"/></th>
            </tr>
            </thead>
            <tbody>
            <c:forEach items="${partitions}" var="partition">
              <tr class="partition-row" data-partition-id="${partition.id}">
                <td><a href="/oper/partitions/${partition.id}">${partition.name}</a></td>
                <td>${bm:dateToStr(pageContext, partition.created_at)}</td>
                <td>
                  <select style="width:110px;" class="partition-select">
                    <option value="0" ${partition.status le 0 ? 'selected' : ''}><spring:message code="oper_partitions.text.inactive"/></option>
                    <c:forEach begin="1" end="4" var="i">
                      <option value="${i}" ${partition.status eq i ? 'selected' : ''}>${i}</option>
                    </c:forEach>
                  </select>
                </td>
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
          TODO: no partition
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