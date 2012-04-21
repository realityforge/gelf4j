package me.moocar.logbackgelf;

/**
 * Standard syslog levels.
 */
public enum SyslogLevel
{
  EMERG,  /* 0 - system is unusable */
  ALERT,  /* 1 - action must be taken immediately */
  CRIT,  /* 2 - critical conditions */
  ERR,  /* 3 - error conditions */
  WARNING,  /* 4 - warning conditions */
  NOTICE,  /* 5 - normal but significant condition */
  INFO,  /* 6 - informational */
  DEBUG  /* 7 - debug-level messages */
}
