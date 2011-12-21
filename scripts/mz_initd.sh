#!/bin/sh

### BEGIN INIT INFO
# Provides:		mz
# Required-Start:	$all
# Required-Stop:	$all
# Default-Start:	2 3 4 5
# Default-Stop:		0 1 6
# Short-Description:	MusicZones control software
### END INIT INFO

# This script requires screen and openjdk-jre to be installed.
# If running this headless I would suggest: openjdk-6-jre-headless
# Install dependencies: apt-get install screen openjdk-6-jre-headless
#
# To install:
# 0) wget --no-check-certificate https://github.com/downloads/jzerbe/MusicZones/mz.jar
# 1) mv mz.jar /srv/mz.jar
# 2) cp mz_initd.sh /etc/init.d/mz
# 3) chmod +x /etc/init.d/mz
# 4) sudo insserv mz

case "$1" in
  start)
	screen -dmS mz java -jar /srv/mz.jar --zone-name=musiczone
	;;
  stop)
	kill `pidof SCREEN`
	;;

  *)
	echo "Usage: /etc/init.d/mz {start|stop}" >&2
	exit 1
esac

exit 0
