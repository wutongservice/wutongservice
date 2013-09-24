#!/usr/bin/expect  -f
proc exec_it {command} {
#spawn -noecho ls -l
#log_user 0      
#expect eof 
#puts [string trimleft $expect_out(buffer) $command]   
#puts [string $expect_out(buffer) ]
spawn whoami
expect eof
set raw [string trim $expect_out(buffer)]
puts $raw
}
proc iputs {c} {
puts $c
}
#log_user 0
set pass [lindex $argv 0]
set cmd [lindex $argv 1]
set param "-m"
if {$argc>3} {
set p [lindex $argv 3]
set param "$p" 
}
set user root
if {$argc>2} {
set n [lindex $argv 2]
set user $n 
}
iputs "pass=$pass"
iputs "cmd=$cmd" 
iputs "param=$param"
iputs "user=$user"
#set timeout 10
#iputs "start to work..."
#iputs "1.get current username..."
#spawn whoami
#expect eof 
#set curuser $expect_out(buffer)
#iputs "current user name ($curuser)"
#iputs "1. end."
#iputs "2.change to root..."
#spawn su root
#expect "assword:"
#send "$pass\r"
#send "$cmd\r"
#iputs "2. end."
#iputs "3.change to old user"
#send "su $curuser"
#iputs "3. end."
puts "exec su $param \"$cmd\" $user ..."
spawn su $param -c "$cmd" $user
expect {
"*assword:" {
send "$pass\r"
interact
}
"#" {}
eof {}
timeout {}
}
#exit
puts "exec command ok."
