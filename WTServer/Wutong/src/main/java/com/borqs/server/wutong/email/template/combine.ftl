<html>
<head>
    <meta http-equiv='Content-type' content='text/html; charset=utf-8'>
    <title>Phoenix3</title>
</head>
<body>
<div id="main"
     style="margin:0 auto; padding:0; text-align:center;clear:both; margin:0 auto; padding:0; height:auto width:820px;">

    <div id="header"
         style="clear: both; margin: 0 auto; padding: 0; height: 50px; width: 820px;background-image: url('http://api.borqs.com/files/email/email_head.png');background-repeat: no-repeat"></div>
    <div id="cont"
         style="clear: both; margin: 0 auto; padding: 0;width: 820px; min-height:350px;background-image: url('http://api.borqs.com/files/email/email_body.png');background-repeat: repeat-y;">
        <div style="clear: both; margin: 0 auto; padding: 15px; ">

            <div style="font-family:Verdana, Geneva, sans-serif, '微软雅黑', '新宋体', '宋体';clear: both; margin: 0 auto; padding: 0; text-align: left; font-size: 35px;">
                Hi  ${map.displayName}</div>
            <div style=" line-height:25px;text-indent:2em;font-family:Verdana, Geneva, sans-serif, '微软雅黑', '新宋体', '宋体';clear: both; margin: 5px auto; padding: 5px; text-align: left; font-size: 15px; border-bottom-color:#999; border-bottom-width:1px; border-bottom-style:solid">
                ${map.content}
                    <p></p>
            </div>
            ${map.html}
        </div>
    </div>
    <div id="footer"
         style="clear: both; margin: 0 auto; padding: 0; height: 50px; width: 820px; background-image: url('http://api.borqs.com/files/email/email_bottom.png');background-repeat: no-repeat;">
        <div style="padding-top: 16px">${map.subscribe}  This Email is auto sent by system, <font color='red'>don't
            reply
            directly</font>. Copyright 北京播思软件技术有限公司
        </div>
    </div>
</div>
</body>
</html>
