#!/usr/bin/python

from command import *
import os

# res =  execCmd('ps -ef | grep "java -Xms128m -Xmx512m" | awk \'{print $2}\'')
# arr = res.split('\n')
# for pid in arr:
#   execCmd('kill -9 ' + pid)

print "Stop notif mq receiver ..."
execCmd('./stop_notif_mq_receiver.py')
print "Stop mail mq receiver ..."
execCmd('./stop_mail_mq_receiver.py')
print "Stop stream mq receiver ..."
execCmd('./stop_stream_mq_receiver.py')
print "Stop link mq receiver ..."
execCmd('./stop_link_mq_receiver.py')
print "Stop email mq receiver ..."
execCmd('./stop_email_mq_receiver.py')
print "Stop main process ..."
execCmd('./stop_main.py')
