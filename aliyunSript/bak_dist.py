#!/usr/bin/python

from common_utils import *
import os
import shutil

def today():
    d = datetime.date.today()
    return (d.year, d.month, d.day)


def main():
	ymd = today()
	str_time = ymd_to_str(ymd)
        os.chdir('/home/wutong/workWT')
        os.system('tar -zcvf dist.tar.gz dist-r3-distribution')	
        shutil.move('dist.tar.gz', '/mnt/bak/dist_' + str_time + '.tar.gz')

if __name__ == '__main__':
    main() 
