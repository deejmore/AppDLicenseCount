/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.appdynamics.licensecount.file;

import org.appdynamics.appdrestapi.resources.s;
import org.appdynamics.licensecount.resources.LicenseS;
import org.appdynamics.licensecount.data.*;
import org.appdynamics.appdrestapi.data.Tier;
import org.appdynamics.appdrestapi.data.Node;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;

import java.util.logging.Logger;
import java.util.logging.Level;

import org.apache.poi.ss.usermodel.HorizontalAlignment;

/**
 *
 * @author gilbert.solorzano
 */
public class WriteExcelDoc {
    private static Logger logger=Logger.getLogger(WriteExcelDoc.class.getName());
    private HashMap<String, ArrayList<ApplicationLicenseCount>> groupings=new HashMap<String, ArrayList<ApplicationLicenseCount>>();
    private CustomerLicenseCount customer;
    private String licensePath;
    private XSSFCellStyle style;
    
    public WriteExcelDoc(){}
    
    public WriteExcelDoc(CustomerLicenseCount customer){this.customer=customer;}
    
    public void init(){
        XSSFWorkbook workbook = new XSSFWorkbook(); 
         
        /* This will create the sheets.  */
        XSSFSheet licenseSummary = workbook.createSheet(LicenseS.LICENSE_SUMMARY);
        XSSFSheet licenseTiers = workbook.createSheet(LicenseS.TIER_SUMMARY);
        XSSFSheet licenseHourlyTiers = workbook.createSheet(LicenseS.HOURLY_TIER_SUMMARY);
        XSSFSheet licensedHourlyNodes=null;
        XSSFSheet licensedEUM=null;
        if(LicenseS.NODE_V) licensedHourlyNodes=workbook.createSheet(LicenseS.HOURLY_NODE_SUMMARY);
        if(LicenseS.EUM_V){ 
            licensedEUM=workbook.createSheet(LicenseS.EUM_SUMMARY);
            addEUMPages(licensedEUM);
        }
        XSSFSheet licenseNodeInfo = workbook.createSheet(LicenseS.NODE_INFO_SUMMARY);
        XSSFSheet licenseNoNodeTiers = workbook.createSheet(LicenseS.TIERS_WITH_NO_NODES);
        XSSFSheet licenseDotNetNodeMap = workbook.createSheet(LicenseS.DOTNET_NODE_MAP);
        
        style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.RIGHT);
        
        /**  Here we are going to add the  information  **/
        addNodeInfo(licenseNodeInfo);
        addTierWNoNodeInfo(licenseNoNodeTiers);
        addDotNetMap(licenseDotNetNodeMap);
        // Lets create the first row which will be the header.
        int headerRowIndex=0;
        Row headerRow = licenseSummary.createRow(headerRowIndex);
        Row tierRow = licenseTiers.createRow(headerRowIndex);
        Row hourlyTierRow = licenseHourlyTiers.createRow(headerRowIndex);
        
        int i=0;
        Cell cell_1 = headerRow.createCell(i);cell_1.setCellValue(LicenseS.CUSTOMER_NAME);
        
        Cell cell_2 = tierRow.createCell(i);cell_2.setCellValue(LicenseS.APPLICATION_NAME);
        cell_2 = tierRow.createCell(i+1);cell_2.setCellValue(LicenseS.TIER_NAME);
        
        Cell cell_3 = hourlyTierRow.createCell(i);cell_3.setCellValue(LicenseS.APPLICATION_NAME);
        cell_3 = hourlyTierRow.createCell(i+1);cell_3.setCellValue(LicenseS.TIER_NAME);
        
        Cell cell_4=null;
         if(LicenseS.NODE_V) {
             Row hourlyNodeRow = licensedHourlyNodes.createRow(headerRowIndex);
             cell_4 = hourlyNodeRow.createCell(i);cell_4.setCellValue(LicenseS.APPLICATION_NAME);
             cell_4 = hourlyNodeRow.createCell(i + 1);cell_4.setCellValue(LicenseS.TIER_NAME);
             cell_4 = hourlyNodeRow.createCell(i + 2);cell_4.setCellValue(LicenseS.NODE_NAME);
                     }
        i+=2;
        
        int columnCount=2;
        int columnCount1=3;
        // Create the date headers
        for(CustomerLicenseRange cRange:customer.getCustomerRangeValues()){
            cell_1=headerRow.createCell(columnCount);cell_1.setCellValue(cRange.getColumnName());
            cell_2=tierRow.createCell(columnCount1);cell_2.setCellValue(cRange.getColumnName());
            columnCount++;columnCount1++;
        }
        
        
        
        i=addCustomer(licenseSummary, i);  
        //logger.log(Level.INFO,"Next row " + ++i);
        headerRow = licenseSummary.createRow(++i);
        cell_1 = headerRow.createCell(0);cell_1.setCellValue(LicenseS.APPLICATION_NAME);
        i++;
        int tierRowCount=2;
        int nodeRowCount=2;
        int createdHourlyTierHeader=0;
        int createdHourlyNodeHeader=0;
        columnCount1=3;
        
        //logger.log(Level.INFO,new StringBuilder().append("\n\n\tNumber of applications ").append(customer.getApplications().size()).toString());
        for(ApplicationLicenseCount app: customer.getApplications().values()){
            i=addApplication(licenseSummary,i,app);
            int inCount=0;
            for(TierLicenseCount tier: app.getTierLicenses().values()){
                if(createdHourlyTierHeader == 0){
                    for(TierHourLicenseRange tr:tier.getTierHourLicenseRange()){
                        cell_3=hourlyTierRow.createCell(columnCount1);cell_3.setCellValue(tr.getHourColumnName());
                        columnCount1++;
                    }
                    createdHourlyTierHeader=1;
                }
                tierRowCount=addTier(licenseTiers,tierRowCount,tier,app.getApplicationName(), inCount);
                inCount++;
            }
            if(inCount != 0) tierRowCount++;
            i++;
        }
        
        tierRowCount=2;
        for(ApplicationLicenseCount app: customer.getApplications().values()){
            //i=addApplication(licenseSummary,i,app);
            int inCount=0;
            tierRowCount=addHourlyApp(licenseHourlyTiers,tierRowCount,app, inCount);
            
            for(TierLicenseCount tier: app.getTierLicenses().values()){
                tierRowCount=addHourlyTier(licenseHourlyTiers,tierRowCount,tier,app.getApplicationName(), inCount);
                inCount++;
            }
            tierRowCount++;
            i++;
        }
        
        //create grouped BU excel sheet
        if (!LicenseS.GROUP_V.isEmpty()) {
            XSSFSheet BULicenseSummary = workbook.createSheet(LicenseS.BU_LICENSE_SUMMARY);
           
            //parse through file to see if there is key & value
            customer.aggregateByGroup(customer.getApplications());
            groupings = customer.getGroupings();
            
            Row groupedTierRow = BULicenseSummary.createRow(0);

            cell_4 = groupedTierRow.createCell(0);cell_4.setCellValue(LicenseS.GROUP_NAME);
            cell_4 = groupedTierRow.createCell(1);cell_4.setCellValue(LicenseS.APPLICATION_NAME);
            int groupColumnCount = 3;
            for(CustomerLicenseRange cRange:customer.getCustomerRangeValues()){
                cell_4=groupedTierRow.createCell(groupColumnCount);cell_4.setCellValue(cRange.getColumnName());
                groupColumnCount++;
            }
            
            //iterate through all groups for BU summary license file
            int groupIndex = 1;
            for(Entry<String, ArrayList<ApplicationLicenseCount>> entry : groupings.entrySet() ) {
                groupIndex = addGroupInfo(BULicenseSummary, groupIndex, entry);
            }
        }

        try
        {
            //Write the workbook in file system
            //String fileName=new StringBuilder().append("/Users/gilbert.solorzano/Documents/").append(customer.getName()).append("LicenseFile.xlsx").toString();
            
            FileOutputStream out = new FileOutputStream(new File(LicenseS.FILENAME_V));
            workbook.write(out);
            out.close();
            StringBuilder bud=new StringBuilder();
            bud.append("Completed writing the file: ").append(LicenseS.FILENAME_V).append(".");
            logger.log(Level.INFO,bud.toString());
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
        }
    }

    public int addGroupInfo(XSSFSheet curSheet, int rowIndex, Entry<String, ArrayList<ApplicationLicenseCount>> group) {
        // This going to add the customer information.
        int tempRowIndex=rowIndex;
        int limit = group.getValue().size();
        int j = 0;
        int increment;
        
        ArrayList<Row> rows=new ArrayList<Row>();
        
        if (limit > 6) {
            increment = limit;
        } else { increment = 7; }
        
        for(int i=rowIndex; i < (rowIndex + increment); i++){
            rows.add(curSheet.createRow(i));
        }
        
        Cell cell=null;
        //logger.log(Level.INFO,"The size of the array is rows " + rows.size() + ", the limit is " + limit );
        
        for(int i=0; i <  7 || j < limit ;i++){
            //logger.log(Level.INFO,"The value of i is " + i + ", the size of j is " + j);
            if ( j < limit ) {
                cell = rows.get(i).createCell(1);
                cell.setCellValue(group.getValue().get(j).getApplicationName());
                j++;
            } 
            
            switch(i){
            
                case 0:
                    cell = rows.get(i).createCell(0);
                    cell.setCellValue(group.getKey());
                    
                    cell = rows.get(i).createCell(2);
                    cell.setCellValue(LicenseS.TOTAL_AGENT_COUNT);
                    tempRowIndex++;
                    break;
                case 1:
                    cell = rows.get(i).createCell(2);
                    cell.setCellValue(LicenseS.JAVA_AGENT_COUNT);
                    tempRowIndex++;
                    break;
               
                case 2:
                    cell = rows.get(i).createCell(2);
                    cell.setCellValue(LicenseS.DOTNET_AGENT_COUNT);
                    tempRowIndex++;
                    break;
                    
                case 3:
                    cell = rows.get(i).createCell(2);
                    cell.setCellValue(LicenseS.PHP_AGENT_COUNT);
                    tempRowIndex++;
                    break;
                
                case 4:
                    cell = rows.get(i).createCell(2);
                    cell.setCellValue(LicenseS.NODEJS_AGENT_COUNT);
                    tempRowIndex++;
                    break;
                    
                case 5:
                    cell = rows.get(i).createCell(2);
                    cell.setCellValue(LicenseS.WEBSERVER_AGENT_COUNT);
                    tempRowIndex++;
                    break;
                    
                case 6:
                    cell = rows.get(i).createCell(2);
                    cell.setCellValue(LicenseS.MACHINE_AGENT_COUNT);
                    tempRowIndex++;
                    break;
                default:
                    break;
            }

        }
                
        //just get one app
        ApplicationLicenseCount appRef = group.getValue().get(0);
        int columnCount=3;

        //for each date range
        
        for (ApplicationLicenseRange cRange: appRef.getAppLicenseRange()) {
            //for every app in the group
            double totalCount = 0, javaCount = 0, iisCount = 0, iisInternalCount = 0, phpCount = 0, nodeJsCount = 0, machineCount = 0, webserverCount=0;
            
            for (ApplicationLicenseCount app : group.getValue()) {
                //for each day
                for (int i = 0 ; i <  7; i++ ) {
                    if(app.getAppLicenseRange().get(i).getEnd() == cRange.getEnd()) {
                        totalCount += app.getAppLicenseRange().get(i).getTotalCount();
                        javaCount += app.getAppLicenseRange().get(i).getJavaCount();
                        iisCount += app.getAppLicenseRange().get(i).getIisCount();
                        iisInternalCount += app.getAppLicenseRange().get(i).getIisInternalCount();
                        phpCount += app.getAppLicenseRange().get(i).getPhpCount();
                        nodeJsCount += app.getAppLicenseRange().get(i).getNodeJSCount();
                        machineCount += app.getAppLicenseRange().get(i).getMachineCount();
                        webserverCount += app.getAppLicenseRange().get(i).getWebserverCount();
                        break;
                    }
                }
            }
            //now loop down the list to print the values
            for (int i = 0; i < 7; i++) {
                switch(i){
                case 0: //Total Count
                    cell = rows.get(i).createCell(columnCount);
                    cell.setCellValue(totalCount);
                    break;
                case 1: //Java Agent
                    cell = rows.get(i).createCell(columnCount);
                    cell.setCellValue(javaCount);
                    break;

                case 2: //DotNet Agent
                    cell = rows.get(i).createCell(columnCount);
                    //cell.setCellValue(cRange.getIisCount());
                    cell.setCellValue( new Double(iisCount).intValue() + "(" + new Double (iisInternalCount).intValue() + ")");
                    cell.setCellStyle(style);
                    break;

                case 3: //PHP Agent
                    cell = rows.get(i).createCell(columnCount);
                    cell.setCellValue(phpCount);
                    break;

                case 4: //NodeJS Agent
                    cell = rows.get(i).createCell(columnCount);
                    
                    cell.setCellValue(LicenseS.licenseRound(nodeJsCount/10) + "(" + new Double(nodeJsCount).intValue() + ")");
                    cell.setCellStyle(style);
                    break;
                    
                case 5:
                    cell = rows.get(i).createCell(columnCount);
                    cell.setCellValue(webserverCount);
                    break;
                    
                case 6: //Machine Agent
                    cell = rows.get(i).createCell(columnCount);
                    cell.setCellValue(machineCount);
                    break;
                    
                default:
                    break;
                }
            }
            columnCount++;
        }
        if ( j > 6 ) { return tempRowIndex + j - 6 + 2; }
        return tempRowIndex + 2; 
    }

    public int addCustomer(XSSFSheet curSheet, int rowIndex){
        // This going to add the customer information.
        int tempRowIndex=rowIndex;
        ArrayList<Row> rows=new ArrayList<Row>();
        
        // Was 7
        for(int i=rowIndex; i < (rowIndex + LicenseS.A_INDEX); i++){
            rows.add(curSheet.createRow(i));
        }
        
        Cell cell=null;
        for(int i=0; i <  LicenseS.A_INDEX;i++){
            switch(i){
                case 0:
                    cell = rows.get(i).createCell(0);
                    cell.setCellValue(customer.getName());
                    cell = rows.get(i).createCell(1);
                    cell.setCellValue(LicenseS.TOTAL_AGENT_COUNT);
                    tempRowIndex++;
                    break;
                case 1:
                    cell = rows.get(i).createCell(1);
                    cell.setCellValue(LicenseS.JAVA_AGENT_COUNT);
                    tempRowIndex++;
                    break;
               
                case 2:
                    cell = rows.get(i).createCell(1);
                    cell.setCellValue(LicenseS.DOTNET_AGENT_COUNT);
                    tempRowIndex++;
                    break;
                    
                case 3:
                    cell = rows.get(i).createCell(1);
                    cell.setCellValue(LicenseS.PHP_AGENT_COUNT);
                    tempRowIndex++;
                    break;
                
                case 4:
                    cell = rows.get(i).createCell(1);
                    cell.setCellValue(LicenseS.NODEJS_AGENT_COUNT);
                    tempRowIndex++;
                    break;
                case 5:
                    cell = rows.get(i).createCell(1);
                    cell.setCellValue(LicenseS.WEBSERVER_AGENT_COUNT);
                    tempRowIndex++;        
                    break;
                
                case 6:
                    cell = rows.get(i).createCell(1);
                    cell.setCellValue(LicenseS.NATIVE_SDK_AGENT_COUNT);
                    tempRowIndex++;        
                    break;
                    
                case 7:
                    cell = rows.get(i).createCell(1);
                    cell.setCellValue(LicenseS.MACHINE_AGENT_COUNT);
                    tempRowIndex++;
                    break;
                default:
                    break;
            }

        }
        
        int columnCount=2;
        for(CustomerLicenseRange cRange:customer.getCustomerRangeValues()){
            
            for(int i=0; i < LicenseS.A_INDEX;i++){
                switch(i){
                    case 0: //Total Count
                        cell = rows.get(i).createCell(columnCount);
                        cell.setCellValue(cRange.getTotalCount());
                        break;
                    case 1: //Java Agent
                        cell = rows.get(i).createCell(columnCount);
                        cell.setCellValue(cRange.getJavaCount());
                        break;

                    case 2: //DotNet Agent
                        cell = rows.get(i).createCell(columnCount);
                        //cell.setCellValue(cRange.getIisCount());
                        cell.setCellValue(cRange.getDotNetCount());
                        cell.setCellStyle(style);
                        break;

                    case 3: //PHP Agent
                        cell = rows.get(i).createCell(columnCount);
                        cell.setCellValue(cRange.getPhpCount());
                        break;

                    case 4: //NodeJS Agent
                        cell = rows.get(i).createCell(columnCount);
                        
                        cell.setCellValue(cRange.getNodeJSCount_C());
                        cell.setCellStyle(style);
                        break;
                        
                    case 5: // WebServer
                        cell = rows.get(i).createCell(columnCount);
                        cell.setCellValue(cRange.getWebserverCount());
                        break;
                        
                    case 6: // NativeSDK
                        cell = rows.get(i).createCell(columnCount);
                        cell.setCellValue(cRange.getNativeSDKCount());
                        break;
                        
                    case 7://Machine Agent
                        cell = rows.get(i).createCell(columnCount);
                        cell.setCellValue(cRange.getMachineCount());
                        break;
                        
                    default:
                        break;
                }
            }
            columnCount++;
        }
        return tempRowIndex++;
    }
    
    public int addApplication(XSSFSheet curSheet, int rowIndex, ApplicationLicenseCount appLicenseCount){
        // This going to add the customer information.
        int tempRowIndex=rowIndex;
        ArrayList<Row> rows=new ArrayList<Row>();
        for(int i=rowIndex; i < (rowIndex + LicenseS.A_INDEX); i++){
            rows.add(curSheet.createRow(i));
        }
        
        Cell cell=null;
        for(int i=0; i <  LicenseS.A_INDEX;i++){
            switch(i){
                case 0:
                    cell = rows.get(i).createCell(0);
                    cell.setCellValue(appLicenseCount.getApplicationName());
                    cell = rows.get(i).createCell(1);
                    cell.setCellValue(LicenseS.TOTAL_AGENT_COUNT);
                    tempRowIndex++;
                    break;
                case 1:
                    cell = rows.get(i).createCell(1);
                    cell.setCellValue(LicenseS.JAVA_AGENT_COUNT);
                    tempRowIndex++;
                    break;
               
                case 2:
                    cell = rows.get(i).createCell(1);
                    cell.setCellValue(LicenseS.DOTNET_AGENT_COUNT);
                    tempRowIndex++;
                    break;
                    
                case 3:
                    cell = rows.get(i).createCell(1);
                    cell.setCellValue(LicenseS.PHP_AGENT_COUNT);
                    tempRowIndex++;
                    break;
                
                case 4:
                    cell = rows.get(i).createCell(1);
                    cell.setCellValue(LicenseS.NODEJS_AGENT_COUNT);
                    tempRowIndex++;
                    break;
                    
                case 5:
                    cell = rows.get(i).createCell(1);
                    cell.setCellValue(LicenseS.WEBSERVER_AGENT_COUNT);
                    tempRowIndex++;
                    break;   
                    
                case 6:
                    cell = rows.get(i).createCell(1);
                    cell.setCellValue(LicenseS.NATIVE_SDK_AGENT_COUNT);
                    tempRowIndex++;
                    break;
                    
                case 7:
                    cell = rows.get(i).createCell(1);
                    cell.setCellValue(LicenseS.MACHINE_AGENT_COUNT);
                    tempRowIndex++;
                    break;
                default:
                    break;
            }

        }
        
        int columnCount=2;
        for(ApplicationLicenseRange cRange:appLicenseCount.getAppLicenseRange()){
            
            for(int i=0; i < LicenseS.A_INDEX;i++){
                switch(i){
                    case 0: //Total Count
                        cell = rows.get(i).createCell(columnCount);
                        cell.setCellValue(cRange.getTotalCount());
                        break;
                    case 1: //Java Agent
                        cell = rows.get(i).createCell(columnCount);
                        cell.setCellValue(cRange.getJavaCount());
                    
                        break;

                    case 2: //DotNet Agent
                        cell = rows.get(i).createCell(columnCount);
                        //logger.log(Level.INFO,new StringBuilder().append("Adding .Net ").append(cRange.getIisCount()).toString());
                        //cell.setCellValue(new Double(cRange.getIisCount()));
                        cell.setCellValue(cRange.getDotNetCount());
                        cell.setCellStyle(style);
      
                        break;

                    case 3: //PHP Agent
                        cell = rows.get(i).createCell(columnCount);
                        cell.setCellValue(new Double(cRange.getPhpCount()));

                        break;

                    case 4: //NodeJS Agent
                        cell = rows.get(i).createCell(columnCount);
                        cell.setCellValue(cRange.getNodeJSCount_TA());
                        cell.setCellStyle(style);
     
                        break;
                        
                    case 5: //WebServer
                        cell = rows.get(i).createCell(columnCount);
                        cell.setCellValue(cRange.getWebserverCount());
                        break;
                        
                    case 6: //Native SDK
                        cell = rows.get(i).createCell(columnCount);
                        cell.setCellValue(cRange.getNativeSDKCount());
                        break;
                        
                    case 7: //MachineAgent
                        cell = rows.get(i).createCell(columnCount);
                        cell.setCellValue(cRange.getMachineCount());
   
                        break;
                        
                    default:
                        break;
                }
            }
            columnCount++;
        }
        return tempRowIndex++;
    }
    
    public int addTier(XSSFSheet curSheet, int rowIndex, TierLicenseCount tLicenseCount, String appName, int inCount){
        // This going to add the customer information.
        int tempRowIndex=rowIndex;
        ArrayList<Row> rows=new ArrayList<Row>();
        for(int i=rowIndex; i < (rowIndex + LicenseS.A_INDEX); i++){
            rows.add(curSheet.createRow(i));
        }
        
        Cell cell=null;
        for(int i=0; i <  LicenseS.A_INDEX;i++){
            switch(i){
                case 0:
                    if(inCount == 0){
                       cell = rows.get(i).createCell(0);
                       cell.setCellValue(appName); 
                    }
                    cell = rows.get(i).createCell(1);
                    cell.setCellValue(tLicenseCount.getName());
                    cell = rows.get(i).createCell(2);
                    cell.setCellValue(LicenseS.TOTAL_AGENT_COUNT);
                    tempRowIndex++;
                    break;
                case 1:
                    cell = rows.get(i).createCell(2);
                    cell.setCellValue(LicenseS.JAVA_AGENT_COUNT);
                    tempRowIndex++;
                    break;
               
                case 2:
                    cell = rows.get(i).createCell(2);
                    cell.setCellValue(LicenseS.DOTNET_AGENT_COUNT);
                    tempRowIndex++;
                    break;
                    
                case 3:
                    cell = rows.get(i).createCell(2);
                    cell.setCellValue(LicenseS.PHP_AGENT_COUNT);
                    tempRowIndex++;
                    break;
                
                case 4:
                    cell = rows.get(i).createCell(2);
                    cell.setCellValue(LicenseS.NODEJS_AGENT_COUNT);
                    tempRowIndex++;
                    break;
                    
                case 5:
                    cell = rows.get(i).createCell(2);
                    cell.setCellValue(LicenseS.WEBSERVER_AGENT_COUNT);
                    tempRowIndex++;
                    break;
                    
                case 6:
                    cell = rows.get(i).createCell(2);
                    cell.setCellValue(LicenseS.NATIVE_SDK_AGENT_COUNT);
                    tempRowIndex++;
                    break;
                    
                case 7:
                    cell = rows.get(i).createCell(2);
                    cell.setCellValue(LicenseS.MACHINE_AGENT_COUNT);
                    tempRowIndex++;
                    break;
                default:
                    break;
            }

        }
        
        int columnCount=3;
        for(TierLicenseRange cRange:tLicenseCount.getTierLicenseRange()){
            
            for(int i=0; i < LicenseS.A_INDEX;i++){
                switch(i){
                    case 0: //Total Count
                        cell = rows.get(i).createCell(columnCount);
                        cell.setCellValue(cRange.getTotalCount());
                        break;
                    case 1: //Java Agent
                        cell = rows.get(i).createCell(columnCount);
                        cell.setCellValue(cRange.getJavaCount());
                    
                        break;

                    case 2: //DotNet Agent
                        cell = rows.get(i).createCell(columnCount);
                        //logger.log(Level.INFO,new StringBuilder().append("Adding .Net ").append(cRange.getIisCount()).toString());
                        //cell.setCellValue(new Double(cRange.getIisCount()));
                        cell.setCellValue(cRange.getDotNetCount());
                        cell.setCellStyle(style);
                        break;

                    case 3: //PHP Agent
                        cell = rows.get(i).createCell(columnCount);
                        cell.setCellValue(new Double(cRange.getPhpCount()));

                        break;

                    case 4: //NodeJS Agent
                        cell = rows.get(i).createCell(columnCount);
                        cell.setCellValue(cRange.getNodeJSCount_TA());
                        cell.setCellStyle(style);
     
                        break;
                        
                    case 5: //WebServer Agent
                        cell = rows.get(i).createCell(columnCount);
                        cell.setCellValue(cRange.getWebserverCount());
                        break;
                        
                    case 6: //WebServer Agent
                        cell = rows.get(i).createCell(columnCount);
                        cell.setCellValue(cRange.getNativeSDKCount());
                        break;
                        
                    case 7: //Machine Agent
                        cell = rows.get(i).createCell(columnCount);
                        cell.setCellValue(cRange.getMachineCount());
   
                        break;
                        
                    default:
                        break;
                }
            }
            columnCount++;
        }
        tempRowIndex++;
        return tempRowIndex++;
    }
    
    public int addHourlyTier(XSSFSheet curSheet, int rowIndex, TierLicenseCount tLicenseCount, String appName, int inCount){
        // This going to add the customer information.
        int tempRowIndex=rowIndex;
        ArrayList<Row> rows=new ArrayList<Row>();
        for(int i=rowIndex; i < (rowIndex + 2); i++){
            rows.add(curSheet.createRow(i));
        }
        
        Cell cell=null;
        for(int i=0; i <  2;i++){
            switch(i){
                case 0:
                    if(inCount == 0){
                       //cell = rows.get(i).createCell(0);
                       //cell.setCellValue(appName); 
                    }
                    cell = rows.get(i).createCell(1);
                    cell.setCellValue(tLicenseCount.getName());
                    cell = rows.get(i).createCell(2);
                    cell.setCellValue(LicenseS.MACHINE_AGENT_COUNT);
                    tempRowIndex++;
                    break;
                case 1:
                    cell = rows.get(i).createCell(2);
                    cell.setCellValue(LicenseS.APPLICATION_AGENT_COUNT);
                    tempRowIndex++;
                    break;
               
                default:
                    break;
            }

        }
        
        int columnCount=3;
        for(TierHourLicenseRange cRange:tLicenseCount.getTierHourLicenseRange()){
            
            for(int i=0; i < 2;i++){
                switch(i){
                    case 0: //Total Count
                        cell = rows.get(i).createCell(columnCount);
                        cell.setCellValue(cRange.getMachineAgent());
                        break;
                    case 1: //Java Agent
                        cell = rows.get(i).createCell(columnCount);
                        cell.setCellValue(cRange.getAppAgent());
                    
                        break;

                        
                    default:
                        break;
                }
            }
            columnCount++;
        }
        tempRowIndex++;
        return tempRowIndex++;
    }
    
    public int addHourlyApp(XSSFSheet curSheet, int rowIndex, ApplicationLicenseCount appLicenseCount, int inCount){
        // This going to add the customer information.
        int tempRowIndex=rowIndex;
        ArrayList<Row> rows=new ArrayList<Row>();
        for(int i=rowIndex; i < (rowIndex + 2); i++){
            rows.add(curSheet.createRow(i));
        }
        
        Cell cell=null;
        for(int i=0; i <  2;i++){
            switch(i){
                case 0:
                    if(inCount == 0){
                        cell = rows.get(i).createCell(0);
                        cell.setCellValue(appLicenseCount.getApplicationName());
    
                        cell = rows.get(i).createCell(2);
                        cell.setCellValue(LicenseS.MACHINE_AGENT_COUNT);
                        tempRowIndex++;
                    }
                    break;
                case 1:
                    cell = rows.get(i).createCell(2);
                    cell.setCellValue(LicenseS.APPLICATION_AGENT_COUNT);
                    tempRowIndex++;
                    break;
               
                default:
                    break;
            }

        }
        
        int columnCount=3;
        for(AppHourLicenseRange cRange:appLicenseCount.getAppHourLicenseRange()){
            
            for(int i=0; i < 2;i++){
                switch(i){
                    case 0: //Total Count
                        cell = rows.get(i).createCell(columnCount);
                        cell.setCellValue(cRange.getMachineAgent());
                        break;
                    case 1: //Java Agent
                        cell = rows.get(i).createCell(columnCount);
                        cell.setCellValue(cRange.getAppAgent());
                    
                        break;

                        
                    default:
                        break;
                }
            }
            columnCount++;
        }
        tempRowIndex++;
        return tempRowIndex++;
    }
    
    public void addNodeInfo(XSSFSheet curSheet){
        // Create the header
        int row=0;
        Row mainRow = curSheet.createRow(row);
        row+=2;

   
        Cell cell = mainRow.createCell(0);
        cell.setCellValue(LicenseS.APPLICATION_NAME);
        cell = mainRow.createCell(1);
        cell.setCellValue(LicenseS.TIER_NAME);
        cell = mainRow.createCell(2);
        cell.setCellValue(LicenseS.NODE_NAME);
        cell = mainRow.createCell(3);
        cell.setCellValue(LicenseS.AGENT_TYPE);
        cell = mainRow.createCell(4);
        cell.setCellValue(LicenseS.AGENT_NAME_MACHINE_AGENT);
        cell = mainRow.createCell(5);
        cell.setCellValue(LicenseS.DESCRIPTION);
        
        Iterator<Integer> appIter = customer.getApplications().keySet().iterator();
        while(appIter.hasNext()){
            Integer appId = appIter.next();
            ApplicationLicenseCount appCount = customer.getApplications().get(appId);
            Iterator<Integer> tierIter = appCount.getTierLicenses().keySet().iterator();
            while(tierIter.hasNext()){
                Integer tierId = tierIter.next();
                TierLicenseCount tierCount = appCount.getTierLicenses().get(tierId);
                for(NodeLicenseCount nodeCount: tierCount.getNodeLicenseCount()){
                    mainRow = curSheet.createRow(row);
                    cell = mainRow.createCell(0);
                    cell.setCellValue(appCount.getApplicationName());
                    cell = mainRow.createCell(1);
                    cell.setCellValue(tierCount.getName());
                    cell = mainRow.createCell(2);
                    cell.setCellValue(new StringBuilder().append(nodeCount.getName()).append("(").append(nodeCount.getMachineName()).append(")").toString());
                    // This is when we pick the agent type
                    cell = mainRow.createCell(3);
                    if(nodeCount.getNode().isAppAgentPresent()){
                        cell.setCellValue(nodeCount.getAgentName(nodeCount.getType()));
                    }else{
                        cell.setCellValue(LicenseS.NONE);
                    }
                    
                    cell = mainRow.createCell(4);
                    if(nodeCount.getNode().isMachineAgentPresent()){
                        cell.setCellValue(LicenseS.PRESENT);
                    }else{
                        cell.setCellValue(LicenseS.NONE);
                    }
                    
                    cell = mainRow.createCell(5);
                    cell.setCellValue(getDescription(nodeCount));
                    row++;
                }
            }
        }
        
        
    }
    
    public void addTierWNoNodeInfo(XSSFSheet curSheet){
        // Create the header
        int row=0;
        Row mainRow = curSheet.createRow(row);
        row+=2;

   
        Cell cell = mainRow.createCell(0);
        cell.setCellValue(LicenseS.APPLICATION_NAME);
        cell = mainRow.createCell(1);
        cell.setCellValue(LicenseS.TIER_ID);
        cell = mainRow.createCell(2);
        cell.setCellValue(LicenseS.TIER_NAME);
        cell = mainRow.createCell(3);
        cell.setCellValue(LicenseS.TIER_TYPE);
        cell = mainRow.createCell(4);
        cell.setCellValue(LicenseS.TIER_AGENT_TYPE);
        
        
        Iterator<Integer> appIter = customer.getApplications().keySet().iterator();
        while(appIter.hasNext()){
            Integer appId = appIter.next();
            ApplicationLicenseCount appCount = customer.getApplications().get(appId);
            Iterator<Integer> tierIter = appCount.getTierLicensesNoNodes().keySet().iterator();
            while(tierIter.hasNext()){
                Integer tierId = tierIter.next();
                Tier tierCount = appCount.getTierLicensesNoNodes().get(tierId);
                    mainRow = curSheet.createRow(row);
                    cell = mainRow.createCell(0);
                    cell.setCellValue(appCount.getApplicationName());
                    cell = mainRow.createCell(1);
                    cell.setCellValue(tierCount.getId());
                    cell = mainRow.createCell(2);
                    cell.setCellValue(tierCount.getName());
                    // This is when we pick the agent type
                    cell = mainRow.createCell(3);
                    cell.setCellValue(tierCount.getType());
                    cell = mainRow.createCell(4);
                    cell.setCellValue(tierCount.getAgentType());
                    row++;
            }
        }
        
        
    }
    
    //DotNet
    public void addDotNetMap(XSSFSheet curSheet){
        // Create the header
        int row=0;
        String appName=null;
        Row mainRow = curSheet.createRow(row);
   
        Cell cell = mainRow.createCell(0);
        cell.setCellValue(LicenseS.WINDOWS_HOST);
        cell = mainRow.createCell(1);
        cell.setCellValue(LicenseS.APPLICATION_NAME); //APPLICATION_NAME
        cell = mainRow.createCell(2);
        cell.setCellValue(LicenseS.WINDOWS_MAPPING);

        
        // we have a new map in the form of new HashMap<String,HashMap<Integer,ArrayList<String>>>
        Iterator<String> machineIter = customer.getDotNetMapLog().keySet().iterator();
        while(machineIter.hasNext()){
            String machineName = machineIter.next();
            HashMap<Integer,ArrayList<String>> dotNetCnt = customer.getDotNetMapLog().get(machineName);
                 
            Iterator<Integer> appIter = dotNetCnt.keySet().iterator();
            
            // These are the keys which are the apps.
            while(appIter.hasNext()){
                
                Integer _key=appIter.next();
                appName = customer.getApplications().get(_key).getApplicationName();

                
                for(String nn:dotNetCnt.get(_key)){
                        row++;
                        mainRow = curSheet.createRow(row);
                        cell = mainRow.createCell(0);
                        cell.setCellValue(machineName);
            
                        cell = mainRow.createCell(1);
                        cell.setCellValue(appName);
                        
                        cell = mainRow.createCell(2);
                        cell.setCellValue(nn);
                }
                                                
            }

        }
        
        
    }
    
    public void addEUMPages(XSSFSheet curSheet){
        // Create the header
        int row=0;
        String appName=null;
        Row mainRow = curSheet.createRow(row);
   
        Cell cell = mainRow.createCell(0);
        cell.setCellValue(LicenseS.CUSTOMER_NAME);
        cell = mainRow.createCell(1);
        cell.setCellValue(LicenseS.APPLICATION_NAME); //APPLICATION_NAME
        cell = mainRow.createCell(2);
        cell.setCellValue(LicenseS.EUM_PAGE_NAME);
        
                
        // Create the date headers
        int columnCount=3;
        for(CustomerLicenseRange cRange:customer.getCustomerRangeValues()){
            cell=mainRow.createCell(columnCount);cell.setCellValue(cRange.getColumnName());
            columnCount++;
        }
        row++;
        mainRow = curSheet.createRow(row);
        cell=mainRow.createCell(0);cell.setCellValue(customer.getName());
        columnCount=3;
        for(CustomerLicenseRange cRange:customer.getCustomerRangeValues()){
            cell=mainRow.createCell(columnCount);cell.setCellValue(cRange.getEumCount());
            columnCount++;
        }

        
        int colCnt=1;
        for(ApplicationLicenseCount appL:customer.getApplications().values()){
            row++;
            mainRow = curSheet.createRow(row);
            
            cell=mainRow.createCell(colCnt);cell.setCellValue(appL.getApplicationName());
            columnCount=3;
            for(ApplicationLicenseRange appR:appL.getAppLicenseRange()){
                cell=mainRow.createCell(columnCount);cell.setCellValue(appR.getEumCount());
                columnCount++;
            }
            
            for(EUMPageLicenseCount epl:appL.getEumPages()){
                row++;
                mainRow = curSheet.createRow(row);
                cell=mainRow.createCell(2);cell.setCellValue(epl.getPageName());
                columnCount=3;
                for(EUMPageLicenseRange epr:epl.getPageLicenseRange()){
                    cell=mainRow.createCell(columnCount);cell.setCellValue(epr.getValue());
                    columnCount++;
                }
            }
        }
        
        
    }
    
    public String getDescription(NodeLicenseCount node){
        StringBuilder bud = new StringBuilder();
        if(node.getNode().isAppAgentPresent()){
            bud.append(node.getNode().getAppAgentVersion());
            if(node.getNode().isMachineAgentPresent()) bud.append(" || ");
        }
        if(node.getNode().isMachineAgentPresent()) bud.append(node.getNode().getMachineAgentVersion());
        return bud.toString();
    }
}
