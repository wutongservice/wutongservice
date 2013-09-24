#!/usr/bin/python

import os

print "Stop mail mq receiver ..."
os.system('./stop_mail_mq_receiver.py')
print "Start mail mq receiver ..."
os.system('./start_mail_mq_receiver.py')
