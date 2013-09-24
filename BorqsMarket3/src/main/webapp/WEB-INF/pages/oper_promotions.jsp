<!DOCTYPE html>
<html lang="en">
<head>
<title><spring:message code="oper_promotions.page.title"/></title>
<%@include file="fragments/includeStyleAndScript.jspf" %>
<script src="/static/mustache/js/mustache.min.js"></script>
<script src="/static/jquery/js/jquery.tablednd.js"></script>
<style type="text/css">
  select.promotion-type-select {
    width: 100%;
  }

  .promotion-rank-text {
    font-weight: bolder;
    font-size: 2em;
    font-style: italic;
  }

  .promotion-target {
    width: 100%;
  }

  .promotion-name {
    width: 75%;
  }

  .promotion-logo-id {
    width: 100%;
  }

  .promotion-row {

  }

</style>
<script type="text/javascript">

  var availableProducts = ${bm:arrayToJson(availableProducts, false)};
  var availablePartitions = ${bm:arrayToJson(availablePartitions, false)};

  function resortPromotions() {
    $('.promotion-rank-text').each(function (i, rankElem) {
      $(rankElem).html((i + 1).toString());
    });
  }

  function setupPromotionsDnD() {
    $('#promotionsTable').tableDnD({
      onDrop: function () {
        resortPromotions();
      },
      dragHandle: '.promotion-drag-handle'
    });
  }

  function onDeletePromotion(btn) {
    $(btn).parents('tr.promotion-row').remove();
    resortPromotions();
  }

  function addPromotionRow() {
    var rowHtml = '<tr class="promotion-row">' +
        '<td><span class="promotion-rank-text"></span></td>' +
        '<td class="type-td"><select class="promotion-type-select"><option value="1">{{productLabel}}</option><option value="2">{{partitionLabel}}</option><option value="3">{{tagLabel}}</option><option value="4">{{userShareLabel}}</option><option value="5">{{sortLabel}}</option></select></td>' +
        '<td class="target-td"></td>' +
        '<td class="name-td"><input type="text" class="promotion-name" value=""></td>' +
        '<td class="logo-td"><input type="text" class="promotion-logo-id" value=""></td>' +
        '<td><button type="button" class="btn btn-danger delete-promotion-button" onclick="onDeletePromotion(this)"><i class="icon-white icon-remove"></i></button></td>' +
        '<td class="promotion-drag-handle"></td>' +
        '</tr>';


    $('#promotionsTable > tbody:last').append(Mustache.render(rowHtml, {
      productLabel: '${bm:springMessage(pageContext, "oper_promotions.label.product")}',
      partitionLabel: '${bm:springMessage(pageContext, "oper_promotions.label.partition")}',
      tagLabel: '${bm:springMessage(pageContext, "oper_promotions.label.byTag")}',
      userShareLabel: '${bm:springMessage(pageContext, "oper_promotions.label.userShare")}',
      sortLabel: '${bm:springMessage(pageContext, "oper_promotions.label.sort")}'
    }));
    var row = $('#promotionsTable tr:last').get(0);
    $(row).find("td.type-td select.promotion-type-select").change(function () {
      var type = getSelectedValue($(this));
      displayPromotionTarget(row, type, '', '', '');
    });

    //$(row).find('.fileupload').fileupload();
    return row;
  }

  function getSelectedValue(selectElem) {
    return $(selectElem).find('option:selected').val();
  }

  function setSelectedByValue(selectElem, val) {
    $(selectElem).find('option').filter(function () {
      return $(this).val() == val;
    }).prop('selected', true);
  }

  function displayPromotionTarget(row, type, target, name, logoImage) {
    $(row).find('td.name-td input.promotion-name').val(name);
    $(row).find('td.logo-td input.promotion-logo-id').val(logoImage);

    if (type == 1) {
      // products
      $(row).find('td.target-td').html(Mustache.render(
          '<select class="promotion-target">{{#availableProducts}}<option value="{{id}}">{{name}} - ({{id}})</option>{{/availableProducts}}</select>',
          {
            availableProducts: availableProducts
          }));
      if (target != '') {
        setSelectedByValue($(row).find('td.target-td .promotion-target'), target);
      }
    } else if (type == 2) {
      // partitions
      $(row).find('td.target-td').html(Mustache.render(
          '<select class="promotion-target">{{#availablePartitions}}<option value="{{id}}">{{name}} - ({{id}})</option>{{/availablePartitions}}</select>',
          {
            availablePartitions: availablePartitions
          }));
      if (target != '') {
        setSelectedByValue($(row).find('td.target-td .promotion-target'), target);
      }
    } else if (type == 3) {
      // tag
      $(row).find('td.target-td').html(Mustache.render(
          '<input type="text" class="promotion-target" value="{{target}}">',
          {
            target: target
          }));
    } else if (type == 4) {
      // user share
      $(row).find('td.target-td').html('');
    } else if (type == 5) {
      $(row).find('td.target-td').html(
          '<select class="promotion-target">' +
              '<option value="3">${bm:springMessage(pageContext, "oper_promotions.text.sortByDownloadCount")}</option>' +
              '<option value="1">${bm:springMessage(pageContext, "oper_promotions.text.sortByPurchaseCount")}</option>' +
              '<option value="2">${bm:springMessage(pageContext, "oper_promotions.text.sortByRating")}</option>' +
              '</select>');
      if (target != '') {
        setSelectedByValue($(row).find('td.target-td .promotion-target'), target);
      }
    }
  }

  function displayPromotion(row, promotion) {
    var type = promotion.type || 1;
    var target = promotion.target || '';
    var name = promotion.name || '';
    var logoImage = promotion.logo_image || '';

    setSelectedByValue($(row).find('td.type-td select.promotion-type-select'), type.toString());

    displayPromotionTarget(row, type, target, name, logoImage);
  }

  function addPromotion(promotion) {
    var lastRow = addPromotionRow();
    displayPromotion(lastRow, promotion);
  }

  $(function () {
    var promotions = ${bm:arrayToJson(promotions, false)};
    for (var i in promotions) {
      addPromotion(promotions[i]);
    }
    setupPromotionsDnD();
    resortPromotions();
  });


  $(function () {
    $('#addButton').click(function () {
      addPromotion({});
      resortPromotions();
      setupPromotionsDnD();
    });
  });

  function makePromotions() {
    var promotions = [];
    $('#promotionsTable').find('tbody tr').each(function (idx, row) {
      var type = parseInt(getSelectedValue($(row).find('td.type-td .promotion-type-select')));

      var target = '';
      if (type == 1 || type == 2 || type == 5) {
        target = getSelectedValue($(row).find('td.target-td .promotion-target'));
      } else if (type == 3) {
        target = $(row).find('td.target-td .promotion-target').val();
      }

      var name = $(row).find('td.name-td input.promotion-name').val();
      var logoId = $(row).find('td.logo-td input.promotion-logo-id').val();
      promotions.push({type: parseInt(type), target: $.trim(target), name: name, logo_image:logoId});
    });
    return promotions;
  }

  function savePromotions() {
    var promotions = $.toJSON(makePromotions());
    var form = $('#promotionsForm');
    form.find('#promotions').val(promotions);
    form.submit();
  }

  $(function () {
    $('#saveButton').click(function () {
      savePromotions();
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
      <bm:operAppFeaturesTag currentFeature="managePromotions" currentCategory="${currentCategory}"
                             appId="${currentAppId}"/>
      <ul class="nav nav-pills">
        <c:forEach items="${categories}" var="category">
          <li class="${currentCategory eq category.category_id ? 'active' : ''}">
            <a href="/oper/apps/${currentAppId}/promotions?category=${category.category_id}">
              <strong>${category.name}</strong>
            </a>
          </li>
        </c:forEach>
      </ul>
      <hr>
      <p class="text-info"><spring:message code="oper_promotions.text.dragHint"/></p>

      <div class="row-fluid">
        <table id="promotionsTable" class="table" style="margin-top:30px;">
          <thead>
          <tr>
            <th width="8%"><spring:message code="oper_promotions.label.promotionRank"/></th>
            <th width="15%"><spring:message code="oper_promotions.label.promotionType"/></th>
            <th width="30%"><spring:message code="oper_promotions.label.promotionTarget"/></th>
            <th width="20%"><spring:message code="oper_promotions.label.promotionName"/></th>
            <th width="20%">Logo</th>
            <th width="5%"><spring:message code="oper_promotions.label.promotionAction"/></th>
            <th width="2%"></th>
          </tr>
          </thead>
          <tbody>
          </tbody>
        </table>
        <hr>
        <div class="row-fluid">
          <div class="span12">
            <button id="addButton" type="button" class="pull-left btn btn-info"><i
                class="icon-plus icon-white"></i>&nbsp;<spring:message code="oper_promotions.button.addPromotion"/>
            </button>
            <button id="saveButton" type="button" class="pull-right btn btn-success"><i
                class="icon-ok icon-white"></i>&nbsp;<spring:message code="oper_promotions.button.savePromotion"/>
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
  <form id="promotionsForm" method="POST" style="display: none;">
    <input type="hidden" id="promotions" name=promotions value=""/>
  </form>
</div>
<bm:bottomFooter/>
</body>
</html>