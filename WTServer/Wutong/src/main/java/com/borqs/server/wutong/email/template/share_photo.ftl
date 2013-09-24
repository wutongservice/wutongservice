<html>
<head>
    <meta http-equiv='Content-type' content='text/html; charset=utf-8'>
    <script type="text/javascript" src="http://bpc.borqs.com/elearning/js/jquery.js"></script>
    <title>Phoenix3</title>
</head>
<!-- script type="text/javascript">

    var post_id = "";
    showPost();

    function showPost() {
        if (post_id.length>0){
            $(document).ready(
                    function() {
                        var fresh = Math.random();
                        $.getJSON("http://api.borqs.com/post/get",{"postIds":post_id,"fresh":fresh}, function(ret){
                                    showSingle(ret);
                                }
                        );
                    });
        }
    }

    function showSingle(ret) {
        var this_DATA_RETURN = eval(ret);
        var this_POST = this_DATA_RETURN[0];
        var this_attachments =  this_POST.attachments;
        var photos =  eval(this_attachments);
        if (photos.length>0){
            var ima0 = photos[0];
            var str1 = "";
            document.getElementById("largeImg").src = ima0.photo_img_big;

            for (var i = 0; i < photos.length; i++) {
                str1 = str1 + " <a href=\"javascript:image_onclick('"+photos[i].photo_img_big+"')\")><img src='"+photos[i].photo_img_big+"' width=100 style='border: solid 1px #ccc; padding: 5px;onmouseover='this.style.border-color: #FF9900';></a> ";
            }
            if (photos.length==1){
                $("#pic_all").html("");
            }  else{
                $("#pic_all").html(str1);
            }

            document.getElementById("pic_all").innerHTML = str1;
        }
    }

    function image_onclick(url) {
        document.getElementById("largeImg").src = url;
    }

</script -->
<body>
<div id="main" style="margin:0 auto; padding:0; text-align:center;font-family:Arial;clear:both; margin:0 auto; padding:0; width:820px; height:500px;">
    <style type="text/css">
        body{margin:0 auto; padding:0; text-align:center;font-family:Arial;}
        #main{clear:both; margin:0 auto; padding:0; width:820px; height:500px;}
        #cont{clear: both; margin: 0 auto; padding: 0;width: 820px;background-image: url('http://api.borqs.com/files/email/email_body.png');background-repeat: repeat-y;}
        #header{clear: both; margin: 0 auto; padding: 0; height: 50px; width: 820px;background-image: url('http://api.borqs.com/files/email/email_head.png');background-repeat: no-repeat}
        #cont_l{float: left; margin-left: 20px; width: 168px; height: 122px;}
        #cont_r{float: left; margin-left: 20px; width: 590px; height: 122px;}
        #accept{float: left; margin: 0 auto; padding: 0; width: 116px; height: 39px;background-image: url(http://api.borqs.com/files/email/accept_button_normal.png); background-repeat: no-repeat; color: #fff; cursor:pointer;}
        #thank{float: left; margin: 0 auto; padding: 0; margin-left: 30px; width: 116px; height: 39px; background-image: url(http://api.borqs.com/files/email/thanks_button_normal.png);background-repeat: no-repeat; color: #5B5A5A;cursor:pointer;}
        #footer{clear: both; margin: 0 auto; padding: 0; height: 50px; width: 820px; background-image: url('http://api.borqs.com/files/email/email_bottom.png');background-repeat: no-repeat;}
        .time{clear: both; margin: 0 auto; padding: 0; height: 30px; font-size: 15px;font-weight: bold; color: #414141}
    </style>
    <div id="header" style="clear: both; margin: 0 auto; padding: 0; height: 50px; width: 820px;background-image: url('http://api.borqs.com/files/email/email_head.png');background-repeat: no-repeat">
    </div>
    <div id="cont" style="clear: both; margin: 0 auto; padding: 0;width: 820px;background-image: url('http://api.borqs.com/files/email/email_body.png');background-repeat: repeat-y;">
        <div style="clear: both; margin: 0 auto; padding: 0; height: 120px">
            <div id="cont_l" align="center" style="border:1px #808080 solid; padding: 3px; text-align-last:center;float: left; margin-left: 20px; width: 120px; height: 120px;background-repeat: no-repeat;">
                <img src="${icon}" alt="" width="120px" height="120px" border="0" />
            </div>
            <div id="cont_r" style="float: left; margin-left: 20px; width: 590px; padding: 3px; ">
                <div style="clear: both; margin: 0 auto; padding: 0; text-align: left; font-size: 30px;">
                    Hi  ${displayName}
                </div>
                <div style="clear: both; margin: 0 auto; padding: 0; text-align: left; font-size: 12px;">
                    <pre>${content}</pre>
                </div>
                <div style="clear: both; margin: 0 auto; padding: 0; text-align: left; font-size: 12px;">
                    <pre>${description}</pre>
                 </div>
            </div>
        </div>
        <div style="width:95%;clear: both; margin: 0 auto;  padding-bottom: 3px; padding-top: 3px; ">
            <hr style="clear: both; margin: 0 auto;border-bottom:1px #000 solid;border-top:0px ;border-left:0px;border-right:0px;">
        </div>
        <div style="clear: both; margin: 0 auto; padding: 0;margin-left: 10px; margin-top: 6px; text-align:left;">
            <p style="text-align:left;margin-top: 0px; ">
              ${photos}
            </p>
        </div>
    </div>
    <div id="footer" style="clear: both; margin: 0 auto; padding: 0;font-size: 12px; height: 50px; width: 820px; background-image: url('http://api.borqs.com/files/email/email_bottom.png');background-repeat: no-repeat;">
        <div style="padding-top: 12px">${subscribe} This Email is auto sent by system, <font color='red'>don't reply directly</font>. Copyright 北京播思软件技术有限公司</div>
    </div>
</div>




</body>
</html>
