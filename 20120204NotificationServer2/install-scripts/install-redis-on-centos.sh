#sudo yum -y install gcc

cd /tmp
wget http://redis.googlecode.com/files/redis-2.4.9.tar.gz
tar -zxf redis-2.4.9.tar.gz
cd redis-2.4.9
sudo make install

cd utils
sudo ./install_server