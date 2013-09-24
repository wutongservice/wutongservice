<!DOCTYPE html>
<html lang="en">
<head>
  <title><spring:message code="publish_inputManifestForVersion.page.title"/></title>
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
      <h1 style="text-align: center;margin-bottom: 40px;"><spring:message code="publish_inputManifestForProduct.title.fillManifest"/></h1>

      <div class="alert alert-info">
        <spring:message code="publish_inputManifestForProduct.text.reason"/>
      </div>

      <c:if test="${bm:isCheckResultError(result)}">
        <div class="alert alert-error">
          <spring:message code="publish_inputManifestForProduct.text.inputError"/>
        </div>
      </c:if>


      <form method="POST" class="form-horizontal" enctype="multipart/form-data" accept-charset="UTF-8"
            action="/publish/products/${result['id']}/upload/fill_manifest">

        <fieldset>
          <input type="hidden" name="fileStatus" value="${fileStatus}"/>

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
                            inputSpan="span12" required="true"
                            placeholder="${bm:springMessage(pageContext, 'publish_inputManifestForVersion.text.lastVersionPrompt')}&nbsp;${result['last_version_name_'.concat(locale)]}"/>
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