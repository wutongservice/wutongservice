<!DOCTYPE html>
<html lang="en">
<head>
  <title>${mode eq 'create' ? bm:springMessage(pageContext, 'oper_partition.page.title.create') : bm:springMessage(pageContext, 'oper_partition.page.title.info')}</title>
  <%@include file="fragments/includeStyleAndScript.jspf" %>
  <style type="text/css">
    <c:if test="${mode != 'create'}">
    .modification {
      display: none;
    }

    </c:if>
  </style>
  <script type="text/javascript">
    <c:if test="${mode != 'create'}">
    $(function () {
      $('#toggleModificationButton').click(function () {
        if ($(this).hasClass('active')) {
          $('.modification').hide();
          $.removeCookie('oper.partition.displayInfo');
        } else {
          $('.modification').show();
          $.cookie('oper.partition.displayInfo', "1");
        }
      });
      <c:if test="${displayInfo == true}">
      $('#toggleModificationButton').click();
      </c:if>
    });

    function makeProductIds() {
      return $('#productsBox').val();
    }

    $(function () {
      $('#submitBtn').click(function () {
        var productIds = makeProductIds();
        $('#products').val(productIds);
        $('#partitionForm').submit();
      });
    });

    </c:if>
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
    <div class="span2 offset10 affix">

      <ul id="actionList">
        <li>
          <a class="btn btn-danger" href="/oper/delete_partition?id=${partition.id}">
            <i class="icon-white icon-remove"></i>&nbsp;
            <strong><spring:message code="oper_partition.button.delete"/></strong>
          </a>
        </li>
      </ul>
    </div>
    <div class="span8 offset2" id="content">
      <c:if test="${bm:isCheckResultError(partition)}">
        <div class="alert alert-error">
          <spring:message code="oper_partition.text.updateError"/>
        </div>
      </c:if>
      <form id="partitionForm" method="POST" class="form-horizontal" enctype="multipart/form-data"
            accept-charset="UTF-8">

        <fieldset>
          <div class="row-fluid">
            <div class="span12">

              <h2 class="">${mode eq 'create' ? bm:springMessage(pageContext, "oper_partition.title.createPartition") : bm:springMessage(pageContext, 'oper_partition.title.partition').concat(partitionDisplayName)}</h2>

              <c:if test="${mode != 'create'}">
                <button id="toggleModificationButton" type="button" class="btn btn-primary"
                        data-toggle="button"><spring:message code="oper_partition.button.editInfo"/>
                </button>
              </c:if>
            </div>
          </div>
          <hr class="hr2">
          <div class="row-fluid modification">
            <bm:multipleLocalePanel id="product" var="locale">
              <bm:textInput id="name" name="name"
                            label="${bm:springMessage(pageContext, 'oper_partition.label.name')}"
                            value="${partition['name_'.concat(locale)]}"
                            errorMessage="${partition['name_errorMessage']}" readonly="false"
                            locale="${locale}" inputSpan="span12" required="true"/>
              <bm:textAreaInput id="description" name="description"
                                label="${bm:springMessage(pageContext, 'oper_partition.label.description')}"
                                value="${partition['description_'.concat(locale)]}"
                                errorMessage="${partition['description_errorMessage']}" readonly="false"
                                locale="${locale}" rows="5" inputSpan="span12"/>
            </bm:multipleLocalePanel>
          </div>

          <hr class="modification">

          <div class="row-fluid well modification" style="box-sizing: border-box;">
            <ul class="thumbnails">
              <li class="span4">
                <label><strong>${bm:springMessage(pageContext, 'oper_partition.label.logo')}&nbsp;<span
                    class="text-info">*</span></strong></label>

                <p>
                  Type: jpg/png<br>
                  size:TODO
                </p>
                <bm:imageUpload id="logo_image"
                                style="width: 180px; height: 180px;" src="${partition['logo_image']}"/>
                <p class="text-error">${partition['logo_image_errorMessage']}</p>
              </li>
            </ul>
          </div>

          <c:if test="${mode != 'create'}">
            <hr class="hr2 modification">
            <div class="row-fluid">
              <label for="productsBox">Recommend product ID list</label>
              <c:set var="NL" value="
"/>
              <textarea id="productsBox" rows="10" class="span12"
                        placeholder="input partition id">${fn:replace(partition.list, ',', NL)}</textarea>
            </div>
            <input type="hidden" id="products" name="products" value=""/>
          </c:if>
          <div class="row-fluid">
            <button type="submit" id="submitBtn" class="btn btn-large btn-primary offset3 span3 submit-button"><i
                class="icon-ok icon-white"></i> ${mode eq 'create' ? bm:springMessage(pageContext, 'oper_partition.button.create') : bm:springMessage(pageContext, 'oper_partition.button.save')}
            </button>
            <button type="reset"
                    class="btn btn-large span3 submit-button"><spring:message code="oper_partition.button.reset"/>
            </button>
          </div>
        </fieldset>
      </form>

    </div>
  </div>
</div>
<bm:bottomFooter/>
</body>
</html>