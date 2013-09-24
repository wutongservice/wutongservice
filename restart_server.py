#!/usr/bin/python

import os

print "Restart notif mq receiver ..."
os.system('./restart_notif_mq_receiver.py')
print "Restart mail mq receiver ..."
os.system('./restart_mail_mq_receiver.py')
print "Restart stream mq receiver ..."
os.system('./restart_stream_mq_receiver.py')
print "Restart link mq receiver ..."
os.system('./restart_link_mq_receiver.py')
print "Restart email mq receiver ..."
os.system('./restart_email_mq_receiver.py')
print "Restart action mq receiver ..."
os.system('./restart_action_mq_receiver.py')
print "Restart main process ..."
os.system('./restart_main.py')
