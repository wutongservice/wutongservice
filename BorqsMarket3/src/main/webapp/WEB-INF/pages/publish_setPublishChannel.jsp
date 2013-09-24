<!DOCTYPE html>
<html lang="en">
<head>
  <title><spring:message code="publish_setPublishChannels.page.title"/></title>
  <%@include file="fragments/includeStyleAndScript.jspf" %>
  <script type="text/javascript">
    var currentPublishChannels = '${product["publish_channels"]}';

    function onAllOrCustomRadioChanged() {
      if ($('#all_radio').is(':checked')) {
        $('.js-custom').prop('disabled', true);
      }
      if ($('#custom_radio').is(':checked')) {
        $('.js-custom').prop('disabled', false);
      }
    }

    function onSubmit() {
      if ($('#custom_radio').is(':checked')) {
        if (!$('#channel_g_checkbox').is(':checked')
            && !$('#channel_mm_checkbox').is(':checked')) {
          alert('${bm:springMessage(pageContext, "publish_setPublishChannels.text.missingCustomError")}');
          return false;
        }
        $('#publish_channels').val(getDisplayPublishChannels());
      }
    }

    function displayPublishChannels() {
      var isAll = currentPublishChannels == '';
      $('#all_radio').prop('checked', isAll);
      $('#custom_radio').prop('checked', !isAll);
      if (isAll) {
        $('.js-custom').prop('checked', false);
      } else {
        var channels = currentPublishChannels.split(',');
        $('#channel_g_checkbox').prop('checked', $.inArray('g', channels) >= 0);
        $('#channel_mm_checkbox').prop('checked', $.inArray('mm', channels) >= 0);
      }
    }

    function getDisplayPublishChannels() {
      if ($('#all_radio').is(':checked')) {
        return '';
      } else {
        var r = '';
        if ($('#channel_g_checkbox').is(':checked')) {
          r += 'g,';
        }
        if ($('#channel_mm_checkbox').is(':checked')) {
          r += 'mm,';
        }
        return r;
      }
    }

    $(function () {
      displayPublishChannels();
      onAllOrCustomRadioChanged();
      $('.js-all').change(onAllOrCustomRadioChanged);
      $('#contentForm')
          .submit(onSubmit)
          .bind('reset', function () {
            displayPublishChannels();
            onAllOrCustomRadioChanged();
            return false;
          });
    });
  </script>
</head>
<body>
<bm:topNavigationBar module="publish"/>
<bm:secondNavigationBar module="publish" currentAt="apps/${product['app_id']}/${product['id']}"/>
<div class="container">
  <div class="row-fluid">
    <div class="span2 affix left-bar">
      <bm:appNavigationBar module="publish"/>
    </div>
    <div class="span8 offset2 hero-unit" style="text-align: center; line-height: 2em" id="content">
      <h1><spring:message code="publish_setPublishChannels.title.setChannels"/></h1>

      <form method="POST" class="single-form" id="contentForm">
        <fieldset>
          <input type="hidden" id="publish_channels" name="publish_channels"/>

          <div class="span10 offset1">
            <ul style="text-align: left; list-style:none;">
              <li>
                <input type="radio" id="all_radio" name="is_all" value="all" class="js-all"/> <spring:message
                  code="publish_setPublishChannels.label.all"/>
              </li>
              <li>
                <input type=radio id="custom_radio" name="is_all" value="custom" class="js-all"/> <spring:message
                  code="publish_setPublishChannels.label.custom"/>
                <ul style="list-style:none;">
                  <li>
                    <input type="checkbox" id="channel_g_checkbox" name="channel_g" class="js-custom">Google Play
                  </li>
                  <li>
                    <input type="checkbox" id="channel_mm_checkbox" name="channel_mm" class="js-custom"> CMCC MM Market
                  </li>
                </ul>
              </li>
            </ul>
          </div>
          <div class="span12">
            <button class="btn btn-success btn-large submit-button" type="submit">
              <i class="icon-ok icon-white"></i>
              <spring:message code="publish_setPublishChannels.button.save"/>
            </button>
            <button class="btn btn-large submit-button" type="reset">
              <spring:message code="publish_setPublishChannels.button.reset"/>
            </button>
            <a class="btn btn-large btn-warning submit-button" href="/publish/products/${product['id']}${version != '' ? '/'.concat(version) : ''}">
              <i class="icon-backward icon-white"></i>
              <spring:message code="publish_setPublishChannels.button.back"/>
            </a>
          </div>
        </fieldset>
      </form>
    </div>
  </div>
</div>
<bm:bottomFooter/>
</body>
</html>