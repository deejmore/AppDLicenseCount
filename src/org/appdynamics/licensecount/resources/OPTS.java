/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.appdynamics.licensecount.resources;

import java.util.ArrayList;

/**
 *
 * @author gilbert.solorzano
 * 
 *  -v | --version
 *  -f  | --file
 *  -t  | --type
 *  -i  | --interval
 *  -I | --longInterval
 *  -C | --config
 *  -U | --uptime
 *  -A | --apps
 *  -n | --now
 *  -N | --node
 *  -L | --logfile
 *  -g | --groupfile
 *  -c | --controller
 *  -P | --port
 *  -a | --account
 *  -u | --user
 *  -p | --passwd
 *  -s | --ssl
 *  -G | --granular
 * 
 *  -E | --eum
 *  -D | --dbs
 * 
 */
public class OPTS {
    
    public static final String VERSION_L="version";
    public static final String VERSION_S="v";
    public static final boolean VERSION_R=false;
    public static final boolean VERSION_A=false;
    public static final String VERSION_D="Prints out the version of the tool";
    public static final String VERSION_V="2.0.2";
    
    public static final String FILENAME_L="file";
    public static final String FILENAME_S="f";
    public static final String FILENAME_D="Optional : This is going to be the file name that is going to be created. Default is <AccountName>_LicenseCount.xlsx.";
    public static final boolean FILENAME_R=false;
    public static final boolean FILENAME_A=true;
    public static String FILENAME_V="Customer1";
    
    public static final String TYPE_L="type";
    public static final String TYPE_S="t";
    public static final String TYPE_D="Optional : This is going to decide whether to do a daily or minute report. Default is going to be 0 for a daily.  ";
    public static final boolean TYPE_R=false;
    public static final boolean TYPE_A=true;
    public static int TYPE_V=0;
    
    public static final String INTERVAL_L="interval";
    public static final String INTERVAL_S="i";
    public static final String INTERVAL_D="Optional : This is going to be the number of days we go back and run. Default is going back 7 days from midnight to midnight.  ";
    public static final boolean INTERVAL_R=false;
    public static final boolean INTERVAL_A=true;
    public static int INTERVAL_V=7;
    
    public static final String LINTERVAL_L="longInterval";
    public static final String LINTERVAL_S="I";
    public static final String LINTERVAL_D="Optional : This is used when the report is going to be beyond 30 days but up to 365 days only.  ";
    public static final boolean LINTERVAL_R=false;
    public static final boolean LINTERVAL_A=false;
    public static boolean LINTERVAL_V=false;
    
    public static final String CFG_FILE_L="config";
    public static final String CFG_FILE_S="C";
    public static final String CFG_FILE_D="";
    public static final boolean CFG_FILE_R=false;
    public static final boolean CFG_FILE_A=true;
    
    // Uptime : default 25%
    public static final String UPTIME_L="uptime";
    public static final String UPTIME_S="U";
    public static final String UPTIME_D="Optional : The amount of uptime necessary for an agent to be up so that it is counted. Default value is .70 (70%)";
    public static final boolean UPTIME_R=false;
    public static final boolean UPTIME_A=true;
    public static double UPTIME_V=0.0;
    
    //This will be added to limit the number of apps reported.
    public static final String APPS_L="apps";
    public static final String APPS_S="A";
    public static final String APPS_D="Optional : The applications that you want to report on, comma delimited list.";
    public static final boolean APPS_R=false;
    public static final boolean APPS_A=true;
    public static ArrayList<String> APPS_V=new ArrayList<String>();
    //
    public static final String NOW_L="now";
    public static final String NOW_S="n";
    public static final String NOW_D="Include upto the last full hour for the agent count";
    public static final boolean NOW_R=false;
    public static final boolean NOW_A=false;
    public static boolean NOW_V=false;
    
    public static final String NODE_L="node";
    public static final String NODE_S="N";
    public static final String NODE_D="Provide an additional sheet with a node hourly breakdown";
    public static final boolean NODE_R=false;
    public static final boolean NODE_A=false;
    public static boolean NODE_V=false;
    
    //Log File
    
    public static final String LOGFILE_L="logfile";
    public static final String LOGFILE_S="L";
    public static final String LOGFILE_D="Path to the log file";
    public static final boolean LOGFILE_R=false;
    public static final boolean LOGFILE_A=true;
    public static String LOGFILE_V="";
    
    
    public static final String USAGE="java -cp \"execLib/*\" org.appdynamics.licensecount.CountLicenses -c <FQDN-For-Controller> -P <PORT> -u<USER-NAME> -p <PASSWORD> -a <ACCOUNT-NAME> [-s] [-n] [-i] [-A <Apps>] [-d 1|2] [-g<PATH-TO-FILE>]";
    public static final String FILE_ENDING="_LicenseCount.xlsx";
    
    public static final String GROUP_L = "groupfile";
    public static final String GROUP_S = "g";
    public static final String GROUP_D = "Path to group reference file";
    public static final boolean GROUP_R = false;
    public static final boolean GROUP_A =true;
    public static String GROUP_V = "";

    public static final String CONTROLLER_L="controller";
    public static final String CONTROLLER_S="c";
    public static final String CONTROLLER_D="This is going to be the FQDN of the controller, for example: appdyn.saas.appdynamics.com";
    public static final boolean CONTROLLER_R=true;
    public static final boolean CONTROLLER_A=true;
    public static String CONTROLLER_V=null;
    
    public static final String PORT_L="port";
    public static final String PORT_S="P";
    public static final String PORT_D="The is going to be the port that is used by the controller.";
    public static final boolean PORT_R=true;
    public static final boolean PORT_A=true;
    public static String PORT_V=null;
    
    public static final String ACCOUNT_L="account";
    public static final String ACCOUNT_S="a";
    public static final String ACCOUNT_D="If controller is multi-tenant add the account";
    public static final boolean ACCOUNT_R=false;
    public static final boolean ACCOUNT_A=false;
    public static String ACCOUNT_V="customer1";
    
    public static final String USERNAME_L="username";
    public static final String USERNAME_S="u";
    public static final String USERNAME_D="The user name to use for the connection";
    public static final boolean USERNAME_R=true;
    public static final boolean USERNAME_A=true;
    public static String USERNAME_V=null;
    
    public static final String PASSWD_L="passwd";
    public static final String PASSWD_S="p";
    public static final String PASSWD_D="The password to use for the connection";
    public static final boolean PASSWD_R=true;
    public static final boolean PASSWD_A=true;
    public static String PASSWD_V=null;
    
    public static final String SSL_L="ssl";
    public static final String SSL_S="s";
    public static final String SSL_D="Use SSL with connection";
    public static final boolean SSL_R=false;
    public static final boolean SSL_A=false;
    public static boolean SSL_V=false;
    
    public static final String DEBUG_L="debug";
    public static final String DEBUG_S="d";
    public static final String DEBUG_D="Debug level to set the calls at.";
    public static final boolean DEBUG_A=true;
    public static final boolean DEBUG_R=false;
    public static int DEBUG_V=0;
    
    public static final String GRANULAR_L="granular";
    public static final String GRANULAR_S="G";
    public static final String GRANULAR_D="How granular you want the text.";
    public static final boolean GRANULAR_A=false;
    public static final boolean GRANULAR_R=false;
    public static boolean GRANULAR_V=false;
    
    public static final String EUM_L="eum";
    public static final String EUM_S="E";
    public static final String EUM_D="Count EUM Pages";
    public static final boolean EUM_A=false;
    public static final boolean EUM_R=false;
    public static boolean EUM_V=false;
    
    public static final String DB_L="dbs";
    public static final String DB_S="D";
    public static final String DB_D="Count database licenses";
    public static final boolean DB_A=false;
    public static final boolean DB_R=false;
    public static boolean DB_V=false;
    
    public static final String OPTION_ERROR_1="A required parameter was not found. Please view the help menu for required parameters.";
    
    
    
    
}
