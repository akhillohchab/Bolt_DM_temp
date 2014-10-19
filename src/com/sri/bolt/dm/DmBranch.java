package com.sri.bolt.dm;

import java.lang.reflect.Method;

import com.sri.bolt.message.BoltMessages.ErrorSegmentAnnotation;
import com.sri.bolt.message.BoltMessages.SessionData;
import com.sri.bolt.App;
import com.sri.bolt.App.*;

public class DmBranch implements Comparable{
	private static final DmBranchFunctionImpl dmBranchFunctionImpl = new DmBranchFunctionImpl();
	private static int lastId = 0;
	
	private int id;
	private String description;
	private String functionName;
	private DmResponseTypeRuleIdPair firstAction;
	private DmResponseTypeRuleIdPair repeatAction;

	public DmBranch(){
		
	}
	
	public DmBranch(String description, String functionName, String firstAction, String repeatAction) {
		this.id = ++lastId;
		this.description = description;
		this.functionName = functionName;
		this.firstAction = new DmResponseTypeRuleIdPair(firstAction);
		if (repeatAction != null)
			this.repeatAction = new DmResponseTypeRuleIdPair(repeatAction);
		else
			this.repeatAction = new DmResponseTypeRuleIdPair(firstAction);
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getDescription(){
		return description;
	}
	
	public String getFunctionName(){
		return functionName;
	}
	
	public DmResponseTypeRuleIdPair getFirstAction() {
		return firstAction;
	}

	public void setFirstAction(DmResponseTypeRuleIdPair firstAction) {
		this.firstAction = firstAction;
	}

	public DmResponseTypeRuleIdPair getRepeatAction() {
		return repeatAction;
	}

	public void setRepeatAction(DmResponseTypeRuleIdPair repeatAction) {
		this.repeatAction = repeatAction;
	}
	
	public boolean isApplicable(SessionData session, ErrorSegmentAnnotation errorSegment){
		//App.getLog4jLogger().info("Function name: " + functionName);
		Method m = FunctionLoader.getMethod(dmBranchFunctionImpl.getClass(), functionName, session, errorSegment);
		if (m != null)
			return (Boolean) FunctionLoader.invokeMethod(dmBranchFunctionImpl, functionName, session, errorSegment);
		return false;
	}
	
	@Override
	public int compareTo(Object arg0) {
		DmBranch other = (DmBranch) arg0;
		if (this.id < other.id)
			return -1;
		if (this.id > other.id)
			return 1;
		return 0;
	}
}
