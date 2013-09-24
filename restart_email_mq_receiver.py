#!/usr/bin/python

import os

print "Stop email mq receiver ..."
os.system('./stop_email_mq_receiver.py')
print "Start email mq receiver ..."
os.system('./start_email_mq_receiver.py')
