#!/usr/bin/python

import os

files = [
"/static/js/jquery.min.js",
"/static/lib/jquery-ui/jquery-ui-1.8.23.custom.min.js",
"/static/js/forms/jquery.ui.touch-punch.min.js",
"/static/js/jquery.easing.1.3.min.js",
"/static/js/jquery.debouncedresize.min.js",
"/static/js/jquery.cookie.min.js",
"/static/bootstrap/js/bootstrap.min.js",
"/static/js/bootstrap.plugins.min.js",
"/static/lib/google-code-prettify/prettify.min.js",
"/static/lib/sticky/sticky.min.js",
"/static/lib/qtip2/jquery.qtip.min.js",
"/static/lib/colorbox/jquery.colorbox.min.js",
"/static/lib/jBreadcrumbs/js/jquery.jBreadCrumb.1.1.min.js",
"/static/js/jquery.actual.min.js",
"/static/lib/antiscroll/antiscroll.js",
"/static/lib/antiscroll/jquery-mousewheel.js",
"/static/js/ios-orientationchange-fix.js",
"/static/lib/UItoTop/jquery.ui.totop.min.js",
"/static/js/selectNav.js",
"/static/js/gebo_common.js",
"/static/lib/datatables/jquery.dataTables.min.js",
"/static/lib/datatables/jquery.dataTables.sorting.js",
"/static/js/gebo_tables.js",
]

prefix = '.'
uncompressed = './static/uncompressed-all.js'
compressed = './static/all.js'

def delete_file(fp):
    if os.path.exists(fp):
        os.remove(fp)


delete_file(uncompressed)
delete_file(compressed)

for f in files:
    os.system("cat '%s%s' >> '%s'" % (prefix, f, uncompressed))

os.system("yui-compressor --type js --disable-optimizations --nomunge '%s' -o '%s'" % (uncompressed, compressed))
delete_file(uncompressed)
