#!/system/bin/sh

export LD_LIBRARY_PATH=/vendor/lib:/system/lib
kill -9 `pidof mediaserver`
exit
