<!DOCTYPE html>
<html>
<head>
    <meta name="viewport" content="width=device-width,minimum-scale=1,maximum-scale=1,user-scalable=no">
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <title>Your Dream, Borqs's Dream!</title>
    <link rel="stylesheet" href="/files/bootstrap.css">
    <link rel="stylesheet" href="/files/bootstrap-responsive.css">
    <link rel="stylesheet" href="/files/qiupu-responsive.css">
    <script src="/files/ga.js" async="" type="text/javascript"></script>
    <script type="text/javascript" src="/files/jquery-1.js"></script>
    <script type="text/javascript" src="/files/jquery.js"></script>
    <script type="text/javascript" src="/files/md5.js"></script>
    <script type="text/javascript" src="/files/qiupu.js"></script>
    <script type=text/javascript>
        function download() {
            document.location = "http://api.borqs.com/innov/gen_excel?&key=${downloadKey}&appid=10001&sign_method=md5&sign=${downloadSign}";
        }
    </script>
</head>


<body>

<!-- HEADER START -->
<div class="navbar navbar-fixed-top hideOnPhone">
    <div class="navbar-inner">
        <div class="container-fluid">
            <a class="brand" href="#">技术大赛</a>
        </div>
    </div>
</div>
<!-- HEADER END -->

<!-- content -->
<div class="row-fluid" style="margin:auto;width:800px">
    <div class="row-fluid">
                <span class="span9" style="float:left">
                    <h2 id="info" style="padding-left: 20px">Borqs创新大赛报名明细</h2>
                </span>
                <span class="span2 hideOnPhone" style="float:right">
                    <a class="btn" href="#" onclick="download()">导出excel</a>
                </span>
    </div>
    <br>
    <div class="row-fluid">
        <table class="span12 table table-striped">
            <tr>
                <th>序号</th>
                <th>申报人</th>
                <th>部门</th>
                <th>项目名称</th>
                <th>其它项目成员</th>
                <th>申报日期</th>
                <th>下载链接</th>
            </tr>
        <#list rs as li>
            <tr>
                <td>${li_index + 1}</td>
                <td>${li.applicant!""}</td>
                <td>${li.department!""}</td>
                <td>${li.product!"项目待定"}</td>
                <td>${li.member_names!""}</td>
                <td>${li.date!""}</td>
                <td>
                    <#if li.file_url!=''>
                        <a target="_blank" href="${li.file_url}">下载</a>
                    </#if>
                </td>
            </tr>
        </#list>
        </table>
    </div>
</div>


</body>
</html>