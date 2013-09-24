<div style="${style}" id="${id}" class="${styleClass}"></div>
<script type="text/javascript">
  $(function () {
    var data = ${dataJson};
    BorqsMarket.displayDonutGraph('${id}', data, "${spring.message('publish_productStat.text.noData')}");
  });
</script>