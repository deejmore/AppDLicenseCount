/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.appdynamics.licensecount.actions;

import org.appdynamics.licensecount.data.TierLicenseCount;
import org.appdynamics.licensecount.data.NodeLicenseCount;
import org.appdynamics.licensecount.data.NodeLicenseRange;
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
    private ArrayList<TimeRange> timeRanges=new ArrayList<TimeRange>();
    private String appName;
    
    /*
     * We need to insure that we have all of the information required.
     * 
     */
    public TierExecutor(TierLicenseCount tierLic,RESTAccess access, 
            String appName, TimeRange totalTimeRange, 
            ArrayList<TimeRange> timeRanges){
        
        this.tierLic=tierLic;
        this.access=access;
        this.appName=appName;
        this.timeRanges=timeRanges;
        this.totalTimeRange=totalTimeRange;
        
    }
    
    @Override 
    public void run(){
        /*
            We are going to query both for the machine agents and the app agents. The first thing we need to
        determine if we are doing a daily call or a minute call. If we are doing a minute call we can just execute.
        if we are doing a daily call, maybe we split it.
        */
        MetricDatas appAgents=null;
        MetricDatas macAgents=null;
        if(LicenseS.TYPE_V == 1){
            appAgents = getMetrics(0,totalTimeRange.getStart(),totalTimeRange.getEnd());
            macAgents = getMetrics(1,totalTimeRange.getStart(),totalTimeRange.getEnd());
            
        }else{
            appAgents = getMetrics(0,LicenseS.INTERVAL_V);
            macAgents = getMetrics(1,LicenseS.INTERVAL_V);
        }
        
        /*
            For now we are going to fail if we don't get it, but we need to do something else.
        */
        
        if(appAgents != null && macAgents != null){
            pushRanges(appAgents, macAgents);
        }else{
            if(appAgents == null){
                    appAgents = getMetrics(0,totalTimeRange.getStart(),totalTimeRange.getEnd());
            
            }
            if(macAgents == null){
                   macAgents = getMetrics(1,totalTimeRange.getStart(),totalTimeRange.getEnd());
            }
           
            // This is going to be the last check for anything, if we still don't have a true statement, just give it up.
            if(appAgents != null && macAgents != null){
                pushRanges(appAgents, macAgents);
            }else{
                logger.log(Level.SEVERE,"We did not get all of the proper metrics for app and machine agents for tier " + tierLic.getName());
            }
        }
    }
    
    public void pushRanges(MetricDatas app, MetricDatas mach){
        /*
            This is going to get all of the nodes and get the metrics for them.
        */
        for(NodeLicenseCount nlc:tierLic.getNodeLicenseCount()){
                nlc.getTotalRangeValue().setStart(totalTimeRange.getStart());
                nlc.getTotalRangeValue().setEnd(totalTimeRange.getEnd());

                // getMetricData(app, nlc.getName()).
                //nlc.getTotalRangeValue().setMetricValues(app.getFirstMetricValues()); // This will get the metrics for the app agent
                //nlc.getTotalRangeValue().setMachineMetricValues(mach.getFirstMetricValues()); // We need to check for nulls
                nlc.getTotalRangeValue().setMetricValues( getMetricData(app,nlc.getName()).getFirstMetricValues()); // This will get the metrics for the app agent
                
                nlc.getTotalRangeValue().setMachineMetricValues(getMetricData(mach,nlc.getName()).getFirstMetricValues() ); // We need to check for nulls
                for(TimeRange tRange:timeRanges){
                        NodeLicenseRange nodeR = new NodeLicenseRange();
                        nodeR.setStart(tRange.getStart());nodeR.setEnd(tRange.getEnd());
                        nodeR.setName(nodeR.createName());
                        for(MetricValue val: nlc.getTotalRangeValue().getMetricValues().getMetricValue()){
                            if(nodeR.withIn(val.getStartTimeInMillis())) nodeR.getMetricValues().getMetricValue().add(val);
                        }
                        for(MetricValue val: nlc.getTotalRangeValue().getMachineMetricValues().getMetricValue()){
                            if(nodeR.withIn(val.getStartTimeInMillis())) nodeR.getMetricValues().getMetricValue().add(val);
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
    
    
    public MetricDatas getMetrics(int queryIndex, long start, long end){
        int count=1;
        boolean success=false;
        
        MetricDatas mDatasApp= null;
        while(!success && count < 4){           
                mDatasApp=access.getAgentNodeAppMetricQuery(queryIndex, appName, tierLic.getName(), "*", start, end, false);
                        //access.getRESTMetricQuery(queryIndex, appName, tierLic.getName(), "*", start, end);
                if(mDatasApp == null){
                        try{
                            Thread.sleep(1000 * count);
                            count ++;
                        }catch(Exception e){}
                }else{
                    count = 5;success=true;
                }
                
        }
        
        if(!success){
            logger.log(Level.SEVERE, new StringBuilder().append("Unable to get metrics for tier ").append(tierLic.getName()).append(" for the index ").append(queryIndex).append(".").toString());
        }
        return mDatasApp;
    }
    

    
    /*
        This will do multiple requests for the same node.
    */
    private MetricDatas getMetrics(int queryIndex, int valT){
        int myInterval = valT;
        MetricDatas val = null;
        if(myInterval < 8){
            TimeRange t = TimeRangeHelper.getSingleTimeRange(myInterval);
            val= access.getAgentNodeAppMetricQuery(queryIndex, appName, tierLic.getName(), "*",  t.getStart(), t.getEnd(),false);
            if(val != null && !val.hasNoValues()) {
                return val;
            }
        }else{
            // We are going to start to select the metrics 
             TimeRange t = TimeRangeHelper.getSingleTimeRange(myInterval,5);
             MetricDatas val1;
             // First we are going to grab the first set of metrics
             // logger.log(Level.INFO,new StringBuilder().append("Asking for ").append(t.getStart()).append(" and ").append(t.getEnd()).toString());
             val= access.getAgentNodeAppMetricQuery(queryIndex, appName, tierLic.getName(), "*",  t.getStart(), t.getEnd(),false);
                if(val != null && !val.hasNoValues()) {
            
                    val1 = getMetrics(queryIndex,myInterval-5);
                    if(val != null){
                        val.merge(val1);
                    }
                }
            
        }
        
        return val;
    }
    
    
}
