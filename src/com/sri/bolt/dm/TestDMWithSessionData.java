package com.sri.bolt.dm;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import com.sri.bolt.FileIOUtil;
import com.sri.bolt.message.BoltMessages.DmActionType;
import com.sri.bolt.message.BoltMessages.DmClarifyOutput;
import com.sri.bolt.message.BoltMessages.DmClarifySegment;
import com.sri.bolt.message.BoltMessages.DmClarifySegmentActionType;
import com.sri.bolt.message.BoltMessages.DmTranslateOutput;
import com.sri.bolt.message.BoltMessages.DmTranslateSegment;
import com.sri.bolt.message.BoltMessages.DmTranslateSegmentActionType;
import com.sri.bolt.message.BoltMessages.SessionData;
import com.sri.bolt.message.BoltMessages.UtteranceData;

import com.sri.interfaces.lang.Language;
import edu.columbia.bolt.commondata.ConfigurationParameters;
import edu.columbia.bolt.logic.DMLogic;

/**
 * Simple class to read raw SessionData protobuffer from
 * disk then print it. Prints to stdout by default but
 * can specify output to file.
 * @author frandsen
 *
 */
public class TestDMWithSessionData {

	private static final String dmInputFilePattern = ".SessionData";
	
	private static final String responseXmlfile = "C:\\Users\\Akhil\\workspace\\BOLT_DM_proj\\src\\dm_responses-english.xml";
	private static final String branchXmlfile = "C:\\Users\\Akhil\\workspace\\BOLT_DM_proj\\src\\dm_branches-english.xml";
	private static final boolean randomize = false;

	/**
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
    	String priorities = "is_not_resolved,is_asr_oov,is_asr_ambiguous,is_mt_oov,is_mt_word_dropped,is_mt_ambiguous,is_mt_questionable,error_confidence";
        DialogManager dm = new DialogManager(responseXmlfile, branchXmlfile, priorities, Language.ENGLISH, randomize);
        //ConfigurationParameters confParams = ConfigurationParameters.getInstance();
        //confParams.dmVersion = ConfigurationParameters.DMVersionType.RULEBASED;
        //confParams.confScorerWordScoreWekaModelpath = "/home/boltuser/Controller/boltbc-columbia/Externals/ConfScorerWords_Columbia_V2.2.model";
        //confParams.confScorerUttsScoreWekaModelpath = "/home/boltuser/Controller/boltbc-columbia/Externals/ConfScorerUtts_Columbia_V0.model";
        //DMLogic dmLogic = new DMLogic();
        
    	try{
    		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			while (true){
				//System.out.print("Enter a directory: ");
				//String dirName = br.readLine();
				String dirName = "/Users/Akhil/bolt/sessiondata/";
				System.out.println(dirName);
				File dir = new File(dirName);
				FilenameFilter filter = new FilenameFilter() {
				    public boolean accept(File dir, String name) {
				        System.out.println(name.indexOf(dmInputFilePattern));
				    	//return name.indexOf(dmInputFilePattern) != -1;
				        return name.endsWith(dmInputFilePattern);
				    }
				};
				
				FileFilter fileFilter = new FileFilter() {
				    public boolean accept(File file) {
				        return file.isDirectory();
				    }
				};
				
				List<String> allFileNames = new ArrayList<String>();

				File[] allSubdirectories = dir.listFiles(fileFilter);
				if (allSubdirectories == null || allSubdirectories.length == 0){
					String[] fileNames = dir.list(filter);
					for (String fileName : fileNames)
						allFileNames.add(dir + "/" + fileName);
					System.out.println(allFileNames);
				}
				else{
					for (File sub : allSubdirectories){
						String[] fileNames = sub.list(filter);
						if (fileNames != null){
							for (String fileName : fileNames)
								allFileNames.add(sub + "/" + fileName);
						}
					}
				}

				for (String fileName : allFileNames){
					System.out.println("Processing " + fileName);
			        File f = new File(fileName);	
			        byte[] rawData = FileIOUtil.loadFileData(f);
			        if (rawData == null) {
			            System.err.println("Got null data for file: " + f);
			            System.exit(1);
			        }
			        
			        SessionData message = SessionData.parseFrom(rawData);

		            UtteranceData utt = message.getUtterances(0);
		            String workUtt = utt.getAnswerMergerOutput().getWorkingUtterance();
		 
		            com.sri.bolt.message.Util.printErrorSegments(System.out, utt, workUtt, "final");
		            
//			        System.out.println("  Working utt: \"" + message.getUtterances(message.getCurrentTurn()).getAnswerMergerOutput().getWorkingUtterance().trim() + "\"");
//			        try{
//			        	SessionData oldDmMessage = dmLogic.process(confParams, message);
//				        //String s = newMessage.toString();
//			        	String response = getResponse(oldDmMessage.getUtterances(oldDmMessage.getCurrentTurn()));
//			        	System.out.println("  Old Dm_question: " + response);
//			        }
//			        catch (Exception e){
//			        	System.out.println("  Old Dm_question: Exception occurred");
//			        }
			        
			        try{
			        	//System.out.println("  Testing if 'message' works: " + message);
			        	
			        	SessionData newDmMessage = dm.process(message);
			        	System.out.println("  Testing if 'newDmMessage' works: " + newDmMessage);
			        	
			        	
			        	//String s = newMessage.toString();
			        	String response = getResponse(newDmMessage.getUtterances(newDmMessage.getCurrentTurn()));
			        	System.out.println("  New Dm_question: " + response);
			        	
			        }
			        catch (Exception e){
			        	e.printStackTrace();
			        	System.out.println("  New Dm_question: Exception occurred");
			        }
			        
			        
			        System.out.print("Press enter to continue");
			        br.readLine();
				}
			}
		}
		catch (Exception e){
			e.printStackTrace();
		}
    }
    
    public static String getResponse(UtteranceData utt){
    	String question = "Empty Sentence";
    	if (utt.hasDmOutput()){
        	if (utt.getDmOutput().getDmAction() == DmActionType.ACTION_CLARIFY_UTTERANCE){				        		
        		DmClarifyOutput dmClarify = utt.getDmOutput().getDmClarifyOutput();
        		question = getDmClarifyResponse(dmClarify);
                question = "\"" + question.trim() + "\"" + 
                		" [" + utt.getDmOutput().getDmClarifyOutput().getType() + "; " + utt.getDmOutput().getQgRuleId() + "]";
        	}
        	else if (utt.getDmOutput().getDmAction() == DmActionType.ACTION_TRANSLATE_UTTERANCE){
        		DmTranslateOutput dmTranslate = utt.getDmOutput().getDmTranslateOutput();
                question = getDmTranslateResponse(dmTranslate);
                question = "\"" + question.trim() + "\"" + 
                		" [ACTION_TRANSLATE; " + utt.getDmOutput().getQgRuleId() + "]";
        	}
        }
    	return question;
    }
    
    public static String getDmClarifyResponse(DmClarifyOutput dmClarify){
    	String question = "";
   	 	for (DmClarifySegment segment : dmClarify.getSegmentsList()) {
   	 		if (segment.getAction().equals(DmClarifySegmentActionType.ACTION_PLAY_TTS_SEGMENT)) {
   	 			question += segment.getTtsInput() + " ";
   	 		} 
   	 		else if (segment.getAction().equals(DmClarifySegmentActionType.ACTION_PLAY_AUDIO_SEGMENT)) {
   	 			question += "PLAY(error_seg_" + segment.getErrorSegmentIndex() + ") ";
   	 		}
   	 	}
   	 	return question;
    }
    
    public static String getDmTranslateResponse(DmTranslateOutput dmTranslate){
    	String mtInput = "";
        for (DmTranslateSegment segment : dmTranslate.getSegmentsList()) {
           if (segment.getAction().equals(DmTranslateSegmentActionType.ACTION_TRANSLATE_SEGMENT)) {
              mtInput += segment.getMtInput() + " ";
           } else if (segment.getAction().equals(DmTranslateSegmentActionType.ACTION_TRANSLITERATE_SEGMENT)) {
              mtInput += "TRANSLITERATE(error_seg_" + segment.getErrorSegmentIndex() + ") ";
           }
        }
        return mtInput;
    }
}
