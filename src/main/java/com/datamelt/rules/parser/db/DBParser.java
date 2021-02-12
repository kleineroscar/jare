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
package com.datamelt.rules.parser.db;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.datamelt.rules.core.*;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import com.datamelt.rules.core.util.VariableReplacer;
import com.datamelt.rules.parser.xml.Parser;

/**
 * this parser is used to parse a database created by the business rule maintenance tool.
 * It parses all rules corresponding to a given project name.
 * <p>
 * after the data base connection has been parsed, an arraylist of groups is
 * available, containing the subgroups and rules.
 *
 * @author oscar heimbrecht
 */
public class DBParser extends DefaultHandler implements ContentHandler {
	private ArrayList<RuleGroup> groups = new ArrayList<RuleGroup>();
	private ArrayList<ReferenceField> referenceFields = new ArrayList<ReferenceField>();

	private VariableReplacer replacer;

	private RuleGroup group;
	private RuleSubGroup subgroup;
	private XmlRule xmlRule;
	private XmlAction action;
	private ActionObject actionObject;

	private static final String COLUMN_PROJECT_ID = "id";
	private static final String COLUMN_PROJECT_NAME = "name";
	private static final String COLUMN_PROJECT_DESCRIPTION = "description";
	private static final String COLUMN_PROJECT_OBJECT_CLASSNAME = "object_classname";
	private static final String COLUMN_PROJECT_OBJECT_METHOD_GETTER = "object_method_getter";
	private static final String COLUMN_PROJECT_OBJECT_METHOD_SETTER = "object_method_setter";

	private static final String COLUMN_GROUP_ID = "id";
	private static final String COLUMN_GROUP_PROJECT_ID = "project_id";
	private static final String COLUMN_GROUP_NAME = "name";
	private static final String COLUMN_GROUP_DESCRIPTION = "description";
	private static final String COLUMN_GROUP_VALID_FROM = "valid_from";
	private static final String COLUMN_GROUP_VALID_UNTIL = "valid_until";
	private static final String COLUMN_GROUP_DEPENDENT_RULEGROUP_ID = "dependent_rulegroup_id";
	private static final String COLUMN_GROUP_DEPENDENT_RULEGROUP_EXECUTE_IF = "dependent_rulegroup_execute_if";
	private static final String COLUMN_GROUP_DISABLED = "disabled";

	private static final String COLUMN_SUBGROUP_ID = "id";
	private static final String COLUMN_SUBGROUP_RULEGROUP_ID = "rulegroup_id";
	private static final String COLUMN_SUBGROUP_NAME = "name";
	private static final String COLUMN_SUBGROUP_DESCRIPTION = "description";
	private static final String COLUMN_SUBGROUP_INTERGROUPOPERATOR = "intergroupoperator";
	private static final String COLUMN_SUBGROUP_RULEOPERATOR = "ruleoperator";

	private static final String COLUMN_RULE_ID = "id";
	private static final String COLUMN_RULE_RULESUBGROUP_ID = "rulesubgroup_id";
	private static final String COLUMN_RULE_CHECK_ID = "check_id";
	private static final String COLUMN_RULE_NAME = "name";
	private static final String COLUMN_RULE_DESCRIPTION = "description";
	private static final String COLUMN_RULE_OBJECT1_PARAMETERTYPE_ID = "object1_parametertype_id";
	private static final String COLUMN_RULE_OBJECT1_PARAMETER = "object1_parameter";
	private static final String COLUMN_RULE_OBJECT1_TYPE_ID = "object1_type_id";
	private static final String COLUMN_RULE_OBJECT2_PARAMETERTYPE_ID = "object2_parametertype_id";
	private static final String COLUMN_RULE_OBJECT2_PARAMETER = "object2_parameter";
	private static final String COLUMN_RULE_OBJECT2_TYPE_ID = "object2_type_id";
	private static final String COLUMN_RULE_EXPECTEDVALUE = "expectedvalue";
	private static final String COLUMN_RULE_EXPECTEDVALUE_TYPE_ID = "expectedvalue_type_id";
	private static final String COLUMN_RULE_ADDITIONAL_PARAMETER = "additional_parameter";
	private static final String COLUMN_RULE_ADDITIONAL_PARAMETER_TYPE_ID = "additional_parameter_type_id";
	private static final String COLUMN_RULE_MESSAGE_PASSED = "message_passed";
	private static final String COLUMN_RULE_MESSAGE_FAILED = "message_failed";

	private static final String COLUMN_RULEGROUPACTION_ID = "id";
	private static final String COLUMN_RULEGROUPACTION_RULEGROUP_ID = "rulegroup_id";
	private static final String COLUMN_RULEGROUPACTION_ACTION_ID = "action_id";
	private static final String COLUMN_RULEGROUPACTION_NAME = "name";
	private static final String COLUMN_RULEGROUPACTION_DESCRIPTION = "description";
	private static final String COLUMN_RULEGROUPACTION_OBJECT1_PARAMETERTYPE_ID = "object1_parametertype_id";
	private static final String COLUMN_RULEGROUPACTION_OBJECT1_PARAMETER = "object1_parameter";
	private static final String COLUMN_RULEGROUPACTION_OBJECT1_TYPE_ID = "object1_type_id";
	private static final String COLUMN_RULEGROUPACTION_OBJECT2_PARAMETERTYPE_ID = "object2_parametertype_id";
	private static final String COLUMN_RULEGROUPACTION_OBJECT2_PARAMETER = "object2_parameter";
	private static final String COLUMN_RULEGROUPACTION_OBJECT2_TYPE_ID = "object2_type_id";
	private static final String COLUMN_RULEGROUPACTION_OBJECT3_PARAMETERTYPE_ID = "object3_parametertype_id";
	private static final String COLUMN_RULEGROUPACTION_OBJECT3_PARAMETER = "object3_parameter";
	private static final String COLUMN_RULEGROUPACTION_OBJECT3_TYPE_ID = "object3_type_id";
	private static final String COLUMN_RULEGROUPACTION_PARAMETER1 = "parameter1";
	private static final String COLUMN_RULEGROUPACTION_PARAMETER1_TYPE_ID = "parameter1_type_id";
	private static final String COLUMN_RULEGROUPACTION_PARAMETER2 = "parameter2";
	private static final String COLUMN_RULEGROUPACTION_PARAMETER2_TYPE_ID = "parameter2_type_id";
	private static final String COLUMN_RULEGROUPACTION_PARAMETER3 = "parameter3";
	private static final String COLUMN_RULEGROUPACTION_PARAMETER3_TYPE_ID = "parameter3_type_id";
	private static final String COLUMN_RULEGROUPACTION_EXECUTE_IF = "execute_if";

	private static final String COLUMN_REFERENCE_FIELDS_ID = "id";
	private static final String COLUMN_REFERENCE_FIELDS_PROJECT_ID = "project_id";
	private static final String COLUMN_REFERENCE_FIELDS_NAME = "name";
	private static final String COLUMN_REFERENCE_FIELDS_NAME_DESCRIPTTIVE = "name_descriptive";
	private static final String COLUMN_REFERENCE_FIELDS_DESCRIPTION = "description";
	private static final String COLUMN_REFERENCE_FIELDS_JAVA_TYPE_ID = "java_type_id";

	private static final String COLUMN_TYPES_ID = "id";
	private static final String COLUMN_TYPES_NAME = "name";

	private static final String COLUMN_ACTION_ID = "id";
	private static final String COLUMN_ACTION_DESCRIPTION = "description";
	private static final String COLUMN_ACTION_CLASSNAME = "classname";
	private static final String COLUMN_ACTION_METHODNAME = "methodname";

	private static final String COLUMN_CHECK_ID = "id";
	private static final String COLUMN_CHECK_NAME = "name";
	private static final String COLUMN_CHECK_DESCRIPTION = "description";
	private static final String COLUMN_CHECK_NAME_DESCRIPTIVE = "name_descriptive";
	private static final String COLUMN_CHECK_PACKAGE = "package";
	private static final String COLUMN_CHECK_CLASS = "class";
	private static final String COLUMN_CHECK_CHECK_SINGLE_FIELD = "check_single_field";
	
	private static final String TABLE_PROJECT = "`project`";
	private static final String TABLE_GROUP = "`rulegroup`";
	private static final String TABLE_SUBGROUP = "`rulesubgroup`";
	private static final String TABLE_RULE = "`rule`";
	private static final String TABLE_RULEGROUPACTION = "`rulegroupaction`";
	private static final String TABLE_REFERENCE_FIELDS = "`reference_fields`";
	private static final String TABLE_TYPES = "`types`";
	private static final String TABLE_ACTION = "`action`";
	private static final String TABLE_CHECK = "`check`";

	private static final String TYPE_TRUE = "true";

	private static final String TYPE_FAILED = "failed";
	private static final String TYPE_PASSED = "passed";
	private static final String TYPE_ALWAYS = "always";

	private static final String TYPE_SETTER = "setter";

	private static final String groupSQL = "SELECT * "
			+ "FROM " + TABLE_GROUP + " g," + TABLE_PROJECT + " p "
			+ "WHERE g." + COLUMN_GROUP_PROJECT_ID + " = p." + COLUMN_PROJECT_ID + " "
			+ "AND p." + COLUMN_PROJECT_NAME + " = ?;";
	private static final String subgroupSQL = "SELECT * "
			+ "FROM " + TABLE_SUBGROUP + " sg "
			+ "WHERE sg." + COLUMN_SUBGROUP_RULEGROUP_ID + " = ?;";
	private static final String ruleSQL = "SELECT * "
			+ "FROM " + TABLE_RULE + " r, " + TABLE_CHECK + " c "
			+ "WHERE r." + COLUMN_RULE_CHECK_ID + " = c." + COLUMN_CHECK_ID + " "
			+ "AND r." + COLUMN_RULE_RULESUBGROUP_ID + " = ?;";
	private static final String actionSQL = "SELECT * "
			+ "FROM " + TABLE_RULEGROUPACTION + " rga, " + TABLE_ACTION + " a "
			+ "WHERE rga. " + COLUMN_RULEGROUPACTION_ACTION_ID + " = a." + COLUMN_ACTION_ID + " "
			+ "AND rga." + COLUMN_RULEGROUPACTION_RULEGROUP_ID + " = ?;";
	private static final String referenceFieldsSQL = "SELECT * "
			+ "FROM " + TABLE_REFERENCE_FIELDS + " rf, " + TABLE_PROJECT + " p "
			+ "WHERE rf." + COLUMN_REFERENCE_FIELDS_PROJECT_ID + " = p.id AND p." + COLUMN_PROJECT_NAME + " = ?;";
	private static final String typesSQL = "SELECT * FROM " + TABLE_TYPES + " t;";
	private static final String projectSQL = "SELECT * FROM " + TABLE_PROJECT + " p;";

	/**
	 * constructor for a parser object that is used to parse a rule definition xml
	 * file. pass a VariableReplacer object to this constructor, which will be used
	 * to replace variables in the xml rule file with actual values.
	 *
	 * @param replacer the replacer object
	 */
	public DBParser(VariableReplacer replacer) {
		this.replacer = replacer;
	}

	/**
	 * pass an mysql connection and a projectid to this method, which will in turn
	 * be parsed.
	 * 
	 * Because the database contains all projects we have to define a specific
	 * project we want to use
	 *
	 * @param db        the database connection to use for parsing
	 * @param projectId the projectid to use for parsing
	 * @throws Exception exception during parsing
	 */
	public void parse(Connection db, String projectName) throws Exception {
		// First we have to get some general information about the project
		PreparedStatement groupPS = db.prepareStatement(groupSQL);
		PreparedStatement subGroupPS = db.prepareStatement(subgroupSQL);
		PreparedStatement rulePS = db.prepareStatement(ruleSQL);
		PreparedStatement actionPS = db.prepareStatement(actionSQL);
		PreparedStatement referenceFieldPS = db.prepareStatement(referenceFieldsSQL);

		// Create types dictionary to make conversions
		PreparedStatement parameterPS = db.prepareStatement(typesSQL);
		ResultSet parameterRS = parameterPS.executeQuery();
		Map<Integer, String> types = new HashMap<>();
		while (parameterRS.next()) {
			types.put(parameterRS.getInt(COLUMN_TYPES_ID), parameterRS.getString(COLUMN_TYPES_NAME));
		}
		parameterPS.close();

		// Create project dictionary
		PreparedStatement projectPS = db.prepareStatement(projectSQL);
		ResultSet projectRS = projectPS.executeQuery();
		Map<String, String> project = new HashMap<>();
		while (projectRS.next()) {
			project.put(COLUMN_PROJECT_NAME, projectRS.getString(COLUMN_PROJECT_NAME));
			project.put(COLUMN_PROJECT_DESCRIPTION, projectRS.getString(COLUMN_PROJECT_DESCRIPTION));
			project.put(COLUMN_PROJECT_OBJECT_CLASSNAME, projectRS.getString(COLUMN_PROJECT_OBJECT_CLASSNAME));
			project.put(COLUMN_PROJECT_OBJECT_METHOD_GETTER, projectRS.getString(COLUMN_PROJECT_OBJECT_METHOD_GETTER));
			project.put(COLUMN_PROJECT_OBJECT_METHOD_SETTER, projectRS.getString(COLUMN_PROJECT_OBJECT_METHOD_SETTER));
		}
		projectPS.close();

		// Get all groups which belong to the project
		groupPS.setString(1, projectName);
		ResultSet groupRS = groupPS.executeQuery();
		while (groupRS.next()) {
			try {
			// Enrich group object with information in table
			group = new RuleGroup(groupRS.getString(COLUMN_GROUP_NAME), groupRS.getNString(COLUMN_GROUP_DESCRIPTION));
			group.setValidFrom(groupRS.getString(COLUMN_GROUP_VALID_FROM));
			group.setValidUntil(groupRS.getString(COLUMN_GROUP_VALID_UNTIL));
			if (groupRS.getObject(COLUMN_GROUP_DEPENDENT_RULEGROUP_ID) != null && !groupRS.getObject(COLUMN_GROUP_DEPENDENT_RULEGROUP_ID).equals("0")) {
				group.setDependentRuleGroupId(groupRS.getString(COLUMN_GROUP_DEPENDENT_RULEGROUP_ID));
			}
			if (TYPE_PASSED.equals(groupRS.getString(COLUMN_GROUP_DEPENDENT_RULEGROUP_EXECUTE_IF))) {
				group.setDependentRuleGroupExecuteIf(0);
			} else {
				group.setDependentRuleGroupExecuteIf(1);
			}
			subGroupPS.setInt(1, Integer.parseInt(groupRS.getString(COLUMN_GROUP_ID)));
			
			// Get all subgroups belonging to this group
			ResultSet subGroupRS = subGroupPS.executeQuery();
			while (subGroupRS.next()) {
				subgroup = new RuleSubGroup(subGroupRS.getString(COLUMN_SUBGROUP_NAME), subGroupRS.getString(COLUMN_SUBGROUP_DESCRIPTION),
						subGroupRS.getString(COLUMN_SUBGROUP_INTERGROUPOPERATOR), subGroupRS.getString(COLUMN_SUBGROUP_RULEOPERATOR));

				// Get all rules belonging to this subgroup
				rulePS.setInt(1, Integer.parseInt(subGroupRS.getString(COLUMN_SUBGROUP_ID)));
				ResultSet ruleRS = rulePS.executeQuery();
				// Because there could be more than one rule it is stored in a collection
				RuleCollection ruleCol = new RuleCollection();
				while (ruleRS.next()) {
					// "A RuleObject identifies an object that will be instantiated and
					// one of its methods will be run"
					// most of the time this is "getFieldValue", as in read the value from a column
					// some rules get value from multiple column, so multiple objects are possible
					RuleObject ruleObject = new RuleObject(project.get(COLUMN_PROJECT_OBJECT_CLASSNAME),
							project.get(COLUMN_PROJECT_OBJECT_METHOD_GETTER), types.get(ruleRS.getInt(COLUMN_RULE_OBJECT1_TYPE_ID)),
							ruleRS.getString(COLUMN_RULE_OBJECT1_PARAMETER),
							types.get(Integer.parseInt(ruleRS.getString(COLUMN_RULE_OBJECT1_PARAMETERTYPE_ID))));
					xmlRule = new XmlRule(ruleRS.getString(COLUMN_RULE_NAME), ruleRS.getString(COLUMN_RULE_DESCRIPTION));
					xmlRule.getRuleObjects().add(ruleObject);
					// Only add the other RuleObject if its present
					if (ruleRS.getObject(COLUMN_RULE_OBJECT2_PARAMETER) != null) {
						RuleObject ruleObject2 = new RuleObject(project.get(COLUMN_PROJECT_OBJECT_CLASSNAME),
								project.get(COLUMN_PROJECT_OBJECT_METHOD_GETTER), types.get(ruleRS.getInt(COLUMN_RULE_OBJECT2_TYPE_ID)),
								ruleRS.getString(COLUMN_RULE_OBJECT2_PARAMETER),
								types.get(Integer.parseInt(ruleRS.getString(COLUMN_RULE_OBJECT2_PARAMETERTYPE_ID))));
						xmlRule.getRuleObjects().add(ruleObject2);
					}
					// Additional parameters are optional
					if (ruleRS.getObject(COLUMN_RULE_ADDITIONAL_PARAMETER) != null && !ruleRS.getString(COLUMN_RULE_ADDITIONAL_PARAMETER).isEmpty()) {
						xmlRule.addParameter(new Parameter(
								types.get(Integer.parseInt(ruleRS.getString(COLUMN_RULE_ADDITIONAL_PARAMETER_TYPE_ID))),
								ruleRS.getString(COLUMN_RULE_ADDITIONAL_PARAMETER)));
					}
					// Enrich the xmlRule object with the information available
					xmlRule.setCheckToExecute(
							ruleRS.getString(COLUMN_CHECK_PACKAGE) + "." + ruleRS.getString(COLUMN_CHECK_CLASS));
					if (ruleRS.getObject(COLUMN_RULE_EXPECTEDVALUE) != null) {
						xmlRule.setExpectedValueRule(ruleRS.getString(COLUMN_RULE_EXPECTEDVALUE));
						xmlRule.setExpectedValueRuleType(
								types.get(Integer.parseInt(ruleRS.getString(COLUMN_RULE_EXPECTEDVALUE_TYPE_ID))));
					}
					xmlRule.getMessages()
							.add(new RuleMessage(RuleMessage.TYPE_FAILED, ruleRS.getString(COLUMN_RULE_MESSAGE_PASSED)));
					xmlRule.getMessages()
							.add(new RuleMessage(RuleMessage.TYPE_PASSED, ruleRS.getString(COLUMN_RULE_MESSAGE_FAILED)));
					ruleCol.add(xmlRule);
				}
				subgroup.setRulesCollection(ruleCol);
				group.getSubGroupCollection().add(subgroup);
			}
			// Get all actions associated to the group
			actionPS.setInt(1, Integer.parseInt(groupRS.getString(COLUMN_GROUP_ID)));
			ResultSet actionRS = actionPS.executeQuery();
			while (actionRS.next()) {
				action = new XmlAction(actionRS.getString(COLUMN_RULEGROUPACTION_NAME), actionRS.getString(COLUMN_RULEGROUPACTION_DESCRIPTION));
				if ("passed".equals(actionRS.getString(COLUMN_RULEGROUPACTION_EXECUTE_IF))) {
					action.setExecuteIf(XmlAction.TYPE_PASSED);
				} else if ("failed".equals(actionRS.getString(COLUMN_RULEGROUPACTION_EXECUTE_IF))) {
					action.setExecuteIf(XmlAction.TYPE_FAILED);
				} else {
					action.setExecuteIf(XmlAction.TYPE_ALWAYS);
				}
				action.setClassName(actionRS.getString(COLUMN_ACTION_CLASSNAME));
				action.setMethodName(actionRS.getString(COLUMN_ACTION_METHODNAME));

				// Object2 is the default rulegroup action object, just in case every object is
				// treated as optional
				if (actionRS.getObject(COLUMN_RULEGROUPACTION_OBJECT1_PARAMETER) != null && !actionRS.getString(COLUMN_RULEGROUPACTION_OBJECT1_PARAMETER).isEmpty()) {
					ActionObject actionObject1 = new ActionObject(project.get(COLUMN_PROJECT_OBJECT_CLASSNAME),
							project.get(COLUMN_PROJECT_OBJECT_METHOD_GETTER));
					actionObject1.setIsGetter(ActionObject.METHOD_GETTER);
					actionObject1.setReturnType(types.get(Integer.parseInt(actionRS.getString(COLUMN_RULEGROUPACTION_OBJECT1_TYPE_ID))));
					Parameter parameter1 = new Parameter(
							types.get(Integer.parseInt(actionRS.getString(COLUMN_RULEGROUPACTION_OBJECT1_PARAMETERTYPE_ID))),
							actionRS.getString(COLUMN_RULEGROUPACTION_OBJECT1_PARAMETER));
					actionObject1.addParameter(parameter1);
					action.getActionGetterObjects().add(actionObject1);
				}
				if (actionRS.getObject(COLUMN_RULEGROUPACTION_OBJECT2_PARAMETER) != null && !actionRS.getString(COLUMN_RULEGROUPACTION_OBJECT2_PARAMETER).isEmpty()) {
					ActionObject actionObject2 = new ActionObject(project.get(COLUMN_PROJECT_OBJECT_CLASSNAME),
							project.get(COLUMN_PROJECT_OBJECT_METHOD_SETTER));
					actionObject2.setIsGetter(ActionObject.METHOD_SETTER);
					actionObject2.setReturnType(types.get(Integer.parseInt(actionRS.getString(COLUMN_RULEGROUPACTION_OBJECT2_TYPE_ID))));
					Parameter parameter1 = new Parameter(
							types.get(Integer.parseInt(actionRS.getString(COLUMN_RULEGROUPACTION_OBJECT2_PARAMETERTYPE_ID))),
							actionRS.getString(COLUMN_RULEGROUPACTION_OBJECT2_PARAMETER));
					Parameter parameter2 = new Parameter(
							types.get(Integer.parseInt(actionRS.getString(COLUMN_RULEGROUPACTION_OBJECT2_TYPE_ID))), TYPE_TRUE, true);
					actionObject2.addParameter(parameter1);
					actionObject2.addParameter(parameter2);
					action.setActionSetterObject(actionObject2);
				}
				if (actionRS.getObject(COLUMN_RULEGROUPACTION_OBJECT3_PARAMETER) != null && !actionRS.getString(COLUMN_RULEGROUPACTION_OBJECT3_PARAMETER).isEmpty()) {
					ActionObject actionObject3 = new ActionObject(project.get(COLUMN_PROJECT_OBJECT_CLASSNAME),
							project.get(COLUMN_PROJECT_OBJECT_METHOD_GETTER));
					actionObject3.setIsGetter(ActionObject.METHOD_GETTER);
					actionObject3.setReturnType(types.get(Integer.parseInt(actionRS.getString(COLUMN_RULEGROUPACTION_OBJECT3_TYPE_ID))));
					Parameter parameter1 = new Parameter(
							types.get(Integer.parseInt(actionRS.getString(COLUMN_RULEGROUPACTION_OBJECT3_PARAMETERTYPE_ID))),
							actionRS.getString(COLUMN_RULEGROUPACTION_OBJECT3_PARAMETER));
					actionObject3.addParameter(parameter1);
					action.getActionGetterObjects().add(actionObject3);
				}

				// Not cascading, because people can create parameter2 while
				// not creating parameter1
				// therefore treated as optional
				if (actionRS.getObject(COLUMN_RULEGROUPACTION_PARAMETER1) != null && !actionRS.getString(COLUMN_RULEGROUPACTION_PARAMETER1).isEmpty()) {
					Parameter parameter1 = new Parameter(types.get(actionRS.getInt(COLUMN_RULEGROUPACTION_PARAMETER1_TYPE_ID)),
							actionRS.getString(COLUMN_RULEGROUPACTION_PARAMETER1));
					action.addParameter(parameter1);
				}
				if (actionRS.getObject(COLUMN_RULEGROUPACTION_PARAMETER2) != null && !actionRS.getString(COLUMN_RULEGROUPACTION_PARAMETER2).isEmpty()) {
					Parameter parameter2 = new Parameter(types.get(actionRS.getInt(COLUMN_RULEGROUPACTION_PARAMETER2_TYPE_ID)),
							actionRS.getString(COLUMN_RULEGROUPACTION_PARAMETER2));
					action.addParameter(parameter2);
				}
				if (actionRS.getObject(COLUMN_RULEGROUPACTION_PARAMETER3) != null && !actionRS.getString(COLUMN_RULEGROUPACTION_PARAMETER3).isEmpty()) {
					Parameter parameter3 = new Parameter(types.get(actionRS.getInt(COLUMN_RULEGROUPACTION_PARAMETER3_TYPE_ID)),
							actionRS.getString(COLUMN_RULEGROUPACTION_PARAMETER3));
					action.addParameter(parameter3);
				}
				group.getActions().add(action);
			}
			if (group.isValid()) {
				groups.add(group);
			}
			} catch (Exception e) {
				System.out.println("Couldn't create rulegroup for id " + groupRS.getString(COLUMN_GROUP_ID));
				e.printStackTrace();
			}
		}
		// Close the db connections, as they are no longer necessary
		rulePS.close();
		actionPS.close();
		subGroupPS.close();
		groupPS.close();

		// We also need the reference fields that are used in the rules
		referenceFieldPS.setString(1, projectName);
		ResultSet referenceFieldRS = referenceFieldPS.executeQuery();
		while (referenceFieldRS.next()) {
			ReferenceField tempRF = new ReferenceField();
			tempRF.setName(referenceFieldRS.getString(COLUMN_REFERENCE_FIELDS_NAME));
			tempRF.setNameDescriptive(referenceFieldRS.getString(COLUMN_REFERENCE_FIELDS_NAME_DESCRIPTTIVE));
			tempRF.setDescription(referenceFieldRS.getString(COLUMN_REFERENCE_FIELDS_DESCRIPTION));
			tempRF.setJavaTypeId(Integer.parseInt(referenceFieldRS.getString(COLUMN_REFERENCE_FIELDS_JAVA_TYPE_ID)));
			referenceFields.add(tempRF);
		}
		referenceFieldPS.close();

	}

	/**
	 * returns an arraylist of groups that have been constructed by parsing a xml
	 * rule definition file
	 *
	 * @return list of rulegroups
	 */
	public ArrayList<RuleGroup> getGroups() {
		return groups;
	}

	/**
	 * returns an arraylist of fields that have been constructed by parsing a xml
	 * rule definition file
	 *
	 * @return list of fields
	 */
	public ArrayList<ReferenceField> getReferenceFields() {
		return referenceFields;
	}
}
