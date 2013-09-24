#!/usr/bin/python

import os

files = [
    "/static/bootstrap/css/bootstrap.min.css",
    "/static/bootstrap/css/bootstrap-responsive.min.css",
    "/static/lib/jquery-ui/css/Aristo/Aristo.css",
    "/static/css/blue.css",
    "/static/lib/jBreadcrumbs/css/BreadCrumb.css",
    "/static/lib/qtip2/jquery.qtip.min.css",
    "/static/lib/colorbox/colorbox.css",
    "/static/lib/google-code-prettify/prettify.css",
    "/static/lib/sticky/sticky.css",
    "/static/img/splashy/splashy.css",
    "/static/img/flags/flags.css",
    "/static/lib/fullcalendar/fullcalendar_gebo.css",
    "/static/lib/datepicker/datepicker.css",
    "/static/lib/tag_handler/css/jquery.taghandler.css",
    "/static/lib/uniform/Aristo/uniform.aristo.css",
    "/static/lib/multi-select/css/multi-select.css",
    "/static/lib/chosen/chosen.css",
    "/static/lib/stepy/css/jquery.stepy.css",
    "/static/lib/plupload/js/jquery.plupload.queue/css/plupload-gebo.css",
    "/static/lib/CLEditor/jquery.cleditor.css",
    "/static/lib/colorpicker/css/colorpicker.css",
    "/static/lib/smoke/themes/gebo.css",
    "/static/css/style.css",
    ]

prefix = '.'
uncompressed = './static/lib/uncompressed-all.css'
compressed = './static/lib/all.css'

def delete_file(fp):
    if os.path.exists(fp):
        os.remove(fp)


delete_file(uncompressed)
delete_file(compressed)

for f in files:
    os.system("cat '%s%s' >> '%s'" % (prefix, f, uncompressed))

os.system("yui-compressor --type css '%s' -o '%s'" % (uncompressed, compressed))
delete_file(uncompressed)
