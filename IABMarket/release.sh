#!/bin/sh

rm -rf ~/tmp/IABMarket_release
mkdir -p ~/tmp/IABMarket_release
cp -r * ~/tmp/IABMarket_release
rm -rf ~/tmp/IABMarket_release/*.pyc
rm -rf ~/tmp/IABMarket_release/build
rm -rf ~/tmp/IABMarket_release/upload
rm -rf ~/tmp/IABMarket_release/TODO.txt
rm -rf ~/tmp/IABMarket_release/release.sh
rm ~/tmp/IABMarket_release/config.ini
mv ~/tmp/IABMarket_release/release_config.ini ~/tmp/IABMarket_release/config.ini
scp -rp ~/tmp/IABMarket_release wutong@42.121.15.199:/home/wutong/IABMarket1/
rm -rf ~/tmp/IABMarket_release




