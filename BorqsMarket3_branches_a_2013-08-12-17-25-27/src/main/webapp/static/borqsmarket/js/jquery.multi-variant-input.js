/// Multi variant input
/// Created by ThimbleOpenSource.com
/// see http://www.thimbleopensource.com/tutorials-snippets/multi-variant-input-jquery-plugin

(function($){  
    $.fn.extend({   
        multi_variant_input: function(options) {  
          var el = $(this);
          var first_run = true;
          var defaults = { variants:{}, values:{}, hoverable: true };
          var options =  $.extend(defaults, options);  
          var el_name = el.attr('name');
          var popup = $('<div class="multi-variant-input-popup"><table></table></div>');
          popup.hide();
          
          var size = 0, key;
          for (key in options.variants) {
            if (options.variants.hasOwnProperty(key)) size++;
            if (size==1) {
              el.attr('name',el.attr('name')+'['+key+']');
              if (options.values.hasOwnProperty(key)) {
                el.val(options.values[key]);
              }
            } else if (size > 1) {
              var input = $('<input type="text" name="'+el_name+'['+key+']" class="multi-variant-input-popup-input" />');
              if (options.values.hasOwnProperty(key)) {
                input.val(options.values[key]);
              }
              input.blur(on_blur_timeout);
              var tr = $('<tr></tr>');
              tr.append($('<td>'+options.variants[key]+'</td>'));
              var td = $('<td></td>');
              td.append(input);
              tr.append(td);
              popup.append(tr);
            }
          }
          first_run = false;
          var left = el.position().left + el.width() + 10;
          var top = el.position().top;
          popup.offset({left:left, top:top});
          el.after(popup);
            
          function on_blur() {
            if (el.not(':focus').length==1 && (popup.find(':focus').length==0)) {
              popup.fadeOut(400);
            }
          }
            
          function on_blur_timeout() {
            setTimeout(on_blur, 100);
          }
            
          $(this).focus(function(){  
            popup.fadeIn(400);
          }).blur(on_blur_timeout);
          
          if (options.hoverable) {
            el.hover(function(){
              popup.fadeIn(400);
            }, function(){
              setTimeout(on_blur, 1000);
            });
          }
            
        }  
    });
})(jQuery);