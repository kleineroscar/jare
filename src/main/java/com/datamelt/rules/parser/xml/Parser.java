/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.datamelt.rules.parser.xml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import com.datamelt.rules.core.ActionObject;
import com.datamelt.rules.core.Parameter;
import com.datamelt.rules.core.ReferenceField;
import com.datamelt.rules.core.RuleCollection;
import com.datamelt.rules.core.RuleGroup;
import com.datamelt.rules.core.RuleMessage;
import com.datamelt.rules.core.RuleObject;
import com.datamelt.rules.core.RuleSubGroup;
import com.datamelt.rules.core.XmlRule;
import com.datamelt.rules.core.XmlAction;
import com.datamelt.rules.core.util.VariableReplacer;

/**
 * this parser is used to parse rule definition files. rule files have to be
 * written using the syntax as documented. groups, subgroups and rules are
 * collected from the file.
 * 
 * after the xml rule definition file has been parsed, an arraylist of groups
 * is available, containing the subgroups and rules.
 * 
 * @author uwe geercken
 */
public class Parser extends DefaultHandler implements ContentHandler
{
    private ArrayList<RuleGroup>groups = new ArrayList<RuleGroup>();
    private ArrayList<ReferenceField>referenceFields = new ArrayList<ReferenceField>();
    
    private VariableReplacer replacer;
    
    private RuleGroup group;
    private RuleSubGroup subgroup;
    private XmlRule xmlRule;
    private XmlAction action;
    private ActionObject actionObject;
    
    private boolean groupTagActive;
    private boolean subgroupTagActive;
    private boolean ruleTagActive;
    private boolean executeTagActive;
    private boolean actionTagActive;
    private boolean objectTagActive;
    
    private static final String TAG_GROUP                 				= "group";
    private static final String TAG_GROUP_ID              				= "id";
    private static final String TAG_GROUP_DESCRIPTION     				= "description";
    private static final String TAG_GROUP_OUTPUT_AFTER_ACTIONS 			= "outputafteractions";
    private static final String TAG_GROUP_VALID_FROM	  				= "validfrom";
    private static final String TAG_GROUP_VALID_UNTIL	  				= "validuntil";
    private static final String TAG_GROUP_DEPENDENT_GROUP_ID  			= "dependentgroupid";
    private static final String TAG_GROUP_DEPENDENT_GROUP_EXECUTE_IF 	= "dependentgroupexecuteif";
    private static final String TAG_SUBGROUP	          				= "subgroup";
    private static final String TAG_SUBGROUP_ID          				= "id";
    private static final String TAG_SUBGROUP_DESCRIPTION  				= "description";
    private static final String TAG_INTERGROUP_OPERATOR   				= "intergroupoperator";
    private static final String TAG_SUBGROUP_RULEOPERATOR 				= "ruleoperator";
    private static final String TAG_RULE                  				= "rule";
    private static final String TAG_RULE_ID               				= "id";
    private static final String TAG_RULE_DESCRIPTION      				= "description";
    private static final String TAG_OBJECT                				= "object";
    private static final String TAG_OBJECT_CLASSNAME      				= "classname";
    private static final String TAG_OBJECT_METHOD         				= "method";
	private static final String TAG_OBJECT_TYPE           				= "type";
    private static final String TAG_OBJECT_TYPE2          				= "returntype";
    private static final String TAG_OBJECT_PARAMETER      				= "parameter";
    private static final String TAG_OBJECT_PARAMETER_TYPE 				= "parametertype";
    private static final String TAG_OBJECT_PARAMETER_IS_SETTER_VALUE 	= "settervalue";
    private static final String TAG_EXPECTED              				= "expected";
    private static final String TAG_EXPECTED_VALUE        				= "value";
    private static final String TAG_EXPECTED_TYPE         				= "type";
    private static final String TAG_EXECUTE               				= "execute";
    private static final String TAG_EXECUTE_VALUE         				= "value";
    private static final String TAG_PARAMETER             				= "parameter";
    private static final String TAG_PARAMETER_VALUE       				= "value";
    private static final String TAG_PARAMETER_TYPE        				= "type";
    private static final String TAG_MESSAGE               				= "message";
    private static final String TAG_MESSAGE_TYPE		  				= "type";
    private static final String TAG_MESSAGE_TEXT		  				= "text";
    private static final String TAG_ACTION                				= "action";
    private static final String TAG_ACTION_ID             				= "id";
    private static final String TAG_ACTION_DESCRIPTION    				= "description";
    private static final String TAG_ACTION_CLASSNAME      				= "classname";
    private static final String TAG_ACTION_METHOD		  				= "method";
    private static final String TAG_ACTION_METHOD_TYPE	  				= "type";
    private static final String TAG_ACTION_METHOD_RETURNTYPE 			= "returntype";
    private static final String TAG_ACTION_PARAMETER      				= "parameter";
    private static final String TAG_ACTION_PARAMETERTYPE  				= "type";
    private static final String TAG_ACTION_PARAMETERVALUE 				= "value";
    private static final String TAG_ACTION_EXECUTEIF	  				= "executeif";
    
    private static final String TAG_REFERENCE_FIELD		  				= "field";
    private static final String TAG_REFERENCE_FIELD_NAME  				= "name";
    private static final String TAG_REFERENCE_FIELD_NAME_DESCRIPTIVE	= "namedescriptive";
    private static final String TAG_REFERENCE_FIELD_DESCRIPTION			= "description";
    private static final String TAG_REFERENCE_FIELD_JAVA_TYPE_ID		= "javatypeid";
    
    private static final String TAG_TYPE_TRUE   = "true";
        
    private static final String TAG_TYPE_FAILED = "failed";
    private static final String TAG_TYPE_PASSED = "passed";
    private static final String TAG_TYPE_ALWAYS = "always";
    
    private static final String TAG_TYPE_SETTER = "setter";
    
	/**
	 * constructor for a parser object that is used to parse a rule definition
	 * xml file. pass a VariableReplacer object to this constructor, which
	 * will be used to replace variables in the xml rule file with actual values.
	 * 
	 * @param	replacer	the replacer object
	 */
    public Parser(VariableReplacer replacer)
    {
        this.replacer = replacer;
    }
    
	/**
	 * pass an database connection and a projectid to this method, which will in turn
	 * be parsed.
	 * 
	 * Because the database contains all projects we have to define a specific
	 * project we want to use
	 *
	 * @param db        the database connection to use for parsing
	 * @param projectId the projectid to use for parsing
	 * @throws Exception exception during parsing
	 */
	public void parse(Connection db, int projectId) throws Exception {
		// First we have to get some general information about the project
		PreparedStatement groupPS = db.prepareStatement(
				"SELECT id, project_id, name, description, valid_from, valid_until, dependent_rulegroup_id, dependent_rulegroup_execute_if, disabled FROM rulegroup WHERE project_id = ?");
		PreparedStatement subGroupPS = db.prepareStatement(
				"SELECT rulesubgroup.id, rulegroup_id, rulesubgroup.name, rulesubgroup.description, intergroupoperator, ruleoperator FROM `rulesubgroup` WHERE rulegroup_id = ?");
		PreparedStatement rulePS = db.prepareStatement("SELECT " + "r.id, " + "r.rulesubgroup_id, " + "r.check_id, "
				+ "r.last_update_user_id, " + "r.name, " + "r.description, " + "r.object1_parametertype_id, "
				+ "r.object1_parameter, " + "r.object1_type_id, " + "r.object2_parametertype_id, "
				+ "r.object2_parameter, " + "r.object2_type_id, " + "r.expectedvalue, " + "r.expectedvalue_type_id, "
				+ "r.additional_parameter, " + "r.additional_parameter_type_id, " + "r.message_passed, "
				+ "r.message_failed, " + "c.package AS check_package, " + "c.class as check_class, "
				+ "c.check_single_field AS check_single_field " + "FROM rule r, `check` c " + "WHERE r.check_id = c.id "
				+ "AND r.rulesubgroup_id = ?");
		PreparedStatement actionPS = db.prepareStatement(
				"SELECT rulegroupaction.id, rulegroup_id, rulegroupaction.name, rulegroupaction.description, object1_parametertype_id, object1_parameter, object1_type_id, object2_parametertype_id, object2_parameter, object2_type_id, object3_parametertype_id, object3_parameter, object3_type_id , parameter1, parameter1_type_id, parameter2, parameter2_type_id, parameter3, parameter3_type_id, execute_if, `action`.description AS action_description, `action`.classname AS action_classname, `action`.methodname AS action_methodname, `action`.methoddisplayname AS action_methoddisplayname FROM `rulegroupaction` INNER JOIN `action` ON `rulegroupaction`.action_id = `action`.id WHERE rulegroup_id = ?");
		PreparedStatement referenceFieldPS = db.prepareStatement(
				"SELECT id, project_id, name, name_descriptive, description, java_type_id FROM `reference_fields` WHERE project_id = ?");

		// Create types dictionary to make conversions
		PreparedStatement parameterPS = db.prepareStatement("SELECT id, name FROM types");
		ResultSet parameterRS = parameterPS.executeQuery();
		Map<Integer, String> types = new HashMap<>();
		while (parameterRS.next()) {
			types.put(parameterRS.getInt("id"), parameterRS.getString("name"));
		}
		parameterPS.close();

		// Create project dictionary
		PreparedStatement projectPS = db.prepareStatement(
				"SELECT name, description, object_classname, object_method_getter, object_method_setter FROM project");
		ResultSet projectRS = projectPS.executeQuery();
		Map<String, String> project = new HashMap<>();
		while (projectRS.next()) {
			project.put("name", projectRS.getString("name"));
			project.put("description", projectRS.getString("description"));
			project.put("object_classname", projectRS.getString("object_classname"));
			project.put("object_method_getter", projectRS.getString("object_method_getter"));
			project.put("object_method_setter", projectRS.getString("object_method_setter"));
		}
		projectPS.close();

		// Get all groups which belong to the project
		groupPS.setInt(1, projectId);
		ResultSet groupRS = groupPS.executeQuery();
		while (groupRS.next()) {
			// Enrich group object with information in table
			group = new RuleGroup(groupRS.getString("name"), groupRS.getNString("description"));
			group.setValidFrom(groupRS.getString("valid_from"));
			group.setValidUntil(groupRS.getString("valid_until"));
			group.setDependentRuleGroupId(groupRS.getString("dependent_rulegroup_id"));
			group.setDependentRuleGroupExecuteIf(groupRS.getInt("dependent_rulegroup_execute_if"));
			subGroupPS.setInt(1, Integer.parseInt(groupRS.getString("id")));

			// Get all subgroups belonging to this group
			ResultSet subGroupRS = subGroupPS.executeQuery();
			while (subGroupRS.next()) {
				subgroup = new RuleSubGroup(subGroupRS.getString("name"), subGroupRS.getString("description"),
						subGroupRS.getString("intergroupoperator"), subGroupRS.getString("ruleoperator"));

				// Get all rules belonging to this subgroup
				rulePS.setInt(1, Integer.parseInt(subGroupRS.getString("id")));
				ResultSet ruleRS = rulePS.executeQuery();
				// Because there could be more than one rule it is stored in a collection
				RuleCollection ruleCol = new RuleCollection();
				while (ruleRS.next()) {
					// "A RuleObject a RuleObject identifies an object that will be instantiated and
					// one of its methods will be run"
					// most of the time this is "getFieldValue", as in read the value from a column
					// some rules get value from multiple column, so multiple objects are possible
					RuleObject ruleObject = new RuleObject(project.get("object_classname"),
							project.get("object_method_getter"), types.get(ruleRS.getInt("object1_type_id")),
							ruleRS.getString("object1_parameter"),
							types.get(Integer.parseInt(ruleRS.getString("object1_parametertype_id"))));
					xmlRule = new XmlRule(ruleRS.getString("name"), ruleRS.getString("description"));
					xmlRule.getRuleObjects().add(ruleObject);
					// Only add the other RuleObject if its present
					if (ruleRS.getObject("object2_parameter") != null) {
						RuleObject ruleObject2 = new RuleObject(project.get("object_classname"),
								project.get("object_method_getter"), types.get(ruleRS.getInt("object2_type_id")),
								ruleRS.getString("object2_parameter"),
								types.get(Integer.parseInt(ruleRS.getString("object2_parametertype_id"))));
						xmlRule.getRuleObjects().add(ruleObject2);
					}
					// Additional parameters are optional
					if (ruleRS.getObject("additional_parameter") != null) {
						xmlRule.addParameter(new Parameter(
								types.get(Integer.parseInt(ruleRS.getString("additional_parameter_type_id"))),
								ruleRS.getString("additional_parameter")));
					}
					// Enrich the xmlRule object with the information available
					xmlRule.setCheckToExecute(
							ruleRS.getString("check_package") + "." + ruleRS.getString("check_class"));
					if (ruleRS.getObject("expectedvalue") != null) {
						xmlRule.setExpectedValueRule(ruleRS.getString("expectedvalue"));
						xmlRule.setExpectedValueRuleType(
								types.get(Integer.parseInt(ruleRS.getString("expectedvalue_type_id"))));
					}
					xmlRule.getMessages()
							.add(new RuleMessage(RuleMessage.TYPE_FAILED, ruleRS.getString("message_passed")));
					xmlRule.getMessages()
							.add(new RuleMessage(RuleMessage.TYPE_PASSED, ruleRS.getString("message_failed")));
					ruleCol.add(xmlRule);
				}
				subgroup.setRulesCollection(ruleCol);
				group.getSubGroupCollection().add(subgroup);
			}
			// Get all actions associated to the group
			actionPS.setInt(1, Integer.parseInt(groupRS.getString("id")));
			ResultSet actionRS = actionPS.executeQuery();
			while (actionRS.next()) {
				action = new XmlAction(actionRS.getString("name"), actionRS.getString("description"));
				if ("passed".equals(actionRS.getString("execute_if"))) {
					action.setExecuteIf(XmlAction.TYPE_PASSED);
				} else if ("failed".equals(actionRS.getString("execute_if"))) {
					action.setExecuteIf(XmlAction.TYPE_FAILED);
				} else {
					action.setExecuteIf(XmlAction.TYPE_ALWAYS);
				}
				action.setClassName(actionRS.getString("action_classname"));
				action.setMethodName(actionRS.getString("action_methodname"));

				// Object2 is the default rulegroup action object, just in case every object is
				// treated as optional
				if (actionRS.getObject("object1_parameter") != null) {
					ActionObject actionObject1 = new ActionObject(project.get("object_classname"),
							project.get("object_method_getter"));
					actionObject1.setIsGetter(ActionObject.METHOD_GETTER);
					actionObject1.setReturnType(types.get(Integer.parseInt(actionRS.getString("object1_type_id"))));
					Parameter parameter1 = new Parameter(
							types.get(Integer.parseInt(actionRS.getString("object1_parametertype_id"))),
							actionRS.getString("object1_parameter"));
					actionObject1.addParameter(parameter1);
					action.getActionGetterObjects().add(actionObject1);
				}
				if (actionRS.getObject("object2_parameter") != null) {
					ActionObject actionObject2 = new ActionObject(project.get("object_classname"),
							project.get("object_method_setter"));
					actionObject2.setIsGetter(ActionObject.METHOD_SETTER);
					actionObject2.setReturnType(types.get(Integer.parseInt(actionRS.getString("object2_type_id"))));
					Parameter parameter1 = new Parameter(
							types.get(Integer.parseInt(actionRS.getString("object2_parametertype_id"))),
							actionRS.getString("object2_parameter"));
					Parameter parameter2 = new Parameter(
							types.get(Integer.parseInt(actionRS.getString("object2_type_id"))), "true", true);
					actionObject2.addParameter(parameter1);
					actionObject2.addParameter(parameter2);
					action.setActionSetterObject(actionObject2);
				}
				if (actionRS.getObject("object3_parameter") != null) {
					ActionObject actionObject3 = new ActionObject(project.get("object_classname"),
							project.get("object_method_getter"));
					actionObject3.setIsGetter(ActionObject.METHOD_GETTER);
					actionObject3.setReturnType(types.get(Integer.parseInt(actionRS.getString("object3_type_id"))));
					Parameter parameter1 = new Parameter(
							types.get(Integer.parseInt(actionRS.getString("object3_parametertype_id"))),
							actionRS.getString("object3_parameter"));
					actionObject3.addParameter(parameter1);
					action.getActionGetterObjects().add(actionObject3);
				}

				// TODO: add Mapping implementation
//                action.setMappingCollection();
//                action.getMappingCollection()

				// TODO: Not cascading, because not sure if people can create parameter2 while
				// not creating parameter1
				// therefore treated as optional
				if (actionRS.getObject("parameter1") != null) {
					Parameter parameter1 = new Parameter(types.get(actionRS.getInt("parameter1_type_id")),
							actionRS.getString("parameter1"));
					action.addParameter(parameter1);
				}
				if (actionRS.getObject("parameter2") != null) {
					Parameter parameter2 = new Parameter(types.get(actionRS.getInt("parameter2_type_id")),
							actionRS.getString("parameter2"));
					action.addParameter(parameter2);
				}
				if (actionRS.getObject("parameter3") != null) {
					Parameter parameter3 = new Parameter(types.get(actionRS.getInt("parameter3_type_id")),
							actionRS.getString("parameter3"));
					action.addParameter(parameter3);
				}
				group.getActions().add(action);
			}
			if (group.isValid()) {
				groups.add(group);
			}
		}
		// Close the db connections, as they are no longer necessary
		rulePS.close();
		actionPS.close();
		subGroupPS.close();
		groupPS.close();

		// We also need the reference fields that are used in the rules
		referenceFieldPS.setInt(1, projectId);
		ResultSet referenceFieldRS = referenceFieldPS.executeQuery();
		while (referenceFieldRS.next()) {
			ReferenceField tempRF = new ReferenceField();
			tempRF.setName(referenceFieldRS.getString("name"));
			tempRF.setNameDescriptive(referenceFieldRS.getString("name_descriptive"));
			tempRF.setDescription(referenceFieldRS.getString("description"));
			tempRF.setJavaTypeId(Integer.parseInt(referenceFieldRS.getString("java_type_id")));
			referenceFields.add(tempRF);
		}
		referenceFieldPS.close();

	}
    
    /**
     * pass a filename of an xml rule definition file to this method, which will
     * in turn be parsed using SAX. 
     * 
     * @param	filename	the xml file to parse
     * @throws	Exception	exception if the xml file cannot be located or parsed
     */
    public void parse(String filename) throws Exception
    {
		File f = new File(filename);
		if(!f.exists())
		{
			throw new FileNotFoundException("file not found: " + filename);
		}
        try
        {
        	SAXParserFactory factory = SAXParserFactory.newInstance();
        	SAXParser saxParser = factory.newSAXParser();
        	saxParser.parse(filename,this);
        }
        catch(FileNotFoundException ex)
        {
        	throw new FileNotFoundException("error: " + filename + " not found");
        }
        catch(SAXParseException ex)
        {
        	throw new SAXException("error parsing xml file: " + filename + " line: " + ex.getLineNumber());
        }
    }
    
    /**
     * pass an inputstream of a xml file to this method, which will
     * in turn be parsed using SAX.
     * 
     *  @param	stream		the input stream to use for parsing
     *  @throws	Exception	exception during parsing the xml file
     */
    public void parse(InputStream stream) throws Exception
    {
    	try
    	{
    		SAXParserFactory factory = SAXParserFactory.newInstance();
    		SAXParser saxParser = factory.newSAXParser();
    		saxParser.parse(new org.xml.sax.InputSource(stream),this);
    	}
    	catch(SAXParseException ex)
        {
        	throw new SAXException("error parsing xml inputstream line: " + ex.getLineNumber());
        }
    }
    
    public void startDocument() throws SAXException
    {
    }
    
    /**
     * 
     * 
     */
    public void startElement( String namespaceURI, String localName, String qName, Attributes atts ) throws SAXException
	  {
		// new rule starts here
        if(qName.equals(TAG_GROUP)&& !groupTagActive)
        {
            groupTagActive=true;
            // group description may be empty
            String groupDescription = "";
            if(atts.getValue(TAG_GROUP_DESCRIPTION)!=null)
            {
            	groupDescription = atts.getValue(TAG_GROUP_DESCRIPTION);
            }
            group = new RuleGroup(atts.getValue(TAG_GROUP_ID),groupDescription);
            if(atts.getValue(TAG_GROUP_OUTPUT_AFTER_ACTIONS)!=null && atts.getValue(TAG_GROUP_OUTPUT_AFTER_ACTIONS).equals(TAG_TYPE_TRUE))
            {
            	group.setOutputAfterActions(Boolean.TRUE);
            }
            if(atts.getValue(TAG_GROUP_VALID_FROM)!=null)
            {
            	group.setValidFrom(atts.getValue(TAG_GROUP_VALID_FROM));
            }
            if(atts.getValue(TAG_GROUP_VALID_UNTIL)!=null)
            {
            	group.setValidUntil(atts.getValue(TAG_GROUP_VALID_UNTIL));
            }
            if(atts.getValue(TAG_GROUP_DEPENDENT_GROUP_ID)!=null)
            {
           		group.setDependentRuleGroupId(atts.getValue(TAG_GROUP_DEPENDENT_GROUP_ID));
            }
            if(atts.getValue(TAG_GROUP_DEPENDENT_GROUP_EXECUTE_IF)!=null)
            {
            	if(atts.getValue(TAG_GROUP_DEPENDENT_GROUP_EXECUTE_IF).equals(TAG_TYPE_PASSED))
                {
            		group.setDependentRuleGroupExecuteIf(0);
                }
            	else
                {
            		group.setDependentRuleGroupExecuteIf(1);
                }
            }
        }
        if(qName.equals(TAG_OBJECT) && groupTagActive && actionTagActive)
        {
        	objectTagActive=true;
        	actionObject = new ActionObject(atts.getValue(TAG_OBJECT_CLASSNAME),atts.getValue(TAG_OBJECT_METHOD));
        	actionObject.setReturnType(atts.getValue(TAG_ACTION_METHOD_RETURNTYPE));
        	if(atts.getValue(TAG_ACTION_METHOD_TYPE)!=null)
        	{
        		if(atts.getValue(TAG_ACTION_METHOD_TYPE).equals(Parser.TAG_TYPE_SETTER))
        		{
        			actionObject.setIsGetter(ActionObject.METHOD_SETTER);
        			action.setActionSetterObject(actionObject);
        		}
        		else
        		{
        			actionObject.setIsGetter(ActionObject.METHOD_GETTER);
        			action.addActionGetterObject(actionObject);
        		}
        	}
        	else
        	{
        		actionObject.setIsGetter(ActionObject.METHOD_GETTER);
        		action.addActionGetterObject(actionObject);
        	}
        }
        if(qName.equals(TAG_PARAMETER) && objectTagActive && actionTagActive)
        {
        	Parameter parameter = new Parameter(atts.getValue(TAG_ACTION_PARAMETERTYPE),atts.getValue(TAG_ACTION_PARAMETERVALUE));
            if(atts.getValue(TAG_OBJECT_PARAMETER_IS_SETTER_VALUE)!=null)
            {
            	if(atts.getValue(TAG_OBJECT_PARAMETER_IS_SETTER_VALUE).equals(TAG_TYPE_TRUE))
            	{
            		parameter.setSetterValue(true);
            	}
            }
        	actionObject.addParameter(parameter);
        }
        if(qName.equals(TAG_ACTION) && groupTagActive &&! objectTagActive &&!subgroupTagActive && !ruleTagActive)
        {
        	actionTagActive=true;
        	// action description may be empty
            String actionDescription = "";
            if(atts.getValue(TAG_ACTION_DESCRIPTION)!=null)
            {
            	actionDescription = atts.getValue(TAG_ACTION_DESCRIPTION);
            }
            // action id may be empty
            String actionId = "";
            if(atts.getValue(TAG_ACTION_ID)!=null)
            {
            	actionId = atts.getValue(TAG_ACTION_ID);
            }
        	if(atts.getValue(TAG_ACTION_EXECUTEIF).equals(TAG_TYPE_FAILED))
            {
        		action = new XmlAction(actionId,actionDescription);
        		action.setClassName(atts.getValue(TAG_ACTION_CLASSNAME));
        		action.setMethodName(atts.getValue(TAG_ACTION_METHOD));
        		action.setExecuteIf(1);
            }
        	else if(atts.getValue(TAG_ACTION_EXECUTEIF).equals(TAG_TYPE_PASSED))
        	{
        		action = new XmlAction(actionId,actionDescription);
        		action.setClassName(atts.getValue(TAG_ACTION_CLASSNAME));
        		action.setMethodName(atts.getValue(TAG_ACTION_METHOD));
        		action.setExecuteIf(0);
        	}
        	else if(atts.getValue(TAG_ACTION_EXECUTEIF).equals(TAG_TYPE_ALWAYS))
        	{
        		action = new XmlAction(actionId,actionDescription);
        		action.setClassName(atts.getValue(TAG_ACTION_CLASSNAME));
        		action.setMethodName(atts.getValue(TAG_ACTION_METHOD));
        		action.setExecuteIf(2);
        	}
        }
        if(qName.equals(TAG_PARAMETER) && actionTagActive && groupTagActive &&! objectTagActive &&!subgroupTagActive && !ruleTagActive)
        {
        	action.addParameter(new Parameter(atts.getValue(TAG_ACTION_PARAMETERTYPE),atts.getValue(TAG_ACTION_PARAMETERVALUE)));
        }
        if(qName.equals(TAG_SUBGROUP) && groupTagActive && !subgroupTagActive)
        {
            subgroupTagActive=true;
            // subgroup description may be empty
            String subgroupDescription = "";
            if(atts.getValue(TAG_SUBGROUP_DESCRIPTION)!=null)
            {
            	subgroupDescription = atts.getValue(TAG_SUBGROUP_DESCRIPTION);
            }
            // if intergroup operator is not specified, it is AND per default
            String intergroupOperator = RuleSubGroup.OPERATOR_AND_EXPRESSION;
            if(atts.getValue(TAG_INTERGROUP_OPERATOR)!=null)
            {
            	intergroupOperator = atts.getValue(TAG_INTERGROUP_OPERATOR);
            }
            // if rule operator is not specified it is AND per default
            String ruleOperator = RuleSubGroup.OPERATOR_AND_EXPRESSION;
            if(atts.getValue(TAG_SUBGROUP_RULEOPERATOR) !=null)
            {
            	ruleOperator = atts.getValue(TAG_SUBGROUP_RULEOPERATOR);
            }
            subgroup = new RuleSubGroup(atts.getValue(TAG_SUBGROUP_ID),subgroupDescription,intergroupOperator,ruleOperator);
        }
        if(qName.equals(TAG_RULE) && subgroupTagActive && !ruleTagActive)
        {
            ruleTagActive=true;
            // rule description may be empty
            String ruleDescription = "";
            if(atts.getValue(TAG_RULE_DESCRIPTION) !=null)
            {
            	ruleDescription = atts.getValue(TAG_RULE_DESCRIPTION);
            }
            xmlRule = new XmlRule(atts.getValue(TAG_RULE_ID),ruleDescription);
        }
        
        if(ruleTagActive && qName.equals(TAG_OBJECT))
        {
            String parameterValue= atts.getValue(TAG_OBJECT_PARAMETER);
            if (replacer!=null && replacer.getNumberOfVariables()>0)
            {
                try
                {
                    parameterValue = replacer.getValue(parameterValue);
                }
                catch(Exception ex)
                {
                    throw new SAXException(ex.getMessage());
                }
            }
            // the following lines are just for the better understanding of the xml syntax
            // the TAG_OBJECT_TYPE = type is not clear enough; it is really the return type 
            // of the method
            String methodReturnType = null;
            if(atts.getValue(TAG_OBJECT_TYPE)!=null)
            {
            	methodReturnType = atts.getValue(TAG_OBJECT_TYPE);
            }
            else
            {
            	methodReturnType = atts.getValue(TAG_OBJECT_TYPE2);
            }
            xmlRule.getRuleObjects().add(new RuleObject(atts.getValue(TAG_OBJECT_CLASSNAME),atts.getValue(TAG_OBJECT_METHOD),methodReturnType,parameterValue,atts.getValue(TAG_OBJECT_PARAMETER_TYPE)));
        }
        else if(ruleTagActive && qName.equals(TAG_EXPECTED))
        {
            // replacer object replaces variables with real
            // values based on a properties file
            try
            {
                String expectedRuleValue = atts.getValue(TAG_EXPECTED_VALUE);
                if (replacer!=null && replacer.getNumberOfVariables()>0)
                {
                    expectedRuleValue = replacer.getValue(expectedRuleValue);
                }
                xmlRule.setExpectedValueRule(expectedRuleValue);
                xmlRule.setExpectedValueRuleType(atts.getValue(TAG_EXPECTED_TYPE));
            }
            catch(Exception ex)
            {
                throw new SAXException(ex.getMessage());
            }
        }
        else if(ruleTagActive && qName.equals(TAG_EXECUTE))
        {
            executeTagActive=true;
            xmlRule.setCheckToExecute(atts.getValue(TAG_EXECUTE_VALUE));
        }
        else if(ruleTagActive && qName.equals(TAG_MESSAGE))
        {
            if(atts.getValue(TAG_MESSAGE_TYPE).equals(TAG_TYPE_FAILED))
            {
                xmlRule.getMessages().add(new RuleMessage(RuleMessage.TYPE_FAILED, atts.getValue(TAG_MESSAGE_TEXT)));
            }
            else if(atts.getValue(TAG_MESSAGE_TYPE).equals(TAG_TYPE_PASSED))
            {
                xmlRule.getMessages().add(new RuleMessage(RuleMessage.TYPE_PASSED, atts.getValue(TAG_MESSAGE_TEXT)));
            }
        }
        else if(ruleTagActive && qName.equals(TAG_ACTION))
        {
        	int type;
        	if(atts.getValue(TAG_ACTION_EXECUTEIF)==TAG_TYPE_FAILED)
        	{
        		type=XmlAction.TYPE_FAILED;
        	}
        	else if(atts.getValue(TAG_ACTION_EXECUTEIF)==TAG_TYPE_PASSED)
        	{
        		type=XmlAction.TYPE_PASSED;
        	}
        	else
        	{
        		type=XmlAction.TYPE_ALWAYS;
        	}
        	// action description may be empty
            String actionDescription = "";
            if(atts.getValue(TAG_ACTION_DESCRIPTION)!=null)
            {
            	actionDescription = atts.getValue(TAG_ACTION_DESCRIPTION);
            }
            // action id may be empty
            String actionId = "";
            if(atts.getValue(TAG_ACTION_ID)!=null)
            {
            	actionId = atts.getValue(TAG_ACTION_ID);
            }
            XmlAction action= new XmlAction(actionId,actionDescription);
        	action.setClassName(atts.getValue(TAG_ACTION_CLASSNAME));
        	action.setMethodName(atts.getValue(TAG_ACTION_METHOD));
        	action.setExecuteIf(type);
        	action.addParameter(new Parameter(atts.getValue(TAG_ACTION_PARAMETERTYPE),atts.getValue(TAG_ACTION_PARAMETER)));
        	action.setDescription(actionDescription);
        	action.setId(actionId);
        	xmlRule.getActions().add(action);
        }
        if(executeTagActive && qName.equals(TAG_PARAMETER))
        {
            String parameterValue= atts.getValue(TAG_PARAMETER_VALUE);
            if (replacer!=null && replacer.getNumberOfVariables()>0)
            {
                try
                {
                    parameterValue = replacer.getValue(parameterValue);
                }
                catch(Exception ex)
                {
                    throw new SAXException(ex.getMessage());
                }
            }
            Parameter para = new Parameter(atts.getValue(TAG_PARAMETER_TYPE),parameterValue);
            xmlRule.addParameter(para);
        }
        
        if(qName.equals(TAG_REFERENCE_FIELD))
        {
            ReferenceField field = new ReferenceField();
        	if(atts.getValue(TAG_REFERENCE_FIELD_NAME) !=null)
            {
            	field.setName(atts.getValue(TAG_REFERENCE_FIELD_NAME));
            }
        	if(atts.getValue(TAG_REFERENCE_FIELD_NAME_DESCRIPTIVE) !=null)
            {
            	field.setNameDescriptive(atts.getValue(TAG_REFERENCE_FIELD_NAME_DESCRIPTIVE));
            }
        	if(atts.getValue(TAG_REFERENCE_FIELD_DESCRIPTION) !=null)
            {
            	field.setDescription(atts.getValue(TAG_REFERENCE_FIELD_DESCRIPTION));
            }
        	if(atts.getValue(TAG_REFERENCE_FIELD_JAVA_TYPE_ID) !=null)
            {
            	field.setJavaTypeId(Integer.parseInt(atts.getValue(TAG_REFERENCE_FIELD_JAVA_TYPE_ID)));
            }
        	referenceFields.add(field);
        }
    }

    public void endElement( String namespaceURI, String localName, String qName )
    {
        if(qName.equals(TAG_RULE))
        {
            ruleTagActive=false;
            subgroup.getRulesCollection().add(xmlRule);
        }
        else if(qName.equals(TAG_EXECUTE))
        {
            executeTagActive=false;
        }
        else if(qName.equals(TAG_SUBGROUP))
        {
            subgroupTagActive=false;
            group.getSubGroupCollection().add(subgroup);
        }
        else if(qName.equals(TAG_ACTION))
        {
            actionTagActive=false;
        	group.addAction(action);
        }
        else if(qName.equals(TAG_OBJECT))
        {
            objectTagActive=false;
        }
        else if(qName.equals(TAG_GROUP))
        {
            groupTagActive=false;
            
            try
            {
            	// only add the group if it is valid on the current date
            	// and if the valid from and valid until dates can be parsed correctly
            	if(group.isValid())
            	{
                	groups.add(group);
                }
            }
            catch(Exception ex)
            {
            	ex.printStackTrace();
            }
            
        }
    }

    public void endDocument()
    {
    }
    
    /**
     * returns an arraylist of groups that have been constructed by parsing
     * a xml rule definition file 
     * 
     * @return	list of rulegroups
     */
    public ArrayList<RuleGroup> getGroups()
    {
        return groups;
    }
    
    /**
     * returns an arraylist of fields that have been constructed by parsing
     * a xml rule definition file 
     * 
     * @return	list of fields
     */
    public ArrayList<ReferenceField> getReferenceFields()
    {
        return referenceFields;
    }
}
