package com.ReminderEmailNPAP.filenet;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.ReminderEmailNPAP.util.bean.UserInfoVO;
import com.ReminderEmailNPAP.util.common.ConstantsUtil;
import com.ReminderEmailNPAP.util.common.DAOUtil;
import com.ReminderEmailNPAP.util.common.WorkItemDetails;
import com.filenet.api.constants.DatabaseType;
import com.filenet.api.core.Connection;
import com.filenet.api.util.Id;
import com.filenet.api.util.UserContext;
import com.filenet.api.core.Domain;

import filenet.vw.api.VWCreateLiveWOResult;
import filenet.vw.api.VWException;
import filenet.vw.api.VWFetchType;
import filenet.vw.api.VWLog;
import filenet.vw.api.VWLogElement;
import filenet.vw.api.VWLogQuery;
import filenet.vw.api.VWLoggingOptionType;
import filenet.vw.api.VWQueue;
import filenet.vw.api.VWQueueElement;
import filenet.vw.api.VWQueueQuery;
import filenet.vw.api.VWRoster;
import filenet.vw.api.VWRosterQuery;
import filenet.vw.api.VWSession;
import filenet.vw.api.VWStepElement;
import filenet.vw.api.VWWorkObject;
import filenet.vw.api.VWWorkObjectNumber;
import filenet.vw.base.util.Base64;

public class PEUtil {
	public  final Logger logger = Logger.getLogger(CEUtil.class);
	private CEUtil objCEUtil;
	private VWSession objVWSession;
	private Logger      m_logger = null;
	
	private void getPESession() throws Exception{
        objCEUtil = new CEUtil();
        UserInfoVO loginInfo = CEUtil.getUserInfoVO();
		//CEUtil.getDomain(loginInfo);
		CEUtil.getDomain(loginInfo);
        try {
               objVWSession = new VWSession();
               //objVWSession.setBootstrapCEURI("http://192.168.1.234:9080/wsi/FNCEWS40MTOM/");
               objVWSession.setBootstrapCEURI(ConstantsUtil.CE_CONNECTION_URI);//"http://192.168.1.234:9080/wsi/FNCEWS40MTOM/");
               objVWSession.logon(ConstantsUtil.CONNECTION_POINT);//"CMTOSCP");//"CMTOSCP");
               System.out.println("Got PE Session");
               //VWRoster objRoster = objVWSession.getRoster("EWMS_ACTUARIAL");
               //true - no system queues.
               /*String[] queues = objVWSession.getQueueNames(true);
               for(String queue: queues){
                     System.out.println(queue);
               }*/
        } catch (Exception e) {
               // TODO: handle exception
        }
	}
	private void getPESessionlocal() throws Exception{
		//UserInfoVO loginInfo = CEUtil.getUserInfoVO();
		//objCEUtil.getCEConnection(loginInfo);
		//CEUtil.getDomain(loginInfo);
		try {
			String userName = "p8admin";
			String password = "Hello123";
			// Connection Point
			String connectionPoint = "CMTOSCP";
			// Create a Process Engine Session Object
			objVWSession  = new VWSession();
			// Set Bootstrap Content Engine URI
			objVWSession.setBootstrapCEURI("http://192.168.1.234:9080/wsi/FNCEWS40MTOM/");
			// Log onto the Process Engine Server
			objVWSession.logon(userName, password, connectionPoint);
			System.out.println("Got PE Session");
		}
		catch (Exception e) {
			System.out.println("Exception in PE connection" + e.getMessage());
		}
	}
	
	public HashMap<String, List<WorkItemDetails>> fetchQueueDetails(String queueName ) throws Exception
	{
		boolean retval= false;
		HashMap<String, List<WorkItemDetails>> WIDetailsMap = null;
		try {
			System.out.println("QueueName::" + queueName);
			if(objVWSession==null)
			{
				getPESessionlocal();
			}
			VWQueue queue = objVWSession.getQueue(queueName);
			HashMap<String, WorkItemDetails> retMap=null;
			System.out.println("Queue Depth: " + queue.fetchCount());
			WIDetailsMap= displayQueueElements(queue);			
		}
		catch (VWException vwe) {
			System.out.println("\nVWException Key: " + vwe.getKey() + "\n");
			System.out.println("VWException Cause Class Name: "
			+ vwe.getCauseClassName() + "\n");
			System.out.println("VWException CauseDescription: "
			+ vwe.getCauseDescription()+ "\n");
			System.out.println("VWException Message: " + vwe.getMessage());

			}
		catch (Exception e) {
			//logger.error("Unable to Launch email reminder workflow : " + workflow, e);
			retval = false;
		}
		finally
		{
			 //UserContext.get().popSubject();
			 //objVWSession.logoff();
		}
		// Retrieve the Queue to be searched and Queue depth
		
		return WIDetailsMap;
	}
	 public HashMap<String,List< WorkItemDetails>> displayQueueElements(VWQueue vwQueue)
	    {
	    VWQueueQuery    qQuery = null;
	    VWQueueElement  vwQueueElement = null;
	    HashMap<String,List< WorkItemDetails>> retWIDetails = new HashMap<String, List<WorkItemDetails>>();
	    try
	    {
	        // do we have a valid VWQueue object?
	        if (vwQueue == null)
	        {
	            //m_logger.log("The queue object is null!");
	            return null;
	        }

	        // Set the maximum number of items to be retrieved in each server fetch transaction.
	        // In this case, setting the value to 25 will require less memory for each fetch
	        // than the default setting (50).
	        vwQueue.setBufferSize(25);

	        // construct a queue query object and query for all elements.
	        qQuery = vwQueue.createQuery(null, null, null, 0, null, null, VWFetchType.FETCH_TYPE_QUEUE_ELEMENT);

	        // fetch the first queue element using the VWQueueQuery object
	        vwQueueElement = (VWQueueElement)qQuery.next();

	        List< WorkItemDetails> lst =  new ArrayList<WorkItemDetails>();
	        
	        // check to see if there are any queue elements
	        if (vwQueueElement == null)
	        {
	            //m_logger.log("\t Queue elements: none");
	        }
	        else
	        {
	        	// iterate through the queue elements
	            do
	            {
	            	WorkItemDetails dtl =null;
	                // display the queue element information
	            	dtl = displayQueueElementInfo(vwQueueElement);
	            	lst.add(dtl);
	               
	            }
	            while ((vwQueueElement = (VWQueueElement)qQuery.next()) != null);
	            retWIDetails.put(vwQueue.getName(), lst);
	        }
	    }
	    catch (Exception ex)
	    {
	    	ex.printStackTrace();
	    }
	    return retWIDetails;
    }
	 public WorkItemDetails displayQueueElementInfo(VWQueueElement vwQueueElement)
	    {
	    String[]    fieldNames = null;
	    Object      value = null;
	    WorkItemDetails wiDetails = new WorkItemDetails();
	    try
	    {
	        // do we have a queue element?
	        if (vwQueueElement == null)
	        {
	            //m_logger.log("\t The queue element is null!");
	            return null;
	        }
	        // display the System fields
	        fieldNames = vwQueueElement.getSystemDefinedFieldNames();
	        // iterate through each field
            for (int i = 0; i < fieldNames.length; i++)
            {	                
            	value = vwQueueElement.getFieldValue(fieldNames[i]);
            	if(fieldNames[i].toString().equalsIgnoreCase("F_WobNum"))
            		wiDetails.setWobNum(value);
        		if(fieldNames[i].toString().equalsIgnoreCase("F_StepName"))
        			wiDetails.setStepName(value);
        		if(fieldNames[i].toString().equalsIgnoreCase("F_EnqueueTime"))
        			wiDetails.setEnqueuTime(value);     
        		if(fieldNames[i].toString().equalsIgnoreCase("F_CaseFolder"))
        			wiDetails.setCaseFolder(value);     
        		System.out.println("SYSTEM FIELD NAME::"+fieldNames[i].toString() +"::FIELD VALUE::" +value);
            }
            fieldNames = vwQueueElement.getUserDefinedFieldNames();
	        	            // display the field names and their values
            for (int i = 0; i < fieldNames.length; i++)
            {
            	value = vwQueueElement.getFieldValue(fieldNames[i]);
            	if(fieldNames[i].toString().equalsIgnoreCase("ACT_SubmissionID"))
        			wiDetails.setSubmissionID(value);
        		if(fieldNames[i].toString().equalsIgnoreCase("ACT_LineofBusiness"))
        			wiDetails.setLOB(value);
        		if(fieldNames[i].toString().equalsIgnoreCase("ACT_PASubmissionDate"))
        			wiDetails.setPASubmissionDate(value);
        		if(fieldNames[i].toString().equalsIgnoreCase("ACT_TargetLaunchDate"))
        			wiDetails.setTargetLaunchDate(value);
        		if(fieldNames[i].toString().equalsIgnoreCase("ACT_CaseStatus"))
        			wiDetails.setCaseStatus(value);
        		if(fieldNames[i].toString().equalsIgnoreCase("ACT_SubmissionName"))
        			wiDetails.setSubmissionName(value);  
        		if(fieldNames[i].toString().equalsIgnoreCase("ACT_CreatorName"))
        			wiDetails.setCreatorName(value);    
        		if(fieldNames[i].toString().equalsIgnoreCase("ACT_TargetCompletionDate"))
         			wiDetails.setTargetCompletionDate(value);    
        		if(fieldNames[i].toString().equalsIgnoreCase("ACT_MarkingEntity"))
         			wiDetails.setMarkingEntity(value);   
        		if(fieldNames[i].toString().equalsIgnoreCase("ACT_GPMACValidityPeriod"))
        			wiDetails.setGPMACValidityPeriod(value);
        		if(fieldNames[i].toString().equalsIgnoreCase("ACT_GCEOApprovalDate"))
        			wiDetails.setGCEOApprovalDate(value);
        		
        		System.out.println("USER DEFINED FIELD NAME::"+fieldNames[i].toString() +"::FIELD VALUE::" +value);
            }        
	    }
	    catch (Exception ex)
	    {
	       System.out.println("Exception: " +ex.getMessage());	       
	    }
	    return wiDetails;
	}
	public boolean createWorkflow(String[] toEmail,String[] ccEmail) throws Exception 
	{
		boolean retVal = false;
		VWRoster objRoster = null;
		String sRoster = null;
		VWRosterQuery query = null;
		VWWorkObject vwWorkObj = null;
		UserInfoVO loginInfo = null; 
		try {
			getEmailParam();
			if(objVWSession==null)
			{
				getPESessionlocal();
			}
			/*sRoster = "EWMSGRM";
			System.out.println("Roster: " + sRoster);
			VWRoster roster = objVWSession.getRoster(sRoster);
			//getVWWorkObject(objVWSession,sRoster);
			String workflowName = "APT_ProductSubmissionRequest";
			// Retrieve transfered work classes
			String[] workClassNames = objVWSession.fetchWorkClassNames(true);
			for (int i=0;i<workClassNames.length;i++)
				System.out.println(workClassNames[i] );*/
			 loginInfo = CEUtil.getUserInfoVO();
			HashMap<String, Object> fieldValueMap = new HashMap<String, Object>();
			DAOUtil dao = new DAOUtil();
			String[] emailTemplates = dao.retrieveEmailTemplates("NPAP_PA_3_8_7");
			String process = "ACT";
			String subProcess = "PA";
			String entity = "";
			String department ="";
			String[] to =toEmail;
			String[] cc = ccEmail;
			String[] propertiesWithValue = {
					"#TARGETCOMPLETIONDARE4DAYS#~22/05/2017",
					"#SUBMISSIONID#~1234567890",
					"#SUBMISSIONNAME#~Test from Workflow",
					"#LINEOFBUSINESS#~GEL-NPAP",
					"#TARGETLAUNCHDATE#~27/05/2017",
					"#PRODUCTDETAILS#~eWMSNPAP-GEL"};
			String mailNotificationTo = "NPAP_PA_3_8_7";
			
			fieldValueMap.put("process", process);
			fieldValueMap.put("subProcess", subProcess);
			fieldValueMap.put("entity", "");
			fieldValueMap.put("department", "");
			fieldValueMap.put("to", to);
			fieldValueMap.put("cc", cc);
			fieldValueMap.put("propertiesWithValue", propertiesWithValue);
			fieldValueMap.put("mailNotificationTo",mailNotificationTo);
			
			String str = launchWorkflow(loginInfo, "SendEmailWorkflow", fieldValueMap);
			/*objRoster = objVWSession.getRoster(sRoster);
			System.out.println("Got the Roster from PESession: " + sRoster);
			
			System.out.println("Workflow Count: " + objRoster.fetchCount());
			logger.debug("Got the Roster from PESession: " + sRoster);
			// Workflow name to launch
			String workflowName = "APT_ProductSubmissionRequest";//workflow;
			// Retrieve transfered work classes
			String[] workClassNames = objVWSession.fetchWorkClassNames(true);
			for (int i=0;i<workClassNames.length;i++)
			{
				System.out.println(workClassNames[i] );
			}
			logger.info("Total records for Work ITem: "+workClassNames.length);*/
			// Launch Workflow
			//VWStepElement stepElement = objVWSession.createWorkflow(workflowName);
			// Get and Set Workflow parameters for the Launch Step
			//stepElement.setParameterValue(prameterName, prameterValue,isArray);
			// Dispatch Worflow Launch Step
			//stepElement.doDispatch();
			retVal = true;
		}
		catch (VWException vwe) {
			System.out.println("\nVWException Key: " + vwe.getKey() + "\n");
			System.out.println("VWException Cause Class Name: "
			+ vwe.getCauseClassName() + "\n");
			System.out.println("VWException CauseDescription: "
			+ vwe.getCauseDescription()+ "\n");
			System.out.println("VWException Message: " + vwe.getMessage());

			}
		catch (Exception e) {
			logger.error("Unable to Launch email reminder workflow : " +  e);
			retVal = false;
		}
		finally
		{
			 UserContext.get().popSubject();
			 objVWSession.logoff();
		}
		return retVal;		
	}
	private HashMap<String, String> getEmailParam() throws Exception
	{
		HashMap<String, String> retVal = new HashMap<String, String>();
		
		return null;
	}
	public  String launchWorkflow(UserInfoVO userInfoVO,String workFlowName,HashMap fieldValueMap) throws Exception
    {               
        Set fieldSet=fieldValueMap.keySet();
        Iterator fieldIterator=fieldSet.iterator();
        int fieldCount=0;
        
        while(fieldIterator.hasNext())
        {
            String fieldName=(String)fieldIterator.next();
            if(fieldName==null ||fieldValueMap.get(fieldName)==null)
            {
                            logger.info("Field Value Null for"+fieldName);
                            continue;
            }
            
            fieldCount++;
                        
        }
        String[] launchFields=new String[fieldCount];
        Object[] launchValues=new Object[fieldCount];
        fieldCount=0;
        Iterator fieldIterator1=fieldSet.iterator();
        while(fieldIterator1.hasNext())
        {
                
            String fieldName=(String)fieldIterator1.next();
            if(fieldName==null ||fieldValueMap.get(fieldName)==null)
            {
                            logger.info("Field Value Null for"+fieldName);
                            continue;
            }
            launchFields[fieldCount]=fieldName;
            launchValues[fieldCount]=fieldValueMap.get(fieldName);
            fieldCount++;
                        
        }
        return launchWorkflow(userInfoVO,workFlowName,launchFields,launchValues);
                    
    }
    
    public  String launchWorkflow(UserInfoVO userInfoVO,String workFlowName,String[] launchFields, Object[] launchValues) throws Exception
    {
                    
        String wobNumber = null;
        try
        {   logger.info(launchFields);
           logger.info(launchValues);
           logger.info("About to get PE Session");
           if(objVWSession==null)
			{
				getPESessionlocal();
			}
           //VWSession vwSession= userInfoVO.getPeSession();
        
           //if(peSession.checkWorkflowIdentifier(workflowName)) {
           VWCreateLiveWOResult[] createWORes = objVWSession.createLiveWorkObject(launchFields, launchValues,workFlowName, 1);
           if (createWORes[0].success()) 
           {
             wobNumber = createWORes[0].getWorkObjectNumber();
           }
                              
        }
        catch(Exception ex)
        {
                        logger.error("Unable to launch workflow", ex);
                        throw ex;
        }
        logger.info("Wob Number is: "+wobNumber);
        return wobNumber;

    }

	
	public VWWorkObject getVWWorkObject(VWSession peSession,String rosterName) throws Exception{
	      VWWorkObject vwwObj=null;
	      try{
	     
	     
	          VWRoster roster=peSession.getRoster(rosterName);
	          int queryFlags=VWRoster.QUERY_NO_OPTIONS;
	          String abc = "abc";
	          String filter="Property_Name ="+abc;

	          //Change filter based on requirement

	       
	          VWRosterQuery query=roster.createQuery(null, null, null, queryFlags, filter, null, VWFetchType.FETCH_TYPE_WORKOBJECT);
	          System.out.println("Total records for Work ITem: "+query.fetchCount());
	         
	          while(query.hasNext()){
	              vwwObj=(VWWorkObject) query.next(); 
	          }
	         
	      }catch(VWException vwe){
	    	  System.out.println("Exception found at PEManager.getVWWorkObject():" + vwe.getMessage());
	    	  
	      }catch(Exception vwe){
	    	  System.out.println("Exception found at PEManager.getVWWorkObject():"  + vwe.getMessage());
	      }
	     
	      return vwwObj;
	  }
 	//HashMap<String-KEY1, HashMap<String-KEY2, Object>>  - how many events associated with Case Folder ID
	//String-KEY - FolderID
	//String-KEY2 - Event Name
	//Object - Event Valiue
	public HashMap<String, HashMap<String, Object>> retrieveEvents(String folderID,boolean crtieria) throws Exception 
	 {
	      HashMap<String,HashMap<String, Object>> EventFolderMap =null;
		 try {
       		logger.info("retrieveEvents for Folder ID ::" + folderID);
              if(objVWSession==null)
                    getPESession();
              
              VWLog objLog =null;
              if(crtieria)
            	  objLog = objVWSession.fetchEventLog("GEMS_SRRCase");
              else
            	  objLog = objVWSession.fetchEventLog("GEMS_AHRCase");
              
              EventFolderMap = new HashMap<String, HashMap<String,Object>>();
              // Set Query Parameters
              String finalValue = ReverseGuid(folderID); 
              String queryFilter = "F_CaseFolder = " +finalValue+ " AND F_Text like '%Terminate%'"; // user logged in event type
              
              int queryFlags = VWLog.QUERY_NO_OPTIONS;
              // Perform Query
              VWLogQuery query = objLog.startQuery(null,null,null,queryFlags,queryFilter,null);
              // Process Results
              int count = query.fetchCount();
              logger.info("total query records for Folder ID ::" + folderID + " are:: "+count);
              HashMap<String, Object> EventIndividualMap ;//= new HashMap<String, Object>();
              
              int totalEvent=0;
              while(query.hasNext())
              {
	               VWLogElement logElement = (VWLogElement) query.next();
	               int eventType = logElement.getEventType();
	               String stringEventType = VWLoggingOptionType.getLocalizedString(eventType);
	               String[] fieldNames = logElement.getFieldNames();
	               EventIndividualMap = new HashMap<String, Object>();
	               for (int i = 0; i <= fieldNames.length - 1; i++) {           
	                   //Do your stuff here
	            	   String FieldValue ="";
	            	   if(fieldNames[i] !=null )
	            	   {
	            		   if(logElement.getFieldValue(fieldNames[i])!=null )
	            		   {
	            			   FieldValue = logElement.getFieldValue(fieldNames[i]).toString();
	            			   EventIndividualMap.put(fieldNames[i], FieldValue);
	            		   }
	            		   //logger.info("Filed Name is::" + fieldNames[i] + " and Field value is :: " +FieldValue );
	            	   }
	               }
	               EventFolderMap.put(folderID+totalEvent, EventIndividualMap);
	               //logger.info("KEY for HASMAP::"+folderID+totalEvent);
	               totalEvent++;
              }  
              
              
       } catch (Exception e) {
    	   logger.error("PE Exception::" +e.getMessage());
              throw new Exception("Unable to retrieve Event details from PE: ", e);
       }
		
       return EventFolderMap;
 	}

	 private String ReverseGuid(String folderID) {
			// TODO Auto-generated method stub
			String temp = folderID.replace("{", "");
			temp = temp.replace("}", "");
			
			String[] section = temp.split("-");
			String finalStr = "x'" +reverse(section[0].toCharArray()) + 
					reverse(section[1].toCharArray())+
					reverse(section[2].toCharArray())+
					section[3]+section[4]+"'";
			//System.out.print(finalStr);
			
			return finalStr;
		}
		private String reverse(char[] str)
		{
			String retStr="";
			for (int i=str.length-1;i>=0;i--)
			{
				if(i%2==0)
				{
					retStr=retStr+str[i]+str[i+1];
				}
			}
			return retStr;
		}

}
