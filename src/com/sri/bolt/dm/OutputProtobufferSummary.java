package com.sri.bolt.dm;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.sri.bolt.FileIOUtil;
import com.sri.bolt.message.BoltMessages.ErrorSegmentAnnotation;
import com.sri.bolt.message.BoltMessages.SessionData;
import com.sri.bolt.message.BoltMessages.UtteranceData;

/**
 * Creates a summary from the sessiondata. 
 * Traverse all subfolders of dirName and  output tab-delimited table.
 * Output one line for each sessiondata file: asr hypothesis, detected error segemnts, and DM action
 * All output fields are listed in outputHeader variable 
 * 
 * Used with the dry run version of the system.
 * 
 * 
 * @author sstoyanchev
 *
 */
public class OutputProtobufferSummary {
	//specifies pattern of the filename to be used for processing the input
	public static String dmArabicInputFilePattern = "-08-Dialog-Manager-IA-complete.SessionData";
	public static String dmEnglishInputFilePattern = "-Dialog-Manager-EN-complete.SessionData";

	//when extracting data from the version of the system withput clarification, use these patterns
	public static String dmEnglishTranslationInputFilePattern = "STEP-02-English-Translation-complete.SessionData";
	public static String dmArabitTranslationInputFilePattern = "STEP-02-IA-Translation-complete.SessionData";

	//inpu folder contains all *.sessiondata files
	public static String dirName = "~/Desktop/MTUtterances/MT0205_01";
	//public static String dirName = "/proj/speech/projects/bolt/data/recordedAtColumbia/BOLT2013";
	public static String outFileName = "~/Desktop/outfile.csv";
	//public static String outFileName = "/proj/speech/projects/bolt/data/recordedAtColumbia/BOLT2013/recordedAtColumbia-07-26-13.csv";
	
	//fields output for each 
	public static String outputHeader = "trial\t" + "index (for sorting)\t" + "language\t" + "Clar#\t" + 
				      "Rescored ASR\t" + "Merged Utt\t" + "Translation\t"+ "ASR-err-detect\t" + 
				      "ASR-err-g0\t" + "ASR-err-g40\t" + "ASR-err-g43\t" +"ASR-err-g46\t" + 
  					  "MT-err-detect\t" + "DM-Action\t" + "Attribute\t" + "errseg"; 
	
	
    //****************************************************************************
	// input parameter: dirname that contains sessionData files
	//                  output filename to be written	
    //****************************************************************************

	   public static void main(String[] args) throws IOException {
		   
		   if(args.length!=2)
		   {
			   System.out.println("Usage: OutputProtobufferSummary dirname outfile.csv");
			   return;
		   }
			
		   dirName = args[0];
		   outFileName = args[1];
		   File dir = new File(dirName);

			FilenameFilter filter = new FilenameFilter() {
			    public boolean accept(File dir, String name) {
			        return name.indexOf(dmEnglishInputFilePattern) != -1 || name.indexOf(dmArabicInputFilePattern) != -1 ;
			    	// return name.indexOf(dmEnglishTranslationInputFilePattern) != -1 || name.indexOf(dmArabitTranslationInputFilePattern) != -1 ;
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
				{
					allFileNames.add(dir + "/" + fileName);
				}
			}
			else{
				for (File sub : allSubdirectories){
					String[] fileNames = sub.list(filter);
					System.err.println("processing subdir: " + sub);
					if (fileNames != null){
						for (String fileName : fileNames)
						{
							allFileNames.add(sub + "/" + fileName);
							System.err.println("processing files: " + sub);
						}
					}
				}
			}		   
			
			String outString = outputHeader + "\n";
		   
		    for (String fileName : allFileNames){
			    System.out.println("Processing " + fileName);
		        File f = new File(fileName);	
		        byte[] rawData = FileIOUtil.loadFileData(f);
		        if (rawData == null) {
		            System.err.println("Got null data for file: " + f);
		            System.exit(1);
		        }
		        

		        SessionData message = SessionData.parseFrom(rawData);
	            int clarifTurn = message.getCurrentTurn();
	            System.out.println("Clarif Turn:" + clarifTurn);
	       
	            UtteranceData utt = message.getUtterances(clarifTurn);
	            String currentASR = utt.getRecognizer1Best();
	            System.out.println("asr-utt: " + currentASR);
	            String currentrescoredASR = utt.getRescored1Best();
	            System.out.println("asr-utt: " + currentrescoredASR);
	            String workUtt = utt.getAnswerMergerOutput().getWorkingUtterance();
	            System.out.println("working-utt: " + workUtt);
	            
	            

	            
	            String dmAction = utt.getDmOutput().getDmAction().toString();
	            System.out.println("dmAction: " + dmAction);
	            
	            //****************************************************************************
	            //process DM action
	            //****************************************************************************

	            String dmClarifyType = "";
	            int errSegmentID = -1;
	            String attributeStr = "";
	            if(utt.getDmOutput().getDmAction()== 
	            		com.sri.bolt.message.BoltMessages.DmActionType.ACTION_CLARIFY_UTTERANCE)
	            {
	            	dmClarifyType = utt.getDmOutput().getDmClarifyOutput().getType().toString();
            		errSegmentID = utt.getDmOutput().getDmClarifyOutput().getErrorSegmentIndex();
	            	if( utt.getDmOutput().getDmClarifyOutput().getType().equals(com.sri.bolt.message.BoltMessages.DmClarificationType.ACTION_CONFIRM_ATTRIBUTE))
	            	{
	            		if(utt.getDmOutput().getDmClarifyOutput().getTargetedAttribute()!=null)
	            			attributeStr = utt.getDmOutput().getDmClarifyOutput().getTargetedAttribute().toString();
	            	}
		            System.out.println("dmClarifyType: " + dmClarifyType + " ClarifySegment: " + errSegmentID + "Attr" + attributeStr);

	            }

	            //****************************************************************************
	            // process error segments
	            //****************************************************************************
	            
	            String asrErrorStr = "";
	            String asrErrorIndexes_g0 = "";
	            String asrErrorIndexes_g40 = "";
	            String asrErrorIndexes_g46 = "";
	            String asrErrorIndexes_g43 = "";
	            String mtErrorStr = "";
	            for (int errorSegmentCount = 0; errorSegmentCount < utt.getErrorSegmentsCount(); ++errorSegmentCount) {
	                ErrorSegmentAnnotation errorSeg = utt.getErrorSegments(errorSegmentCount);

	                int startIndex = errorSeg.getStartIndex();	      
	                int endIndex = errorSeg.getEndIndex();	 
	                double errConf = errorSeg.getConfidence();
	                
	                if(errorSeg.getErrorType().equals(com.sri.bolt.message.BoltMessages.ErrorSegmentType.ERROR_SEGMENT_ASR))
		                {
		                asrErrorStr += "[" + startIndex + ";" + endIndex + ";" + String.format("%5.3f", errConf)+ "]";
		                
		                for (int i  = startIndex; i <=endIndex; i++)
		                	asrErrorIndexes_g0 += " " + i;
		                
		                if(errConf>.40)
		                	for (int i  = startIndex; i <=endIndex; i++)
		                		asrErrorIndexes_g40 += " " + i;
		                
		                if(errConf>.43)
		                	for (int i  = startIndex; i <=endIndex; i++)
		                		asrErrorIndexes_g43 += " " + i;		 
		                
		                if(errConf>.46)
		                	for (int i  = startIndex; i <=endIndex; i++)
		                		asrErrorIndexes_g46 += " " + i;		                
		                
		                boolean isOOV = false;
		                double oovConf = 0;
		                if ( errorSeg.getIsAsrOov()!=null) 
		                	{
		                	isOOV = errorSeg.getIsAsrOov().getValue();
		                	oovConf = errorSeg.getIsAsrOov().getConfidence();
		                	asrErrorStr += "oov=" + isOOV +":"+  String.format("%5.3f", oovConf);
		                	}
		                
		                
		                
	                	String neType = "";
		                double neConf = 0;
		                if ( errorSeg.getNeTag()!=null) 
		                	{
		                	neType = errorSeg.getNeTag().getValue();
		                	neConf = errorSeg.getIsAsrOov().getConfidence();
		                	asrErrorStr += ";ne=" + neType +":"+  String.format("%5.3f", neConf);
		                	}
		                
		                String semRole = "";
		                if(errorSeg.getSemanticRole()!=null)
		                {
		                	semRole = errorSeg.getSemanticRole().getValue();
		                	asrErrorStr += ";sem="+ semRole;  	
		                }
		                
		                String whWord = "";
		                if(errorSeg.getWhQuestion()!=null)
		                {
		                	semRole = errorSeg.getSemanticRole().getValue();
		                	asrErrorStr += ";wh="+ whWord;  	
		                }		                

		                }
	                	else if(errorSeg.getErrorType().equals(com.sri.bolt.message.BoltMessages.ErrorSegmentType.ERROR_SEGMENT_MT))
		                {
			                mtErrorStr += "[" + startIndex + ";" + endIndex + ";" + String.format("%5.3f", errConf)+ "]";
			            }
	                
	           	
	                
	                }
	            
	            

	            
	            //this should be clarifTurn instead of 0 but it crashes
	            String translationStr = utt.getMtData().getPostprocessedTranslations(0);
	            
	            System.out.println("AsrErrors:" + asrErrorStr);
	            System.out.println("MTErrors:" + mtErrorStr);
	            com.sri.bolt.message.Util.printErrorSegments(System.out, utt, workUtt, "final");
	            //String trial  id = fileName.replaceFirst(".*dryrun2013\\/", "").replaceFirst("\\/.*", "");
	            String trialid = fileName.replaceFirst(".*MHImage\\/", "").replaceFirst("\\/.*", "");
	            String language = fileName.replaceFirst("-complete.*", "").replaceFirst(".*-Dialog-Manager-", "");
	            fileName = fileName.replaceFirst(".*\\/", "").replaceFirst("STEP.*", "");
	            outString += trialid + "\t"+ fileName + "\t" + language + "\t" + clarifTurn + "\t"+ 
	            currentrescoredASR + "\t" + workUtt + "\t" + translationStr + "\t" + 
	            asrErrorStr + "\t" + asrErrorIndexes_g0 + "\t" + asrErrorIndexes_g40 + "\t" + 
	            asrErrorIndexes_g43 + "\t" + asrErrorIndexes_g46 + "\t" +mtErrorStr + "\t" + 
	            dmAction + "-" + dmClarifyType + "\t" + attributeStr + "\t" + errSegmentID + "\n";
		    }

		    
            FileWriter output = null;
            try {
              output = new FileWriter(outFileName);
              BufferedWriter writer = new BufferedWriter(output);
              writer.write(outString);
              writer.close();
            } 
              catch (IOException e) {
                 System.err.println("failed to write to file");
                
            }
            

	   }
	
}
