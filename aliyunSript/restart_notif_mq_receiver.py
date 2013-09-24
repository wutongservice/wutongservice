#!/usr/bin/python

import os

print "Stop notif mq receiver ..."
os.system('./stop_notif_mq_receiver.py')
print "Start notif mq receiver ..."
os.system('./start_notif_mq_receiver.py')
