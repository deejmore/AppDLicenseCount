/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.appdynamics.licensecount.actions;

import org.appdynamics.licensecount.data.TierLicenseCount;
import org.appdynamics.licensecount.data.NodeLicenseCount;
import org.appdynamics.licensecount.data.NodeLicenseRange;
import org.appdynamics.licensecount.data.TierHourLicenseRange;
import org.appdynamics.licensecount.resources.LicenseS;
import org.appdynamics.appdrestapi.RESTAccess;
import org.appdynamics.appdrestapi.data.*;
import org.appdynamics.appdrestapi.util.*;


import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author gilbert.solorzano
 * <p>
 *      The TierExecutor will be used to get information on the license from tier perspective. This should speed up 
 * the data collection because instead of executing individual queries for each node we are executing for all
 * nodes in a tier. This still needs to be properly tested.
 * </p>
 */

public class TierExecutor implements Runnable{
    
    private static Logger logger=Logger.getLogger(TierExecutor.class.getName());
    private TierLicenseCount tierLic;
    private RESTAccess access;
    private TimeRange totalTimeRange;
    private ArrayList<TimeRange> timeRanges, hourlyTimeRanges;
    private String appName;
    
    /*
     * We need to insure that we have all of the information required.
     * 
     */
    public TierExecutor(TierLicenseCount tierLic,RESTAccess access, 
            String appName, TimeRange totalTimeRange, 
            ArrayList<TimeRange> timeRanges, ArrayList<TimeRange> hourlyTimeRanges){
        
        this.tierLic=tierLic;
        this.access=access;
        this.appName=appName;
        this.timeRanges=timeRanges;
        this.totalTimeRange=totalTimeRange;
        this.hourlyTimeRanges=hourlyTimeRanges;
        
    }
    
    @Override 
    public void run(){
        /*
            We are going to query both for the machine agents and the app agents. The first thing we need to
        determine if we are doing a daily call TYPE_V=1 or a minute call TYPE_V=0. If we are doing a minute call we can just execute.
        if we are doing a daily call, maybe we split it.
        */
        MetricDatas appAgents=null;
        MetricDatas macAgents=null;
        
        MetricDatas appTierAgents=null;
        MetricDatas macTierAgents=null;
        
        // -- logger.log(Level.INFO,"\tExecuting the tier executer on type " + LicenseS.TYPE_V);
        if(LicenseS.TYPE_V == 1){
            appAgents = getMetrics(0,totalTimeRange.getStart(),totalTimeRange.getEnd(),0);
            macAgents = getMetrics(1,totalTimeRange.getStart(),totalTimeRange.getEnd(),0);
            
            appTierAgents = getMetrics(0,totalTimeRange.getStart(),totalTimeRange.getEnd(),1);
            macTierAgents = getMetrics(1,totalTimeRange.getStart(),totalTimeRange.getEnd(),1);
            
        }else{
            appAgents = getMetrics(0,LicenseS.INTERVAL_V,0);
            macAgents = getMetrics(1,LicenseS.INTERVAL_V,0);
            
            appTierAgents = getMetrics(0,LicenseS.INTERVAL_V,1);
            macTierAgents = getMetrics(1,LicenseS.INTERVAL_V,1);
        }
        
        /*
            For now we are going to fail if we don't get it, but we need to do something else.
        */
        
        if(appAgents != null && macAgents != null){
            pushDailyRanges(appAgents, macAgents);
        }else{
            logger.log(Level.INFO,"\tIntial queries returned null values ");
            if(appAgents == null){
                    appAgents = getMetrics(0,totalTimeRange.getStart(),totalTimeRange.getEnd(),0);
            }
            if(macAgents == null){
                   macAgents = getMetrics(1,totalTimeRange.getStart(),totalTimeRange.getEnd(),0);
            }
           
            // This is going to be the last check for anything, if we still don't have a true statement, just give it up.
            if(appAgents != null && macAgents != null){
                pushDailyRanges(appAgents, macAgents);
            }else{
                logger.log(Level.SEVERE,"We did not get all of the proper metrics for app and machine agents for tier " + tierLic.getName());
            }
        }
        
        if(appTierAgents != null && macTierAgents != null){
            pushHourlyRanges(appTierAgents, macTierAgents);
        }else{
            logger.log(Level.INFO,"\tIntial queries returned null values ");
            if(appTierAgents == null){
                    appTierAgents = getMetrics(0,totalTimeRange.getStart(),totalTimeRange.getEnd(),1);
            }
            if(macTierAgents == null){
                   macTierAgents = getMetrics(1,totalTimeRange.getStart(),totalTimeRange.getEnd(),1);
            }
           
            // This is going to be the last check for anything, if we still don't have a true statement, just give it up.
            if(appTierAgents != null && macTierAgents != null){
                pushHourlyRanges(appTierAgents, macTierAgents);
            }else{
                logger.log(Level.SEVERE,"We did not get all of the proper metrics for app and machine agents for tier " + tierLic.getName());
            }
        }
        
        
    }
    
    public void pushHourlyRanges(MetricDatas app, MetricDatas mach){
        /*
            This is going to get all of the nodes and get the metrics for them.
        */
        
        for(TimeRange hourRange: hourlyTimeRanges){
            TierHourLicenseRange tr = new TierHourLicenseRange(hourRange);
            tr.createName();
            for(MetricValue mv:tierLic.getMetricValues(app).getMetricValue()){
                if(tr.withIn(mv.getStartTimeInMillis())) tr.getAppMetricValues().getMetricValue().add(mv);
            }
            
            
            for(MetricValue mv:tierLic.getMetricValues(mach).getMetricValue()){
                if(tr.withIn(mv.getStartTimeInMillis())) tr.getMachineMetricValues().getMetricValue().add(mv);
            }
            
            tr.countAgents();
            tierLic.getTierHourLicenseRange().add(tr);           
        }

        
    }
    
    public void pushDailyRanges(MetricDatas app, MetricDatas mach){
        /*
            This is going to get all of the nodes and get the metrics for them.
        */
        for(NodeLicenseCount nlc:tierLic.getNodeLicenseCount()){
                logger.log(Level.INFO,"Working on node {0}",nlc.getName());
                nlc.getTotalRangeValue().setStart(totalTimeRange.getStart());
                nlc.getTotalRangeValue().setEnd(totalTimeRange.getEnd());

                nlc.getTotalRangeValue().setMetricValues( getMetricData(app,nlc.getName()).getFirstMetricValues()); // This will get the metrics for the app agent
                nlc.getTotalRangeValue().setMachineMetricValues(getMetricData(mach,nlc.getName()).getFirstMetricValues() ); // We need to check for nulls
                
                for(TimeRange tRange:timeRanges){
                        NodeLicenseRange nodeR = new NodeLicenseRange();
                        nodeR.setStart(tRange.getStart());
                        nodeR.setEnd(tRange.getEnd());
                        nodeR.setName(nodeR.createName());
                        logger.log(Level.INFO,"Looking at {0}",nodeR);
                        for(MetricValue val: nlc.getTotalRangeValue().getMetricValues().getMetricValue()){
                            if(nodeR.withIn(val.getStartTimeInMillis())) {
                                //logger.log(Level.INFO,"Found -- Looking at {0}",nodeR);
                                nodeR.getMetricValues().getMetricValue().add(val);
                            }
                        }
                        for(MetricValue val: nlc.getTotalRangeValue().getMachineMetricValues().getMetricValue()){
                            if(nodeR.withIn(val.getStartTimeInMillis())) {
                                nodeR.getMetricValues().getMetricValue().add(val);
                            }
                        }
                        nlc.getRangeValues().add(nodeR);
                }
            
        }

        
    }
    
    /*
        This is going to return the metric data for a particular node, this way we can concentrate on  a single node.
    */
    public MetricData getMetricData (MetricDatas data, String nodeName){
        String name=null;
        for(MetricData mData:data.getMetric_data()){
            String[] nameArr = mData.getMetricPath().split("\\|");
            if(nameArr != null && nameArr.length > 2){ 
                name=nameArr[3];
                if(name.equalsIgnoreCase(nodeName)) return mData;
            } 
            
        }
        return new MetricData();
    }
    
    
    public MetricDatas getMetrics(int queryIndex, long start, long end, int type){
        int count=1;
        boolean success=false;
        
        MetricDatas val= null;
        while(!success && count < 4){       
            
                if(type == 0){
                    val= access.getAgentNodeAppMetricQuery(queryIndex, appName,  tierLic.getName(), "*", start,end, false);
                }else{
                    val= access.getAgentTierAppMetricQuery(queryIndex, appName,  tierLic.getName(), start, end, false);
                }
                
                if(val  == null){
                        try{
                            count ++;
                            Thread.sleep(1000 * count);
                        }catch(Exception e){}
                }else{
                    count = 5;success=true;
                }
                
        }
        
        if(!success){
            logger.log(Level.SEVERE, new StringBuilder().append("Unable to get metrics for tier ").append(tierLic.getName()).append(" for the index ").append(queryIndex).append(".").toString());
        }
        return val;
    }
    

    
    /*
        This will do multiple requests for the same node. The type is either 0 for node and 1 for tier.
    */
    private MetricDatas getMetrics( int queryIndex, int valT, int type ){
       
        MetricDatas val = null;
        
        if(valT < 8){
            TimeRange t = TimeRangeHelper.getSingleTimeRange(valT);
            logger.log(Level.INFO, "Asking for the last " + t.toString());
            if(type == 0){
                val= access.getAgentNodeAppMetricQuery(queryIndex, appName,  tierLic.getName(), "*", t.getStart(), t.getEnd(), false);
             }else{
                 val= access.getAgentTierAppMetricQuery(queryIndex, appName,  tierLic.getName(), t.getStart(), t.getEnd(), false);
             }
             logger.log(Level.INFO, "*****Asking for the nodes in tier last 8 {0} and got\n",tierLic.getName());
            
        }else{
            // We are going to start to select the metrics 
             TimeRange t = TimeRangeHelper.getSingleTimeRange(valT,5); 
             MetricDatas val1;
             // First we are going to grab the first set of metrics
             // logger.log(Level.INFO,new StringBuilder().append("Asking for ").append(t.getStart()).append(" and ").append(t.getEnd()).toString());
             logger.log(Level.INFO, "Asking for the last " + t.toString());
             if(type == 0){
                val= access.getAgentNodeAppMetricQuery(queryIndex, appName,  tierLic.getName(), "*", t.getStart(), t.getEnd(), false);
             }else{
                 val= access.getAgentTierAppMetricQuery(queryIndex, appName,  tierLic.getName(), t.getStart(), t.getEnd(), false);
             }
             logger.log(Level.INFO, "*****Asking for the nodes in tier {0} and got\n", tierLic.getName());
             
                if(val != null && ! val.hasNoValues()) {
                    val1 = getMetrics(queryIndex,valT-5, type);
                    if(val != null){
                        val.merge(val1);
                    }
                }
            
        }
        
        return val;
    }
    
    
}
