var BorqsMarket = {
    call: function (api, params, success, error) {
        if (params == null) {
            params = {};
        }
        var url = BORQS_MARKET_HOST + api + '?' + $.param(params);
        $.get(url, function (data) {
            if (data['code'] == 0) {
                success(data['data']);
            } else {
                if (error) {
                    error({code: data['code'], error_msg: data['error_msg']});
                }
            }
        });
    },

    uploadImage_setSrc: function (img, delbtn, src) {
        if (src == '') {
            $(img).attr('src', '/static/borqsmarket/image/add_file2.png');
            $(delbtn).hide();
        } else {
            $(img).attr('src', src);
            $(delbtn).show();
        }
    },

    uploadImage_previewImage: function (input, delimgInput, img, delbtn) {
        if (input.files && input.files[0]) {
            var reader = new FileReader();
            reader.onload = function (e) {
                BorqsMarket.uploadImage_setSrc(img, delbtn, e.target.result);
            };
            reader.readAsDataURL(input.files[0]);
            $(delimgInput).attr('value', '');
        } else {
            BorqsMarket.uploadImage_setSrc(img, delbtn, '');
            $(delimgInput).attr('value', 'delete');
        }

    },

    uploadImage_deleteImage: function (input, delImgInput, img, delbtn) {
        $(delImgInput).attr('value', 'delete');
        BorqsMarket.uploadImage_setSrc(img, delbtn, '');
    }
};

$(function () {
//    $('img.image-upload-image').hover(function () {
//        $(this).next().show();
//    }, function () {
//        $(this).next().hide();
//    });
});

$.fn.equals = function (compareTo) {
    if (!compareTo || this.length !== compareTo.length) {
        return false;
    }
    for (var i = 0; i < this.length; ++i) {
        if (this[i] !== compareTo[i]) {
            return false;
        }
    }
    return true;
};

// Graph
BorqsMarket.displayLineGraph = function (graphId, xkey, ykeys, labels, data, errorMsg) {
    if (data.length > 0) {
        Morris.Line({
            element: graphId,
            xkey: xkey,
            ykeys: ykeys,
            labels: labels,
            data: data
        });
    } else {
        $('#' + graphId).append('<div class="hero-unit" style="height: 200px;text-align: center;"><h1>' + errorMsg +
            '</h1></div>');
    }
};
BorqsMarket.displayDonutGraph = function (graphId, data, errorMsg) {
    if (data.length > 0) {
        Morris.Donut({
            element: graphId,
            data: data
        });

    } else {
        $('#' + graphId).append('<div class="hero-unit" style="height: 200px;text-align: center;"><h1>' + errorMsg +
            '</h1></div>');
    }
};

BorqsMarket.setupTagsManager = function (elemId, opts) {
    var tmOpts = {};
    tmOpts.tagsContainer = '#' + elemId + '-tags-container';
    tmOpts.tagClass = 'tm-alt tm-tag-success ' + ((opts.readonly || false) ? 'tm-tag-disabled' : '');
    tmOpts.prefilled = opts.prefilled || [];
    if (!((opts.availableTags || []) === [])) {
        tmOpts.typeahead = true;
        tmOpts.typeaheadSource = opts.availableTags || [];
    } else if (opts.availableTagsUrl) {
        tmOpts.typeahead = true;
        tmOpts.typeaheadAjaxSource = opts.availableTagsUrl;
        tmOpts.typeaheadAjaxMethod = 'GET';
    }
    if ('allowFreeTags' in opts) {
        tmOpts.onlyTagList = !opts.allowFreeTags;
    } else {
        tmOpts.onlyTagList = false;
    }
    var elem = $('#' + elemId);
    elem.tagsManager(tmOpts);
    $('#' + elemId + '-tip').text(opts.tip || '');
};

