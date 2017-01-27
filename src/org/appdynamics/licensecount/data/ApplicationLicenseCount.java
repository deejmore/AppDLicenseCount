/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.appdynamics.licensecount.data;

import org.appdynamics.licensecount.resources.LicenseS;
import org.appdynamics.licensecount.actions.*;
import org.appdynamics.appdrestapi.RESTAccess;
import org.appdynamics.appdrestapi.data.*;
import org.appdynamics.appdrestapi.resources.s;
import org.appdynamics.appdrestapi.util.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;


import java.util.logging.Logger;
import java.util.logging.Level;

/**
 *
 * @author soloink
 */
public class ApplicationLicenseCount extends LicenseCount{
    private static Logger logger=Logger.getLogger(ApplicationLicenseCount.class.getName());
    private String applicationName;
    private int applicationId;
    private int tierCount=0;
    private ArrayList<EUMPageLicenseCount> eumPages=new ArrayList<EUMPageLicenseCount>();
    private HashMap<Integer,Tier> tierLicensesNoNodes=new HashMap<Integer,Tier>(); 
    private HashMap<Integer,TierLicenseCount> tierLicenses=new HashMap<Integer,TierLicenseCount>(); 
    private ArrayList<ApplicationLicenseRange> appLicenseRange= new ArrayList<ApplicationLicenseRange>();
    private ArrayList<AppHourLicenseRange> appHourLicenseRange=new ArrayList<AppHourLicenseRange>();
    private ArrayList<String> dotNetKeys=new ArrayList<String>();
    private ApplicationLicenseRange totalRangeValue;
    private HashMap<String,ArrayList<Node>> dotNetMap=new HashMap<String,ArrayList<Node>>();
    private HashMap<String,ArrayList<String>> dotNetMapLog=new HashMap<String,ArrayList<String>>();
    
    

    public ApplicationLicenseCount(){super();}
    
    public ApplicationLicenseCount(String applicationName, int id){
        super();
        this.applicationId=id;
        this.applicationName=applicationName;
        
    }
    
    /*
     * This count is going to be a bit more involved, it will require more time because we are going to gather all of the timeranges and 
     * then request the availability for that time.
     */
    public void populateLicense(Nodes nodes, RESTAccess access, ArrayList<TimeRange> listOfTimes, 
            TimeRange totalTimeRange){
        // In this scenario we are going to zero out the minutes, seconds, hours of 
        //ArrayList<LicenseRange> listOfTimes=getTimeRange(interval);
        if(nodes == null){ 
            logger.log(Level.SEVERE,"The node query for application {0} did not return any nodes.",applicationName);
            return;
        }
        
        if(s.debugLevel >= 2) 
            logger.log(Level.INFO,new StringBuilder().append("Application ").append(applicationName).append(" has ").append(nodes.getNodes().size()).append(" nodes.").toString());
        
        totalRangeValue=new ApplicationLicenseRange("Total Application Count");
        totalRangeValue.setStart(totalTimeRange.getStart());
        totalRangeValue.setEnd(totalTimeRange.getEnd());
        
        
        //If just in case we don't have any nodes
        
        Tiers tiers = access.getTiersForApplication(applicationId);
        if(tiers == null){ 
            logger.log(Level.SEVERE,"The tier query for application {0} did not return any tiers.",applicationName);
            return;
        }
            
        /*
            We are going to create all of the tier
        */
        for(Tier tier:tiers.getTiers()){
            tierLicenses.put(tier.getId(),new TierLicenseCount(tier));
        }
        
        for(Node node:nodes.getNodes()){
            //This is going to create the NodeLicense and return it.
            //NodeLicenseCount nodeL=tierLicenses.get(node.getTierId()).addNodeRange(node);
            tierLicenses.get(node.getTierId()).addNodeRange(node); 
        }
        
        /*
         This is where we clean up any tiers that don't have nodes
        */
        Iterator<Integer> tierIt=tierLicenses.keySet().iterator();
        while(tierIt.hasNext()){
            Integer id = tierIt.next();
            if(tierLicenses.get(id).getNodeLicenseCount().isEmpty()){
                tierLicensesNoNodes.put(id, tierLicenses.get(id).getTier());
                tierIt.remove();
            }
        }
        
        
        
        /*
         * Now that we have all of the nodes we are going to get all of the tiers to count
         * the nodes. 
        */
        
        ThreadExecutor execTiers = new ThreadExecutor(5);
        
        for(TierLicenseCount tCount: tierLicenses.values()){
            
            // tCount.populateNodeLicenseRange(totalTimeRange, listOfTimes, access, applicationName);
            TierExecutor tierExec = new TierExecutor(tCount, access, applicationName, totalTimeRange, listOfTimes);
            execTiers.getExecutor().execute(tierExec);
        }
        
        execTiers.getExecutor().shutdown();
        execTiers.shutdown();
        
        /*
            This is going to be captured the EUM data
            First we are going to get the list of pages for this app.
        
        */
        
        if(LicenseS.EUM_V){
            // First we are going to get the pages
            if(s.debugLevel > 1) logger.log(Level.INFO,"EUM Pages License count requested");
            // logger.log(Level.INFO,"The info totalTimeRange " + totalTimeRange + " -- " + listOfTimes.size());
            MetricItems eumIT=access.getBaseMetricListPath(applicationName, LicenseS.EUM_PAGES_PATH);
            if(eumIT != null){
                for(MetricItem mi:eumIT.getMetricItems()){
                    // if(s.debugLevel > 2) logger.log(Level.INFO,new StringBuilder().append("EUM page ").append(mi.getName()).append(" the license usage for this page will be counted").toString());
                    EUMPageLicenseCount epc=new EUMPageLicenseCount(mi.getName(),mi.getName());
                    epc.setTotalRangeValue(new EUMPageLicenseRange());
                    eumPages.add(epc);
                }

                ThreadExecutor execNodes = new ThreadExecutor(4);

                for(EUMPageLicenseCount pageL:eumPages){
                    //public NodeExecutor(NodeLicenseCount nodeLic,RESTAccess access, 
                    //String appName, TimeRange totalTimeRange, 
                    //ArrayList<TimeRange> timeRanges, String tierAgentType)
                    EUMExecuter eumExec = new EUMExecuter(pageL, access, applicationName, totalTimeRange, listOfTimes);
                    execNodes.getExecutor().execute(eumExec);
                    //nodeL.populateNodeLicenseRange(totalTimeRange, timeRanges, access, applicationName, tierAgentType);
                }
                execNodes.getExecutor().shutdown();
                execNodes.shutdown();
            }else{
                logger.log(Level.WARNING,"EUM page license count requested but none returned from base query.");
            }
            
        }
        
    }



    public ArrayList<EUMPageLicenseCount> getEumPages() {
        return eumPages;
    }

    public void setEumPages(ArrayList<EUMPageLicenseCount> eumPages) {
        this.eumPages = eumPages;
    }

    
    public HashMap<Integer, Tier> getTierLicensesNoNodes() {
        return tierLicensesNoNodes;
    }

    public void setTierLicensesNoNodes(HashMap<Integer, Tier> tierLicensesNoNodes) {
        this.tierLicensesNoNodes = tierLicensesNoNodes;
    }
                   
    

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public int getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(int applicationId) {
        this.applicationId = applicationId;
    }

    public HashMap<Integer, TierLicenseCount> getTierLicenses() {
        return tierLicenses;
    }

    public void setTierLicenses(HashMap<Integer, TierLicenseCount> tierLicenses) {
        this.tierLicenses = tierLicenses;
    }

    public ArrayList<ApplicationLicenseRange> getAppLicenseRange() {
        return appLicenseRange;
    }

    public void setAppLicenseRange(ArrayList<ApplicationLicenseRange> appLicenseRange) {
        this.appLicenseRange = appLicenseRange;
    }

    public int getTierCount() {
        return tierCount;
    }

    public void setTierCount(int tierCount) {
        this.tierCount = tierCount;
    }

    public ArrayList<AppHourLicenseRange> getAppHourLicenseRange() {
        return appHourLicenseRange;
    }

    public void setAppHourLicenseRange(ArrayList<AppHourLicenseRange> appHourLicenseRange) {
        this.appHourLicenseRange = appHourLicenseRange;
    }

    public ApplicationLicenseRange getTotalRangeValue() {
        return totalRangeValue;
    }

    public void setTotalRangeValue(ApplicationLicenseRange totalRangeValue) {
        this.totalRangeValue = totalRangeValue;
    }


    public void populateDotNetMap(){
        dotNetMap=new HashMap<String,ArrayList<Node>>();
        for(TierLicenseCount tier: tierLicenses.values()){
            for(NodeLicenseCount nCount: tier.getNodeLicenseCount()){
                if(nCount.getType() == 1 ){
                    if(nCount.getType() == 1){
                        if(!dotNetMap.containsKey(nCount.getNode().getMachineName())) 
                            dotNetMap.put(nCount.getNode().getMachineName(), new ArrayList<Node>());
                        
                        //logger.log(Level.INFO, new StringBuilder().append("Add DotNet Node ").append(nCount.getNode().getName()).append(" - ").append(nCount.getNode().getMachineName()).toString());
                        dotNetMap.get(nCount.getNode().getMachineName()).add(nCount.getNode());
                    }

                }
            }
        }
        
        
        logger.log(Level.INFO,"Start to get the license weights for the nodes " + dotNetMap.keySet().size());
        // This is now going to get the counts for the .Net and php agents.
        for(String key: dotNetMap.keySet()){
            double size = dotNetMap.get(key).size();
            double piePiece=1/size;
            StringBuilder bud=new StringBuilder();
            StringBuilder scr=new StringBuilder().append("DotNet license for ").append(key).append(" is used by ")
                    .append(size).append(" nodes.\n");
            ArrayList<String> mLog=new ArrayList<String>();
            mLog.add(scr.toString());
            bud.append(scr);
            for(Node node: dotNetMap.get(key)){
                //For every node in the array, we are going to add this to the count.
                //node.updateLicenseWeight(piePiece, node);
                //iis+=piePiece;
                scr=new StringBuilder().append("\tDotNet license usage in tier '").append(node.getTierName()).append("' for node '").append(node.getName())
                        .append("'\n");
                bud.append(scr.toString());
                mLog.add(scr.toString());
            }
            if(!mLog.isEmpty()) dotNetMapLog.put(key, mLog);
            logger.log(Level.INFO,bud.toString());
        }
                
        
    }

    public HashMap<String, ArrayList<String>> getDotNetMapLog() {
        return dotNetMapLog;
    }

    public void setDotNetMapLog(HashMap<String, ArrayList<String>> dotNetMapLog) {
        this.dotNetMapLog = dotNetMapLog;
    }

    
    public HashMap<String, ArrayList<Node>> getDotNetMap() {
        return dotNetMap;
    }

    public void setDotNetMap(HashMap<String, ArrayList<Node>> dotNetMap) {
        this.dotNetMap = dotNetMap;
    }

    public ArrayList<String> getDotNetKeys() {
        return dotNetKeys;
    }

    public void setDotNetKeys(ArrayList<String> dotNetKeys) {
        this.dotNetKeys = dotNetKeys;
    }
    
    

    @Override
    public String toString(){
        StringBuilder bud=new StringBuilder();
        bud.append("Application Name: ").append(applicationName).append("\n");
        bud.append("Total Tier Count: ").append(tierLicenses.size()).append("\n");
        bud.append("------------------- Application Time Ranges --------------------------------\n");
        for(ApplicationLicenseRange tRange: appLicenseRange){
            bud.append(tRange.toString());
        }
        bud.append("------------------- Tier Time Ranges --------------------------------\n");
        for(TierLicenseCount tLic:tierLicenses.values()){
            bud.append(tLic.toString());
        }
        return bud.toString();
    }
    
}
