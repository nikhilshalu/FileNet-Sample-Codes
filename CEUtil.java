package com.ReminderEmailNPAP.filenet;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.text.SimpleDateFormat;

import javax.activation.MimetypesFileTypeMap;
import javax.naming.ldap.LdapName;
import javax.security.auth.Subject;

import org.apache.log4j.Logger;

import com.ReminderEmailNPAP.util.bean.UserInfoVO;
import com.ReminderEmailNPAP.util.common.ConstantsUtil;
import com.ReminderEmailNPAP.util.common.EWMSProperties;
import com.filenet.api.admin.Choice;
import com.filenet.api.admin.ChoiceList;
import com.filenet.api.collection.AccessPermissionList;
import com.filenet.api.collection.AnnotationSet;
import com.filenet.api.collection.ContentElementList;
import com.filenet.api.collection.EventSet;
import com.filenet.api.collection.IndependentObjectSet;
import com.filenet.api.collection.MarkingList;
import com.filenet.api.collection.MarkingSetSet;
import com.filenet.api.collection.RepositoryRowSet;
import com.filenet.api.collection.UserSet;
import com.filenet.api.constants.AutoClassify;
import com.filenet.api.constants.AutoUniqueName;
import com.filenet.api.constants.CheckinType;
import com.filenet.api.constants.DefineSecurityParentage;
import com.filenet.api.constants.RefreshMode;
import com.filenet.api.constants.ReservationType;
import com.filenet.api.core.Annotation;
import com.filenet.api.core.Connection;
import com.filenet.api.core.ContentTransfer;
import com.filenet.api.core.Document;
import com.filenet.api.core.Domain;
import com.filenet.api.core.Factory;
import com.filenet.api.core.Folder;
import com.filenet.api.core.IndependentObject;
import com.filenet.api.core.ObjectStore;
import com.filenet.api.core.ReferentialContainmentRelationship;
import com.filenet.api.core.VersionSeries;
import com.filenet.api.events.Event;
import com.filenet.api.exception.EngineRuntimeException;
import com.filenet.api.property.Properties;
import com.filenet.api.property.Property;
import com.filenet.api.query.SearchSQL;
import com.filenet.api.query.SearchScope;
import com.filenet.api.security.Group;
import com.filenet.api.security.User;
import com.filenet.api.util.Id;
import com.filenet.api.util.UserContext;
import com.filenet.api.security.Group;
import com.filenet.api.security.User;

public class CEUtil {

	public static final Logger logger = Logger.getLogger(CEUtil.class);

	/**
	 * This method is used to fetch the CE Connection based on the information
	 * provided.
	 * BATCH
	 * @param loginVO
	 * @return
	 * @throws Exception
	 * @throws Exception
	 */

	public static UserInfoVO getUserInfoVO() {
		UserInfoVO userInfoVO = new UserInfoVO();
		userInfoVO.setUserName(ConstantsUtil.SERVICE_USER_NAME);
		userInfoVO.setPassword(ConstantsUtil.SERVICE_USER_PASSWORD);
		return userInfoVO;
	}



	public static Connection getCEConnection(UserInfoVO loginVO) throws Exception {
		Connection ceConnection = null;
		ceConnection = loginVO.getCeConnection();
		System.setProperty("java.security.auth.login.config", "");
		if (ceConnection == null) {
			logger.debug("****************CE CONNECTION IS NULL, CREATING ONE**************");
			ceConnection = Factory.Connection.getConnection("http://WIN-0E87GO6ASTA:9080/wsi/FNCEWS40MTOM/");
		} else {
			logger.debug("****************CONNECTION IS NOT NULL****************");
		}

		getSubject(loginVO, ceConnection);
		return ceConnection;
	}

	/**
	 * This method is used to fetch the Domain.
	 * 
	 * @param loginVO
	 * @return
	 * @throws Exception
	 */
	public static Domain getDomain(UserInfoVO loginVO) throws Exception {

		logger.debug("===================getting filenet Domain====================");
		Domain domain = Factory.Domain.fetchInstance(getCEConnection(loginVO), null, null);
		logger.debug("domain:::" + domain.get_Name());
		return domain;
	}

	/**
	 * This method is used to fetch the objectstore as specified by the
	 * objStoreName property
	 * 
	 * @param userInfoVO
	 * @param objStoreName
	 * @return
	 * @throws Exception
	 */
	public static ObjectStore getObjectStore(UserInfoVO userInfoVO, String objStoreName) throws Exception {
		return Factory.ObjectStore.fetchInstance(getDomain(userInfoVO), objStoreName, null);
	}

	/**
	 * This method returns the authenticated subject used for creating session.
	 * 
	 * @param loginVO
	 * @param ceConnection
	 * @return
	 * @throws EngineRuntimeException
	 */
	public static Subject getSubject(UserInfoVO loginVO, Connection ceConnection) throws EngineRuntimeException {
		Subject userSubject = null;

		userSubject = UserContext.createSubject(ceConnection, loginVO.getUserName(), loginVO.getPassword(),
				"FileNetP8");
		UserContext.get().pushSubject(userSubject);
		// SUBJECT*****************"+userSubject);
		return userSubject;
	}

	/**
	 * This method searches the Object Store based on the query string.
	 * 
	 * @param objStore
	 * @param select
	 * @param className
	 * @param whereClause
	 * @param alias
	 * @return
	 */
	

	public static IndependentObjectSet CESearch(com.filenet.api.core.ObjectStore objStore, String select,
			String className, String whereClause, String alias, String orderByClause) {
		SearchSQL sqlObject = new SearchSQL();
		sqlObject.setSelectList(select);
		boolean subclassesToo = true;
		logger.info("CESearch:::" + className);
		sqlObject.setFromClauseInitialValue(className, alias, subclassesToo);
		sqlObject.setWhereClause(whereClause);
		if (orderByClause != null)
			sqlObject.setOrderByClause(orderByClause);

		logger.info("sql query::::" + sqlObject.toString());
		SearchScope searchScope = new SearchScope(objStore);
		IndependentObjectSet independentObjectSetObj = searchScope.fetchObjects(sqlObject, null, null,
				new Boolean(true));
		return independentObjectSetObj;
	}

	public static IndependentObjectSet NPAPSearch(UserInfoVO userInfoVO) throws Exception {
		SearchSQL searchsql = new SearchSQL();
		
		String whereClause = ConstantsUtil.WHERE_NPAP_CLAUSE;

		StringBuffer query = new StringBuffer(
				ConstantsUtil.SELECT_NPAP_QUERY + ConstantsUtil.SPACE + whereClause);
		logger.info("= query is:: " + query.toString());
		searchsql.setQueryString(query.toString());
		SearchScope searchscope = new SearchScope(getObjectStore(userInfoVO));
		IndependentObjectSet independentObjectSet = searchscope.fetchObjects(searchsql, null, null, new Boolean(true));
		return independentObjectSet;
	}
	
	public static Folder getCaseFolder(String wherePropertyName, String wherePropertyValue, String className) {
		String selectClause = "searchProp.*";
		String whereClause = " " + wherePropertyName + "='" + wherePropertyValue + "'";
		logger.info("WhereClause::" + whereClause);
		String alias = "searchProp";
		//String propertyValue = "";
		Folder folder = null;
		try {
			IndependentObjectSet independentSetObj = CESearch(
					getObjectStore(CEUtil.getUserInfoVO(), "CMTOS"), selectClause, className, whereClause,
					alias, null);
			if (!independentSetObj.isEmpty()) {
				Iterator itrObj = independentSetObj.iterator();
				while (itrObj.hasNext()) {
					folder = (Folder) itrObj.next();
					break;
				}
			}
		} catch (Exception e) {
			logger.error("Error getting fOLDER::", e);
		}

		return folder;
	}
	public IndependentObjectSet Search(UserInfoVO userInfoVO) throws Exception {
		SearchSQL searchsql = new SearchSQL();
		
		String whereClause = "WHERE ([GRM_SystemStatus] = 'Open') OPTIONS(TIMELIMIT 180)";

		StringBuffer query = new StringBuffer(
				"SELECT *  FROM GRM_APT WITH INCLUDESUBCLASSES" + " "+ whereClause);
		logger.info("= query is:: " + query.toString());
		searchsql.setQueryString(query.toString());
		SearchScope searchscope = new SearchScope(getObjectStore(userInfoVO));
		IndependentObjectSet independentObjectSet = searchscope.fetchObjects(searchsql, null, null, new Boolean(true));
		return independentObjectSet;
	}
	private static ObjectStore getObjectStore(UserInfoVO userInfoVO) throws Exception {
		return CEUtil.getObjectStore(userInfoVO, "CMTOS");
	}
	@SuppressWarnings("unchecked")
	public String retrieveEmailIdsFromGroups(String[] arrAddresses) throws Exception {
		logger.debug("retrieveEmailIdsFromGroups started here...");
		Group objGroup = null;
		UserSet objUserSet = null; 
		User objUser = null;
		Iterator<User> itrUser = null;
		String sUserEmailId = null;
		String lstEmailIDs = null;
		boolean isGroup = false;
		String sDistinguishedName = null;
		String strUserCN = null;
		try {
			
			UserInfoVO objUserInfoVO = getUserInfoVO();
			
			Connection objConnection = getCEConnection(objUserInfoVO);
			for(String sGroupName : arrAddresses){
				logger.info("Retrieving the users for the group: " + sGroupName);
				try {
					objGroup = Factory.Group.fetchInstance(objConnection, sGroupName, null);
					isGroup = true;
				} catch (EngineRuntimeException e) {
					isGroup = false;
					if(e.getMessage().contains("The requested item was not found.")){
						objUser = Factory.User.fetchInstance(objConnection, sGroupName, null);
					}
				}
				
				if(isGroup){
					logger.info("Group instance retrieved...");
					objUserSet = objGroup.get_Users();
					itrUser = objUserSet.iterator();
					while(itrUser.hasNext()) {				
						objUser =  itrUser.next();
						sDistinguishedName = objUser.get_DistinguishedName();
						sDistinguishedName = objUser.get_DistinguishedName();
						logger.info("get_DistinguishedName: " + sDistinguishedName);
						LdapName ln = new LdapName(sDistinguishedName);
						Enumeration<String> str1 = ln.getAll();
						//System.out.println(str1);
						while (str1.hasMoreElements()) {
							strUserCN = (String) str1.nextElement();
						}
						logger.info("First CN has value: " + strUserCN);
						sUserEmailId = strUserCN.substring(3);	
						lstEmailIDs = (lstEmailIDs==null)?sUserEmailId:lstEmailIDs.concat(",").concat(sUserEmailId);
						logger.info(sUserEmailId + " added to the list.");
					}
					objUser = null;
				} else {
					logger.info("User instance retrieved...");
					sDistinguishedName = objUser.get_DistinguishedName();
					logger.info("get_DistinguishedName: " + sDistinguishedName);
					LdapName ln = new LdapName(sDistinguishedName);
					Enumeration<String> str1 = ln.getAll();
					//System.out.println(str1);
					while (str1.hasMoreElements()) {
						strUserCN = (String) str1.nextElement();
					}
					logger.info("First CN has value: " + strUserCN);
					sUserEmailId = strUserCN.substring(3);					
					lstEmailIDs = (lstEmailIDs==null)?sUserEmailId:lstEmailIDs.concat(",").concat(sUserEmailId);
					logger.info(sUserEmailId + " added to the list.");
				}
			}
			logger.debug("list EmailIDs: " + lstEmailIDs);
		} catch (Exception e) {
			throw new Exception("Unable to retrieve the EmailID from the groups.", e);
		} 
		logger.info("lstEmailIDs: " + lstEmailIDs);
		logger.debug("retrieveEmailIdsFromGroups ended here...");
		return lstEmailIDs;
	}
	

	
}
