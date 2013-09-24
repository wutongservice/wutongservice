#!/usr/bin/python

import os

if os.path.isfile('/home/wutong/.bpid/mail_action_receiver.pid') == False:
    os.system('nohup ./run_action_mq_receiver &')
else:
    print "The process is running, please restart the program."
