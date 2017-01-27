/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.appdynamics.licensecount.actions;


import org.appdynamics.licensecount.data.EUMPageLicenseCount;
import org.appdynamics.licensecount.data.EUMPageLicenseRange;
import org.appdynamics.appdrestapi.RESTAccess;
import org.appdynamics.appdrestapi.resources.s;
import org.appdynamics.appdrestapi.data.*;
import org.appdynamics.appdrestapi.util.TimeRange;
import org.appdynamics.appdrestapi.util.TimeRangeHelper;


import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.appdynamics.licensecount.resources.LicenseS;

/**
 *
 * @author gilbert.solorzano
 */
public class EUMExecuter implements Runnable{
    private static Logger logger=Logger.getLogger(EUMExecuter.class.getName());
    private EUMPageLicenseCount pageLic;
    private RESTAccess access;
    private TimeRange totalTimeRange;
    private ArrayList<TimeRange> timeRanges=new ArrayList<TimeRange>();
    private String appName;
    private String tierAgentType;
    
    public EUMExecuter(
        EUMPageLicenseCount pageLic,RESTAccess access, 
            String appName, TimeRange totalTimeRange, 
            ArrayList<TimeRange> timeRanges){
        
        this.pageLic=pageLic;
        this.access=access;
        this.appName=appName;
        this.tierAgentType=tierAgentType;
        this.timeRanges=timeRanges;
        this.totalTimeRange=totalTimeRange;
    }
    
    @Override 
    public void run(){
        /* 
            Problem to solve: 
            If the time interval is greater than 7 days then we need to stagger it
        
        */
        
       MetricDatas mDatas= access.getRESTEUMMetricQuery(22, appName, pageLic.getPageName(), totalTimeRange.getStart(), totalTimeRange.getEnd());
                //access.getRESTMetricQuery(nodeLic.getQueryType(), appName, nodeLic.getNode().getTierName(), nodeLic.getNode().getName(), 
                //totalTimeRange.getStart(), totalTimeRange.getEnd());
        //logger.log(Level.INFO, "Executed query!");
        //What happens when this is null ?
        //nodeLic.setTotalRangeValue(new NodeLicenseRange("Total Node Count"));
        pageLic.getTotalRangeValue().setStart(totalTimeRange.getStart());
        pageLic.getTotalRangeValue().setEnd(totalTimeRange.getEnd());
        
        // We need to set the MetricValues:
        
        if(mDatas != null){
            pageLic.getTotalRangeValue().setMetricValues(mDatas.getMetric_data().get(0).getMetricValues().get(0));

            /*
                After getting metrics for the timerange we are going to put the appropriate metrics into their timerange.
            */
            for(TimeRange tRange:timeRanges){

                EUMPageLicenseRange nodeR = new EUMPageLicenseRange();
                nodeR.setStart(tRange.getStart());nodeR.setEnd(tRange.getEnd());
                nodeR.setName(nodeR.createName());
                for(MetricValue val: pageLic.getTotalRangeValue().getMetricValues().getMetricValue()){
                    if(nodeR.withIn(val.getStartTimeInMillis())) nodeR.getMetricValues().getMetricValue().add(val);
                }
                pageLic.getPageLicenseRange().add(nodeR);

            }
        }else{
            logger.log(Level.SEVERE,"We did not get any values " + pageLic.getPageName());
        }
            
        
        
    }
    
    /*
        This is where we are going to do the query, we are going to insure that we make the calls in a staggered
         method. 
    */
    private MetricValues getMetricValues(int valT){
        int myInterval = valT;
        MetricValues val = null;
        if(myInterval < 8){

            TimeRange t = TimeRangeHelper.getSingleTimeRange(myInterval);
            MetricDatas mDatas= access.getRESTEUMMetricQuery(22, appName, pageLic.getPageName(), t.getStart(), t.getEnd());
            if(mDatas != null && !mDatas.hasNoValues()) {
                return mDatas.getMetric_data().get(0).getMetricValues().get(0);
            }
        }else{
            // We are going to start to select the metrics 
             TimeRange t = TimeRangeHelper.getTimeRange(myInterval,5);
             MetricValues val1=null;
             // First we are going to grab the first set of metrics
             MetricDatas mDatas= access.getRESTEUMMetricQuery(22, appName, pageLic.getPageName(), t.getStart(), t.getEnd());
                if(mDatas != null && !mDatas.hasNoValues()) {
                    val= mDatas.getMetric_data().get(0).getMetricValues().get(0);
                }
            
            val1 = getMetricValues(myInterval-5);
            if(val1 != null){
                if(val != null){
                    val.getMetricValue().addAll(val1.getMetricValue());
                }else{
                    return val1;
                }
            }
            
        }
        
        return val;
    }
    
    
}
