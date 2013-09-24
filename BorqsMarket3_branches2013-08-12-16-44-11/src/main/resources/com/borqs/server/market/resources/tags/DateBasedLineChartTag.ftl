<div style="${style}" id="${id}" class="${styleClass}"></div>
<script type="text/javascript">
  $(function () {
    var data = ${dataJson};

    BorqsMarket.displayLineGraph('${id}', 'dates', ${yKeysJson}, ${labelsJson}, data, "${spring.message('publish_productStat.text.noData')}");
  });
</script>
