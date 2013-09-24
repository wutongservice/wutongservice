<html>
<head>
    <meta http-equiv='Content-type' content='text/html; charset=utf-8'>
    <title>Phoenix3</title>
</head>
<body>
<div id="main" style="margin:0 auto; padding:0; text-align:center;font-family:Arial;clear:both; margin:0 auto; padding:0; width:820px; height:500px;">
    <style type="text/css">
        body{margin:0 auto; padding:0; text-align:center;font-family:Arial;}
        #main{clear:both; margin:0 auto; padding:0; width:820px; height:500px;}
        #cont{clear: both; margin: 0 auto; padding: 0;width: 820px;background-image: url('http://api.borqs.com/files/email/email_body.png');background-repeat: repeat-y;}
        #header{clear: both; margin: 0 auto; padding: 0; height: 50px; width: 820px;background-image: url('http://api.borqs.com/files/email/email_head.png');background-repeat: no-repeat}
        #cont_l{float: left; margin-left: 20px; width: 168px; height: 200px;background-image: url('http://api.borqs.com/files/email/event_date.png');background-repeat: no-repeat;}
        #cont_r{float: left; margin-left: 20px; width: 590px; height: 200px;}
        #accept{float: left; margin: 0 auto; padding: 0; width: 116px; height: 39px;background-image: url(http://api.borqs.com/files/email/accept_button_normal.png); background-repeat: no-repeat; color: #fff; cursor:pointer;}
        #thank{float: left; margin: 0 auto; padding: 0; margin-left: 30px; width: 116px; height: 39px; background-image: url(http://api.borqs.com/files/email/thanks_button_normal.png);background-repeat: no-repeat; color: #5B5A5A;cursor:pointer;}
        #footer{clear: both; margin: 0 auto; padding: 0; height: 50px; width: 820px; background-image: url('http://api.borqs.com/files/email/email_bottom.png');background-repeat: no-repeat;}
        .time{clear: both; margin: 0 auto; padding: 0; height: 30px; font-size: 15px;font-weight: bold; color: #414141}
    </style>
    <div id="header" style="clear: both; margin: 0 auto; padding: 0; height: 50px; width: 820px;background-image: url('http://api.borqs.com/files/email/email_head.png');background-repeat: no-repeat">
    </div>
    <div id="cont" style="clear: both; margin: 0 auto; padding: 0;width: 820px;background-image: url('http://api.borqs.com/files/email/email_body.png');background-repeat: repeat-y;">
        <div style="clear: both; margin: 0 auto; padding: 0; height: 180px">
            <div id="cont_l" align="center" style="float: left; margin-left: 20px; width: 168px; height: 200px;background-image: url('http://api.borqs.com/files/email/event_date.png');background-repeat: no-repeat;">
                <div style="clear: both; margin: 0 auto; padding: 0; height: 20px; font-size: 18px;
                        color: #fff; font-weight: bold; margin-top: 3px">
                    ${month}
                </div>
                <div style="clear: both; margin: 0 auto; padding: 0; height: 80px; font-size: 50px;
                        color: #000; font-weight: bold; padding-top: 5px;">
                    ${day}
                </div>
                <div style="clear: both; margin: 0 auto; padding: 0;height: 30px; font-size: 15px;
                        color: #ADADAD; font-weight:bold;">
                    ${weekday}
                </div>
                <div style="clear: both; margin: 0 auto; padding: 0; margin-top: 5px; height: 30px;
                        font-size: 11px; color: #000; font-weight: bold; font-family: Verdana;">
                    ${time}
                </div>
            </div>
            <div id="cont_r" style="float: left; margin-left: 20px; width: 590px; height: 200px;">
                <div style="clear: both; margin: 0 auto; padding: 0; text-align: left; font-size: 35px;">
                    Hi  ${displayName}
                </div>
                <div style="clear: both; margin: 0 auto; padding: 0; text-align: left; font-size: 15px;">
                ${fromName} invite you ${register} join ${groupType} 【${groupName}】.
                </div>
                <div style="clear: both; margin: 0 auto; padding: 0; text-align: left; font-size: 15px;">
                ${message}
                </div>
            </div>
        </div>
        <div style="clear: both; margin: 0 auto; padding: 0; height: 150px; text-align: left;
                margin-left: 207px;">
            <div class="time" style="clear: both; margin: 0 auto; padding: 0; height: 30px; font-size: 15px;font-weight: bold; color: #414141">
                When&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;${startTime} - ${endTime}
            </div>
            <div class="time" style="clear: both; margin: 0 auto; padding: 0; height: 30px; font-size: 15px;font-weight: bold; color: #414141">
                Where&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;${address}
            </div>
            <div class="time" style="clear: both; margin: 0 auto; padding: 0; height: 30px; font-size: 15px;font-weight: bold; color: #414141">
                What&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;${description}
            </div>
            <div class="time" style="clear: both; margin: 0 auto; padding: 0; height: 30px; font-size: 15px;font-weight: bold; color: #414141">
                Creator&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;${creator}
            </div>
            <div class="time" style="clear: both; margin: 0 auto; padding: 0; height: 30px; font-size: 15px;font-weight: bold; color: #414141">
                Joined&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;${joined}
            </div>
            <div style="clear: both; margin: 0 auto; padding: 0; height: 50px;margin-left: 50px;">
                <a href="${acceptUrl}" style="text-decoration: none"><div id="accept" style="float: left; margin: 0 auto; padding: 0; width: 116px; height: 39px;background-image: url(http://api.borqs.com/files/email/accept_button_normal.png); background-repeat: no-repeat; color: #5B5A5A; cursor:pointer;">
                    <div style="margin: 8px 5px 5px 25px">
                        Accept
                    </div>
                </div></a>
                <a href="${rejectUrl}" style="text-decoration: none"><div id="thank" style="float: left; margin: 0 auto; padding: 0; margin-left: 30px; width: 116px; height: 39px; background-image: url(http://api.borqs.com/files/email/thanks_button_normal.png);background-repeat: no-repeat; color: #5B5A5A;cursor:pointer;">
                    <div style="margin: 8px 5px 5px 25px">
                        No, thanks
                    </div>
                </div></a>
            </div>
        </div>
    </div>
    <div id="footer" style="clear: both; margin: 0 auto; padding: 0; height: 50px; width: 820px; background-image: url('http://api.borqs.com/files/email/email_bottom.png');background-repeat: no-repeat;">
        <div style="padding-top: 16px">This Email is auto sent by system, <font color='red'>don't reply directly</font>. Copyright 北京播思软件技术有限公司</div>
    </div>
</div>
</body>
</html>
