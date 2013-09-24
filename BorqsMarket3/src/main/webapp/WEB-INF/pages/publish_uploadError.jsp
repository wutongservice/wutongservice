<!DOCTYPE html>
<html lang="en">
<head>
  <title><spring:message code="publish_uploadError.page.title"/></title>
  <%@include file="fragments/includeStyleAndScript.jspf" %>
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
      <c:if test="${bm:isCheckResultError(result)}">
        <div class="alert alert-error">
          <spring:message code="publish_uploadError.text.uploadError"/>
        </div>
      </c:if>
      <form class="form-horizontal">
        <fieldset>
          <h2><spring:message code="publish_product.title.productInfo"/></h2>
          <hr class="hr2">

          <div class="row-fluid">
            <bm:textInput id="id" name="id" label="${bm:springMessage(pageContext, 'publish_product.label.id')}"
                          value="${result['id']}" readonly="true" inputSpan="span12"
                          errorMessage="${result['id_errorMessage']}" required="true"/>
          </div>
          <div class="row-fluid">
            <bm:textInput id="app_id" name="app_id"
                          label="${bm:springMessage(pageContext, 'publish_product.label.appID')}"
                          value="${result['app_id']}" readonly="true" inputSpan="span12"
                          errorMessage="${result['app_id_errorMessage']}" required="true"/>
          </div>

          <div class="row-fluid">
            <bm:textInput id="category_id" name="category_id"
                          label="${bm:springMessage(pageContext, 'publish_product.label.categoryID')}"
                          value="${result['category_id']}" readonly="true" inputSpan="span12"
                          errorMessage="${result['category_id_errorMessage']}" required="true"/>
          </div>
          <hr>

          <div class="row-fluid">
            <bm:multipleLocalePanel id="product" var="locale">
              <bm:textInput id="name" name="name" label="${bm:springMessage(pageContext, 'publish_product.label.name')}"
                            value="${result['name_'.concat(locale)]}"
                            errorMessage="${result['name_errorMessage']}" readonly="true" locale="${locale}"
                            inputSpan="span12" required="true"/>
              <bm:textAreaInput id="description"
                                name="${bm:springMessage(pageContext, 'publish_product.label.description')}"
                                label="Description"
                                value="${result['description_'.concat(locale)]}"
                                errorMessage="${result['description_errorMessage']}" readonly="true"
                                locale="${locale}" rows="5" inputSpan="span12"/>
            </bm:multipleLocalePanel>
          </div>
          <hr>

          <div class="row-fluid">
            <bm:textInput id="author_name" name="author_name"
                          label="${bm:springMessage(pageContext, 'publish_product.label.authorName')}"
                          value="${result['author_name']}"
                          errorMessage="${result['author_name_errorMessage']}" readonly="true" inputSpan="span12"
                          required="true"/>
          </div>
          <div class="row-fluid">
            <bm:textInput id="author_email" name="author_email"
                          label="${bm:springMessage(pageContext, 'publish_product.label.authorEmail')}"
                          value="${result['author_email']}"
                          errorMessage="${result['author_email_errorMessage']}" readonly="true" inputSpan="span12"/>
          </div>
          <div class="row-fluid">
            <bm:textInput id="author_phone" name="author_phone"
                          label="${bm:springMessage(pageContext, 'publish_product.label.authorPhone')}"
                          value="${result['author_phone']}"
                          errorMessage="${result['author_phone_errorMessage']}" readonly="true" inputSpan="span12"/>

          </div>
          <div class="row-fluid">
            <bm:textInput id="author_website" name="author_website"
                          label="${bm:springMessage(pageContext, 'publish_product.label.authorWebsite')}"
                          value="${result['author_website']}"
                          errorMessage="${result['author_website_errorMessage']}" readonly="true" inputSpan="span12"/>
          </div>
          <hr>

          <div class="row-fluid">
            <bm:textInput id="logo_image" name="logo_image"
                          label="${bm:springMessage(pageContext, 'publish_product.label.logo')}"
                          value="${result['logo_image']}"
                          errorMessage="${result['logo_image_errorMessage']}" readonly="true" inputSpan="span12"
                          required="true"/>
          </div>
          <div class='row-fluid'>
            <bm:textInput id="cover_image" name="cover_image"
                          label="${bm:springMessage(pageContext, 'publish_product.label.cover')}"
                          value="${result['cover_image']}"
                          errorMessage="${result['cover_image_errorMessage']}" readonly="true" inputSpan="span12"
                          required="true"/>
          </div>
          <div class="row-fluid">
            <bm:textInput id="screenshot1_image" name="screenshot1_image"
                          label="${bm:springMessage(pageContext, 'publish_uploadError.label.screenshot1')}"
                          value="${result['screenshot1_image']}"
                          errorMessage="${result['screenshot1_image_errorMessage']}" readonly="true"
                          inputSpan="span12"/>
          </div>
          <div class="row-fluid">
            <bm:textInput id="screenshot2_image" name="screenshot2_image"
                          label="${bm:springMessage(pageContext, 'publish_uploadError.label.screenshot2')}"
                          value="${result['screenshot2_image']}"
                          errorMessage="${result['screenshot2_image_errorMessage']}" readonly="true"
                          inputSpan="span12"/>
          </div>
          <div class="row-fluid">
            <bm:textInput id="screenshot3_image" name="screenshot3_image"
                          label="${bm:springMessage(pageContext, 'publish_uploadError.label.screenshot3')}"
                          value="${result['screenshot3_image']}"
                          errorMessage="${result['screenshot3_image_errorMessage']}" readonly="true"
                          inputSpan="span12"/>
          </div>
          <div class="row-fluid">
            <bm:textInput id="screenshot4_image" name="screenshot4_image"
                          label="${bm:springMessage(pageContext, 'publish_uploadError.label.screenshot4')}"
                          value="${result['screenshot4_image']}"
                          errorMessage="${result['screenshot4_image_errorMessage']}" readonly="true"
                          inputSpan="span12"/>
          </div>
          <div class="row-fluid">
            <bm:textInput id="screenshot5_image" name="screenshot5_image"
                          label="${bm:springMessage(pageContext, 'publish_uploadError.label.screenshot5')}"
                          value="${result['screenshot5_image']}"
                          errorMessage="${result['screenshot5_image_errorMessage']}" readonly="true"
                          inputSpan="span12"/>
          </div>
          <hr>
          <h2><spring:message code="publish_product.title.versionInfo"/></h2>
          <hr class="hr2">

          <div class="row-fluid">
            <bm:textInput id="version" name="version"
                          label="${bm:springMessage(pageContext, 'publish_product.label.version')}"
                          errorMessage="${result['version_errorMessage']}"
                          value="${result['version']}" readonly="true" inputSpan="span12" required="true"/>
          </div>
          <hr>

          <div class="row-fluid">
            <bm:textInput id="min_app_version" name="min_app_version"
                          label="${bm:springMessage(pageContext, 'publish_product.label.minAppVersion')}"
                          value="${bm:formatMinAppVersion(result['min_app_version'])}"
                          errorMessage="${result['min_app_version_errorMessage']}" readonly="true" inputSpan="span12"
                          required="true"/>
          </div>
          <div class="row-fluid">
            <bm:textInput id="max_app_version" name="max_app_version"
                          label="${bm:springMessage(pageContext, 'publish_product.label.maxAppVersion')}"
                          value="${bm:formatMaxAppVersion(result['max_app_version'])}"
                          errorMessage="${result['max_app_version_errorMessage']}" readonly="true" inputSpan="span12"
                          required="true"/>
          </div>
          <div class="row-fluid">
            <bm:textInput id="supported_mod" name="supported_mod"
                          label="${bm:springMessage(pageContext, 'publish_product.label.supportedMod')}"
                          errorMessage="${result['supported_mod_errorMessage']}"
                          value="${bm:formatSupportedMod(result['supported_mod'])}"
                          readonly="true" inputSpan="span12"/>
          </div>
          <hr>

          <div class="row-fluid">
            <bm:multipleLocalePanel id="version" var="locale">
              <bm:textInput id="version_name" name="version_name"
                            label="${bm:springMessage(pageContext, 'publish_product.label.versionName')}"
                            value="${result['version_name_'.concat(locale)]}"
                            errorMessage="${result['name_errorMessage']}" readonly="true" locale="${locale}"
                            inputSpan="span12" required="true"/>
              <bm:textAreaInput id="recent_change" name="recent_change"
                                label="${bm:springMessage(pageContext, 'publish_product.label.recentChange')}"
                                value="${result['recent_change_'.concat(locale)]}"
                                errorMessage="${result['description_errorMessage']}" readonly="true"
                                locale="${locale}" rows="5" inputSpan="span12"/>
            </bm:multipleLocalePanel>
          </div>
          <hr class="hr2">
          <div class="row-fluid">
            <div class="offset4">
              <a class="btn btn-danger btn-large submit-button" href="javascript:history.go(-1)"><i
                  class="icon-backward icon-white"></i><strong><spring:message
                  code="publish_uploadError.button.back"/></strong></a>
            </div>
          </div>
        </fieldset>
      </form>
    </div>
  </div>
</div>
<bm:bottomFooter/>
</body>
</html>