package com.sri.bolt.dm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.sri.bolt.message.BoltMessages.ErrorSegmentAnnotation;
import com.sri.bolt.message.BoltMessages.SessionData;


public class DmBranchTable {
	private static final String DM_BRANCH_TAG = "dm_branch";
	private static final String DM_BRANCH_FUNCTION_TAG = "function";
	private static final String DM_BRANCH_DESCRIPTION_TAG = "description";
	private static final String DM_BRANCH_FIRST_ACTION_TAG = "first_action";
	private static final String DM_BRANCH_REPEAT_ACTION_TAG = "repeat_action";

	private Document doc;
    private List<DmBranch> branchTable;
    
    @SuppressWarnings("unchecked")
	public DmBranchTable(String xmlfile){
		branchTable = new ArrayList<DmBranch>();
    	DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            doc = db.parse(xmlfile);
            parseDocument();
            Collections.sort(branchTable);
        } 
        catch (Exception exception) {
            
        }
    }

    private void parseDocument() throws Exception {
        Element docEle = doc.getDocumentElement();
        NodeList nl = docEle.getElementsByTagName(DM_BRANCH_TAG);
        if (nl != null && nl.getLength() > 0) {
            // for each dm_branch
            for (int i = 0; i < nl.getLength(); i++) {
            	Element branchElement = (Element) nl.item(i);
                String functionName = getTagValue(branchElement, DM_BRANCH_FUNCTION_TAG);
                String description = getTagValue(branchElement, DM_BRANCH_DESCRIPTION_TAG);
                String firstAction = getTagValue(branchElement, DM_BRANCH_FIRST_ACTION_TAG);
                String repeatAction = getTagValue(branchElement, DM_BRANCH_REPEAT_ACTION_TAG);
                DmBranch dmBranch = new DmBranch(description, functionName, firstAction, repeatAction);
                branchTable.add(dmBranch);
            }
        }
    }

	private String getTagValue(Element ele, String tagName) {
        String textVal = null;
        NodeList nl = ele.getElementsByTagName(tagName);
        if (nl != null && nl.getLength() > 0) {
            Element el = (Element) nl.item(0);
            textVal = el.getFirstChild().getNodeValue();
        }
        return textVal;
    }

	public DmBranch getFirstValidBranch(SessionData session, ErrorSegmentAnnotation errorSegment) {
		
		for (DmBranch branch : branchTable){
			if (branch.isApplicable(session, errorSegment))
				return branch;
		}
		return null;
	}
	
	public List<DmBranch> getBranches(){
		return branchTable;
	}
	
	public static void main(String[] args) {
		String[] branchFiles = new String[2];
		//String ROOT = "/home/boltuser/Controller/boltbc-system";
		String ROOT = "/Users/Akhil/workspace/BOLT_DM_proj";
		
		//branchFiles[0] = ROOT + "/controller/src/main/resources/dm_branches-english.xml";
		//branchFiles[1] = ROOT + "/controller/src/main/resources/dm_branches-arabic.xml";
		branchFiles[0] = ROOT + "/src/dm_branches-english.xml";
		branchFiles[1] = ROOT + "/src/dm_branches-arabic.xml";
		
		Map<String, DmBranch> branches = new TreeMap<String, DmBranch>();
		for (String branchFile : branchFiles) {
			DmBranchTable dmBranch = new DmBranchTable(branchFile);
			for (DmBranch branch : dmBranch.getBranches()) {
				branches.put(branch.getFunctionName(), branch);
			}
		}

		for (String key : branches.keySet()) {
			DmBranch branch = branches.get(key);
			System.out.println();
			System.out.println("   //" + branch.getDescription());
			System.out.println("   public boolean " + branch.getFunctionName() + 
					"(SessionData session, ErrorSegmentAnnotation error);");
		}
	}

	
}
