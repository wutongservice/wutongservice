#!/usr/bin/python

import os
import subprocess

def execCmd(cmd):
    handle = subprocess.Popen(cmd, shell=True, stdout=subprocess.PIPE)
    return handle.communicate()[0]
