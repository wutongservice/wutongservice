
<div id="div_${id}">
  <div style="height:0;overflow:hidden">
    <input type="file" id="${id}" name="${id}" src="${src}"
           onchange="BorqsMarket.uploadImage_previewImage(this, '#delimg_${id}', '#img_${id}', '#delbtn_${id}');"/>
    <input type="hidden" id="delimg_${id}" name="delimg_${id}" value=""/>
  </div>
  <a class="thumbnail" style="${style}; position: relative;">
    <img id="img_${id}" alt="${alt}" class="image-upload-image" style="${style}" onclick="$('#${id}').click()" src="${src}">
    <div id="delbtn_${id}" class="image-upload-delete" onclick="BorqsMarket.uploadImage_deleteImage('#${id}', '#delimg_${id}', '#img_${id}', '#delbtn_${id}');"><i class="icon-remove"></i></div>
  </a>
  <script module="text/javascript">
    BorqsMarket.uploadImage_setSrc('#img_${id}', '#delbtn_${id}', '${src}');
  </script>
</div>

