#!/usr/bin/python

import os

if os.path.isfile('/home/wutong/.bpid/notif_mq_receiver.pid') == False:
    os.system('nohup ./run_notif_mq_receiver &')
else:
    print "The process is running, please restart the program."
