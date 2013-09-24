#!/usr/bin/python

import os

print "Start notif mq receiver ..."
os.system('./start_notif_mq_receiver.py')
print "Start mail mq receiver ..."
os.system('./start_mail_mq_receiver.py')
print "Start stream mq receiver ..."
os.system('./start_stream_mq_receiver.py')
print "Start link mq receiver ..."
os.system('./start_link_mq_receiver.py')
print "Start email mq receiver ..."
os.system('./start_email_mq_receiver.py')
print "Start main process ..."
os.system('./start_main.py')
