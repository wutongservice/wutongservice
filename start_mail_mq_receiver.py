#!/usr/bin/python

import os

if os.path.isfile('/home/wutong/.bpid/mail_mq_receiver.pid') == False:
    os.system('nohup ./run_mail_mq_receiver &')
else:
    print "The process is running, please restart the program."
