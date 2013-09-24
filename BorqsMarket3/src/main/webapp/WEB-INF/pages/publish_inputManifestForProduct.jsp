<!DOCTYPE html>
<html lang="en">
<head>
  <title><spring:message code="publish_inputManifestForProduct.page.title"/></title>
  <%@include file="fragments/includeStyleAndScript.jspf" %>
  <style>
    #version_menu li {
      height: 2em;
      margin-top: 5px;
      margin-bottom: 5px;
    }

    ul#actionList {
      list-style: none;
      margin: 0;
    }

    ul#actionList > li {
      margin-top: 10px;
      margin-bottom: 10px;
    }

    ul#actionList > li > a {
      display: block;
    }
  </style>
</head>
<body>
<bm:topNavigationBar module="publish"/>
<bm:secondNavigationBar module="publish" currentAt="apps/${result['app_id']}/${result['id']}"/>
<div class="container">
<div class="row">
<div class="span2 affix left-bar">
  <bm:appNavigationBar module="publish"/>
</div>
<div class="span8 offset2" id="content">
<h1 style="text-align: center;margin-bottom: 40px;"><spring:message
    code="publish_inputManifestForProduct.title.fillManifest"/></h1>

<div class="alert alert-info">
  <spring:message code="publish_inputManifestForProduct.text.reason"/>
</div>

<c:if test="${bm:isCheckResultError(result)}">
  <div class="alert alert-error">
    <spring:message code="publish_inputManifestForProduct.text.inputError"/>
  </div>
</c:if>


<form method="POST" class="form-horizontal" enctype="multipart/form-data" accept-charset="UTF-8"
      action="/publish/apps/${result['app_id']}/upload/fill_manifest?category=${result['category_id']}">

<fieldset>
<input type="hidden" name="fileStatus" value="${fileStatus}"/>

<h2><spring:message code="publish_product.title.productInfo"/></h2>

<hr class="hr2">

<div class="row-fluid">

  <div class="row-fluid">
    <bm:textInput id="id" name="id" label="${bm:springMessage(pageContext, 'publish_product.label.id')}"
                  value="${result['id']}"
                  errorMessage="${result['id_errorMessage']}"
                  inputSpan="span12"
                  required="true"/>
  </div>

  <c:if test="${result['category_id'] == ''}">
    <div class="control-group ${controlGroupClass}">
      <label class="control-label"><strong><spring:message code="publish_product.label.categoryID"/>&nbsp;
        <span class="text-info">*</span></strong></label>
      <div class="controls ${controlsSpan}">
        <select type="text" id="category_id" name="category_id" class="span12">
          <c:forEach items="${categories}" var="categoryItem">
            <option
                value="${categoryItem['category_id']}" ${categoryItem['category_id'] eq result['category_id'] ? 'selected' : ''}>
                ${categoryItem['name']} (${categoryItem['category_id']})
            </option>
          </c:forEach>
        </select>
        <span class="help-block">${result['category_id_errorMessage']}</span>
      </div>
    </div>
  </c:if>
  <c:if test="${result['category_id'] != ''}">
    <div class="row-fluid">
      <bm:textInput id="category_id" name="category_id"
                    label="${bm:springMessage(pageContext, 'publish_product.label.categoryID')}"
                    value="${result['category_id']}" readonly="true" inputSpan="span12"/>
    </div>
  </c:if>
</div>

<hr>

<div class="row-fluid">
  <bm:multipleLocalePanel id="product" var="locale">
    <bm:textInput id="name" name="name"
                  label="${bm:springMessage(pageContext, 'publish_product.label.name')}"
                  value="${result['name_'.concat(locale)]}"
                  errorMessage="${result['name_errorMessage']}" readonly="false"
                  locale="${locale}" inputSpan="span12" required="true"/>
    <bm:textAreaInput id="description" name="description"
                      label="${bm:springMessage(pageContext, 'publish_product.label.description')}"
                      value="${result['description_'.concat(locale)]}"
                      errorMessage="${result['description_errorMessage']}" readonly="false"
                      locale="${locale}" rows="5" inputSpan="span12"/>
  </bm:multipleLocalePanel>
</div>

<hr>
<div class="row-fluid">
  <bm:tagsInput id="tags" name="tags"
                label="${bm:springMessage(pageContext, 'publish_product.label.tags')}"
                value="${result['tags']}" errorMessage="${result['tags_errorMessage']}"
                readonly="false" inputSpan="span12" required="false" allowFreeTags="true"
                typeAhead="${result['available_tags']}"/>
</div>

<hr>
<div class="row-fluid">
  <bm:textInput id="author_name" name="author_name"
                label="${bm:springMessage(pageContext, 'publish_product.label.authorName')}"
                value="${result['author_name']}" errorMessage="${result['author_name_errorMessage']}"
                readonly="false" inputSpan="span12" required="true"/>
</div>
<div class="row-fluid">
  <bm:textInput id="author_email" name="author_email"
                label="${bm:springMessage(pageContext, 'publish_product.label.authorEmail')}"
                value="${result['author_email']}"
                errorMessage="${result['author_email_errorMessage']}" readonly="false" inputSpan="span12"/>
</div>
<div class="row-fluid">
  <bm:textInput id="author_phone" name="author_phone"
                label="${bm:springMessage(pageContext, 'publish_product.label.authorPhone')}"
                value="${result['author_phone']}"
                errorMessage="${result['author_phone_errorMessage']}" readonly="false" inputSpan="span12"/>
</div>
<div class="row-fluid">
  <bm:textInput id="author_website" name="author_website"
                label="${bm:springMessage(pageContext, 'publish_product.label.authorWebsite')}"
                value="${result['author_website']}"
                errorMessage="${result['author_website_errorMessage']}" readonly="false" inputSpan="span12"/>
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
                      style="width: 180px; height: 180px;" src=""/>
      <p class="text-error">${result['logo_image_errorMessage']}</p>
    </li>
    <li class="span3">
      <label><strong><spring:message code="publish_product.label.cover"/>&nbsp;<span
          class="text-info">*</span></strong></label>

      <p>
        Type: jpg/png<br>
        size:TODO
      </p>
      <bm:imageUpload id="cover_image"
                      style="width: 100px; height: 180px;" src=""/>
      <p class="text-error">${result['cover_image_errorMessage']}</p>
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
                      src=""/>
      <p class="text-error">${result['screenshot1_image_errorMessage']}</p>
    </li>
    <li class="span2">
      <bm:imageUpload id="screenshot2_image"
                      style="width: 90px; height: 180px;"
                      src=""/>
      <p class="text-error">${result['screenshot2_image_errorMessage']}</p>
    </li>
    <li class="span2">
      <bm:imageUpload id="screenshot3_image"
                      style="width: 90px; height: 180px;"
                      src=""/>
      <p class="text-error">${result['screenshot3_image_errorMessage']}</p>
    </li>
    <li class="span2">
      <bm:imageUpload id="screenshot4_image"
                      style="width: 90px; height: 180px;"
                      src=""/>
      <p class="text-error">${result['screenshot4_image_errorMessage']}</p>
    </li>
    <li class="span2">
      <bm:imageUpload id="screenshot5_image"
                      style="width: 90px; height: 180px;"
                      src=""/>
      <p class="text-error">${result['screenshot5_image_errorMessage']}</p>
    </li>
  </ul>
</div>

<hr>
<h2><spring:message code="publish_product.title.versionInfo"/></h2>
<hr class="hr2">
<div class="row-fluid">
  <bm:textInput id="version" name="version"
                label="${bm:springMessage(pageContext, 'publish_product.label.version')}"
                value="${result['version']}"
                errorMessage="${result['version_errorMessage']}"
                required="true"
                inputSpan="span12"/>
</div>

<hr>
<div class="row-fluid">
  <bm:textInput id="min_app_version" name="min_app_version"
                label="${bm:springMessage(pageContext, 'publish_product.label.minAppVersion')}"
                value="${bm:formatMinAppVersion(result['min_app_version'])}"
                errorMessage="${result['min_app_version_errorMessage']}"
                inputSpan="span12"/>
</div>

<div class="row-fluid">
  <bm:textInput id="max_app_version" name="max_app_version"
                label="${bm:springMessage(pageContext, 'publish_product.label.maxAppVersion')}"
                value="${bm:formatMaxAppVersion(result['max_app_version'])}"
                errorMessage="${result['max_app_version_errorMessage']}"
                inputSpan="span12"/>
</div>

<div class="row-fluid">
  <bm:tagsInput id="supported_mod" name="supported_mod"
                label="${bm:springMessage(pageContext, 'publish_product.label.supportedMod')}"
                errorMessage="${result['supported_mod_errorMessage']}"
                value="${bm:formatSupportedMod(result['supported_mod'])}" readonly="false"
                inputSpan="span12" typeAhead="${result['available_mods']}" allowFreeTags="false"/>
</div>
<hr>


<div class="row-fluid">
  <bm:multipleLocalePanel id="version" var="locale">
    <bm:textInput id="version_name" name="version_name"
                  label="${bm:springMessage(pageContext, 'publish_product.label.versionName')}"
                  value="${result['version_name_'.concat(locale)]}"
                  errorMessage="${result['name_errorMessage']}" readonly="false" locale="${locale}"
                  inputSpan="span12" required="true"/>
    <bm:textAreaInput id="recent_change" name="recent_change"
                      label="${bm:springMessage(pageContext, 'publish_product.label.recentChange')}"
                      value="${result['recent_change_'.concat(locale)]}"
                      errorMessage="${result['description_errorMessage']}" readonly="false"
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
<input type="hidden" value="${asBeta}" name="as_beta"/>
</fieldset>
</form>
</div>
</div>
</div>
<bm:bottomFooter/>
</body>
</html>