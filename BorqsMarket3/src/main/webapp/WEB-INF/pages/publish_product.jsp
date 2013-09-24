<!DOCTYPE html>
<html lang="en">
<head>
  <title><spring:message code="publish_product.page.title"/></title>
  <%@include file="fragments/includeStyleAndScript.jspf" %>
  <style>
    #version_menu li {
      height: 2em;
      margin-top: 5px;
      margin-bottom: 5px;
    }
  </style>
  <script type="text/javascript">
    <c:if test="${showActiveVersionPrompt == true && !bm:versionIsActive(version['status'])}">
    $(function () {
      var activeVersionButton = $('#activeVersionButton');
      activeVersionButton.popover({
        placement: 'left',
        content: '<button class="close" onclick=\'$("#activeVersionButton").popover("hide");\'>×</button>' +
            '<br><p><strong><spring:message code="publish_product.text.activeVersionPrompt"/></strong></p>',
        trigger: 'manual',
        html: true
      });
      activeVersionButton.popover('show');
    });
    </c:if>
  </script>
</head>
<body>
<bm:topNavigationBar module="publish"/>
<bm:secondNavigationBar module="publish" currentAt="apps/${product['app_id']}/${product['id']}"/>
<div class="container">
<div class="row">
<div class="span2 affix left-bar">
  <bm:appNavigationBar module="publish"/>
</div>
<div class="span2 offset10 affix">

  <ul id="actionList">
    <li>
      <a class="btn btn-warning" href="/publish/products/${product['id']}/price?version=${version['version']}">
        <i class="icon-white icon-tag"></i>
        <strong><spring:message code="publish_product.button.changePrice"/></strong>
      </a>
    </li>

    <li>
      <a class="btn btn-warning" href="/publish/products/${product['id']}/publish_channel?version=${version['version']}">
        <i class="icon-white icon-random"></i>
        <strong><spring:message code="publish_product.button.changePublishChannels"/></strong>
      </a>
    </li>

    <li>
      <a class="btn btn-success" href="/publish/products/${product['id']}/upload">
        <i class="icon-white icon-plus"></i>
        <strong><spring:message code="publish_product.button.newVersion"/></strong>
      </a>
    </li>

    <li>
      <a class="btn btn-info" href="${version['url']}" download="${version['url_filename']}">
        <i class="icon-white icon-download"></i>
        <strong><spring:message code="publish_product.button.downloadVersion"/></strong>
      </a>
    </li>

    <li>
      <c:choose>
        <c:when test="${bm:versionIsActive(version['status'])}">
          <c:set var="activeBtnClass" value="btn-danger"/>
          <c:set var="activeBtnText">
            <spring:message code="publish_product.button.unpublishVersion"/>
          </c:set>
          <c:set var="activeVersionValue" value="0"/>
        </c:when>
        <c:otherwise>
          <c:set var="activeBtnClass" value="btn-success"/>
          <c:set var="activeBtnText">
            <spring:message code="publish_product.button.activeVersion"/>
          </c:set>
          <c:set var="activeVersionValue" value="1"/>
        </c:otherwise>
      </c:choose>
      <a id="activeVersionButton" class="btn ${activeBtnClass}"
         href="/publish/products/${product['id']}/${version['version']}/active?active=${activeVersionValue}">
        <i class="icon-white icon-off"></i>
        <strong>${activeBtnText}</strong>
      </a>
    </li>

    <c:if test="${version['beta'] != 0}">
      <li>
        <a id="releaseVersionButton" class="btn btn-success"
           href="/publish/products/${product['id']}/${version['version']}/release">
          <i class="icon-white icon-ok-circle"></i>
          <strong><spring:message code="publish_product.button.releaseVersion"/></strong>
        </a>
      </li>
    </c:if>
  </ul>
  <hr>
  <ul id="version_menu" class="nav nav-list">
    <li class="nav-header"><spring:message code="publish_product.title.versions"/></li>
    <c:forEach items="${product['versions']}" var="v">
      <c:choose>
        <c:when test="${v['version'] == version['version']}">
          <c:set var="li_class" value="active"/>
          <c:set var="icon_class" value="icon-white"/>
        </c:when>
        <c:otherwise>
          <c:set var="li_class" value=""/>
          <c:set var="icon_class" value=""/>
        </c:otherwise>
      </c:choose>
      <li class="${li_class}">
        <bm:displayVersion product="${product['id']}" version="${v['version']}" versionName="${v['version_name']}"
                           versionStatus="${v['status']}" beta="${v['beta']}"/>
      </li>
    </c:forEach>
  </ul>
</div>
<div class="span8 offset2" id="content">

<bm:publishProductFeatures productId="${product['id']}" currentFeature="info" version="${version['version']}"/>

<c:if test="${bm:isCheckResultError(product) || bm:isCheckResultError(version)}">
  <div class="alert alert-error">
    <spring:message code="publish_product.text.updateError"/>
  </div>
</c:if>
<form method="POST" class="form-horizontal" enctype="multipart/form-data" accept-charset="UTF-8">

<fieldset>
<div class="row-fluid">
  <table class="span12" style="display: table;">
    <tr>
      <td style="width: 40%">
        <h2><spring:message code="publish_product.title.productInfo"/></h2>
      </td>
      <td style="width: 60%; text-align: right;">
        <span>
          <strong>${product['id']}</strong>
          <bm:displayVersion product="${product['id']}" version="${version['version']}" versionName="${version['version_name_'.concat(bm:locale(pageContext))]}"
                             versionStatus="${version['status']}" beta="${version['beta']}"/>
        </span>
      </td>
    </tr>
  </table>
</div>
<hr class="hr2">
<div class="row-fluid">
  <bm:textInput id="id" name="id" label="${bm:springMessage(pageContext, 'publish_product.label.id')}"
                value="${product['id']}" readonly="true" inputSpan="span12"/>
</div>
<div class="row-fluid">
  <bm:textInput id="app_id" name="app_id"
                label="${bm:springMessage(pageContext, 'publish_product.label.appID')}"
                value="${product['app_id']}" readonly="true" inputSpan="span12"/>
</div>

<div class="row-fluid">
  <bm:textInput id="category_id" name="category_id"
                label="${bm:springMessage(pageContext, 'publish_product.label.categoryID')}"
                value="${product['category_id']}" readonly="true" inputSpan="span12"/>
</div>
<hr>

<div class="row-fluid">
  <bm:multipleLocalePanel id="product" var="locale">
    <bm:textInput id="name" name="name"
                  label="${bm:springMessage(pageContext, 'publish_product.label.name')}"
                  value="${product['name_'.concat(locale)]}"
                  errorMessage="${product['name_errorMessage']}" readonly="false"
                  locale="${locale}" inputSpan="span12" required="true"/>
    <bm:textAreaInput id="description" name="description"
                      label="${bm:springMessage(pageContext, 'publish_product.label.description')}"
                      value="${product['description_'.concat(locale)]}"
                      errorMessage="${product['description_errorMessage']}" readonly="false"
                      locale="${locale}" rows="5" inputSpan="span12"/>
  </bm:multipleLocalePanel>
</div>

<hr>
<div class="row-fluid">
  <bm:tagsInput id="tags" name="tags"
                label="${bm:springMessage(pageContext, 'publish_product.label.tags')}"
                value="${product['tags']}" errorMessage="${product['tags_errorMessage']}"
                readonly="false" inputSpan="span12" required="false" allowFreeTags="true"
                typeAhead="${product['available_tags']}"/>
</div>

<hr>
<div class="row-fluid">
  <bm:textInput id="author_name" name="author_name"
                label="${bm:springMessage(pageContext, 'publish_product.label.authorName')}"
                value="${product['author_name']}" errorMessage="${product['author_name_errorMessage']}"
                readonly="false" inputSpan="span12" required="true"/>
</div>
<div class="row-fluid">
  <bm:textInput id="author_email" name="author_email"
                label="${bm:springMessage(pageContext, 'publish_product.label.authorEmail')}"
                value="${product['author_email']}"
                errorMessage="${product['author_email_errorMessage']}" readonly="false" inputSpan="span12"/>
</div>
<div class="row-fluid">
  <bm:textInput id="author_phone" name="author_phone"
                label="${bm:springMessage(pageContext, 'publish_product.label.authorPhone')}"
                value="${product['author_phone']}"
                errorMessage="${product['author_phone_errorMessage']}" readonly="false" inputSpan="span12"/>
</div>
<div class="row-fluid">
  <bm:textInput id="author_website" name="author_website"
                label="${bm:springMessage(pageContext, 'publish_product.label.authorWebsite')}"
                value="${product['author_website']}"
                errorMessage="${product['author_website_errorMessage']}" readonly="false" inputSpan="span12"/>
</div>
<hr>

<div class="row-fluid well" style="box-sizing: border-box;">
  <ul class="thumbnails">
    <li class="span4">
      <label><strong><spring:message code="publish_product.label.logo"/>&nbsp;<span
          class="text-info">*</span></strong></label>

      <p>
        Type: jpg/png<br>
        size:TODO
      </p>
      <bm:imageUpload id="logo_image"
                      style="width: 180px; height: 180px;" src="${product['logo_image']}"/>
      <p class="text-error">${product['logo_image_errorMessage']}</p>
    </li>
    <li class="span3">
      <label><strong><spring:message code="publish_product.label.cover"/>&nbsp;<span
          class="text-info">*</span></strong></label>

      <p>
        Type: jpg/png<br>
        size:TODO
      </p>
      <bm:imageUpload id="cover_image"
                      style="width: 100px; height: 180px;" src="${product['cover_image']}"/>
      <p class="text-error">${product['cover_image_errorMessage']}</p>
    </li>
    <li class="span5">
      <label><strong><spring:message code="publish_product.label.promotion"/></strong></label>

      <p>
        Type: jpg/png<br>
        size:TODO
      </p>
      <bm:imageUpload id="promotion_image"
                      style="width: 200px; height: 180px;" src="${product['promotion_image']}"/>
      <p class="text-error">${product['promotion_image_errorMessage']}</p>
    </li>
  </ul>
</div>
<div class="row-fluid well" style="box-sizing: border-box;">
  <label><strong><spring:message code="publish_product.label.screenshots"/></strong></label>

  <p>
    Type: jpg/png
  </p>
  <ul class="thumbnails">
    <li class="span2">
      <bm:imageUpload id="screenshot1_image"
                      style="width: 90px; height: 180px;"
                      src="${product['screenshot1_image']}"/>
      <p class="text-error">${product['screenshot1_image_errorMessage']}</p>
    </li>
    <li class="span2">
      <bm:imageUpload id="screenshot2_image"
                      style="width: 90px; height: 180px;"
                      src="${product['screenshot2_image']}"/>
      <p class="text-error">${product['screenshot2_image_errorMessage']}</p>
    </li>
    <li class="span2">
      <bm:imageUpload id="screenshot3_image"
                      style="width: 90px; height: 180px;"
                      src="${product['screenshot3_image']}"/>
      <p class="text-error">${product['screenshot3_image_errorMessage']}</p>
    </li>
    <li class="span2">
      <bm:imageUpload id="screenshot4_image"
                      style="width: 90px; height: 180px;"
                      src="${product['screenshot4_image']}"/>
      <p class="text-error">${product['screenshot4_image_errorMessage']}</p>
    </li>
    <li class="span2">
      <bm:imageUpload id="screenshot5_image"
                      style="width: 90px; height: 180px;"
                      src="${product['screenshot5_image']}"/>
      <p class="text-error">${product['screenshot5_image_errorMessage']}</p>
    </li>
  </ul>
</div>

<hr>
<h2><spring:message code="publish_product.title.versionInfo"/></h2>
<hr class="hr2">
<div class="row-fluid">
  <bm:textInput id="version" name="version"
                label="${bm:springMessage(pageContext, 'publish_product.label.version')}"
                value="${version['version']}"
                readonly="true" inputSpan="span12"/>
</div>

<hr>
<div class="row-fluid">
  <bm:textInput id="min_app_version" name="min_app_version"
                label="${bm:springMessage(pageContext, 'publish_product.label.minAppVersion')}"
                value="${bm:formatMinAppVersion(version['min_app_version'])}" readonly="true" inputSpan="span12"/>
</div>

<div class="row-fluid">
  <bm:textInput id="max_app_version" name="max_app_version"
                label="${bm:springMessage(pageContext, 'publish_product.label.maxAppVersion')}"
                value="${bm:formatMaxAppVersion(version['max_app_version'])}" readonly="true" inputSpan="span12"/>
</div>

<div class="row-fluid">
  <bm:tagsInput id="supported_mod" name="supported_mod"
                label="${bm:springMessage(pageContext, 'publish_product.label.supportedMod')}"
                value="${bm:formatSupportedMod(version['supported_mod'])}" readonly="true"
                inputSpan="span12" typeAhead="${version['available_mods']}" allowFreeTags="false"/>
</div>
<hr>


<div class="row-fluid">
  <bm:multipleLocalePanel id="version" var="locale">
    <bm:textInput id="version_name" name="version_name"
                  label="${bm:springMessage(pageContext, 'publish_product.label.versionName')}"
                  value="${version['version_name_'.concat(locale)]}"
                  errorMessage="${version['name_errorMessage']}" readonly="false" locale="${locale}"
                  inputSpan="span12" required="true"/>
    <bm:textAreaInput id="recent_change" name="recent_change"
                      label="${bm:springMessage(pageContext, 'publish_product.label.recentChange')}"
                      value="${version['recent_change_'.concat(locale)]}"
                      errorMessage="${version['description_errorMessage']}" readonly="false"
                      locale="${locale}" rows="5" inputSpan="span12"/>
  </bm:multipleLocalePanel>
</div>

<hr class="hr2">
<div class="row-fluid">
  <button type="submit" class="btn btn-large btn-primary offset3 span3 submit-button"><i
      class="icon-ok icon-white"></i> <spring:message code="publish_product.button.save"/>
  </button>
  <button type="reset"
          class="btn btn-large span3 submit-button">${bm:springMessage(pageContext, 'publish_product.button.reset')}</button>
</div>
</fieldset>
</form>
</div>
</div>
</div>
<bm:bottomFooter/>
</body>
</html>