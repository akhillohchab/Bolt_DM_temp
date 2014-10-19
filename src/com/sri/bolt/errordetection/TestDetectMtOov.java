package com.sri.bolt.errordetection;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.List;


public class TestDetectMtOov {
	public static void main(String[] args){
		String asrOutputFile = args[0];
		String inputFile = args[1];
		String trnsFile = args[2];
		String alignFile = args[3];
		try{
			int cnt = 0;
			String mtInput;
			String trns;
			String align;
			String asrOutput;
			BufferedReader asrOutputReader = new BufferedReader(new FileReader(asrOutputFile));
			BufferedReader inputReader = new BufferedReader(new FileReader(inputFile));
			BufferedReader trnsReader = new BufferedReader(new FileReader(trnsFile));
			BufferedReader alignReader = new BufferedReader(new FileReader(alignFile));
			while (cnt < 20 && 
					(asrOutput = asrOutputReader.readLine()) != null &&
					(mtInput = inputReader.readLine()) != null && 
					(trns = trnsReader.readLine()) != null &&
					(align = alignReader.readLine()) != null){
				DetectMtOov detector = new DetectMtOov(asrOutput, mtInput, trns, "");
				if (detector.oovWordExists()){
					List<Integer> errorsWithoutAlignment = detector.process();
					DetectMtOov detector2 = new DetectMtOov(asrOutput, mtInput, trns, align);
					List<Integer> errorsWithAlignment = detector2.process();
					String[] asrOutputWords = asrOutput.trim().split("\\s+");
					System.out.print("ASR Input: ");
					for (int i =0; i < asrOutputWords.length; i++){
						System.out.print(i + ":" + asrOutputWords[i] + " ");
					}
					System.out.println();
					String[] mtInputWords = mtInput.trim().split("\\s+");
					System.out.print("MT Input : ");
					for (int i =0; i < mtInputWords.length; i++){
						System.out.print(i + ":" + mtInputWords[i] + " ");
					}
					System.out.println();
					String[] trnsWords = trns.trim().split("\\s+");
					System.out.print("Trns     : ");
					for (int i =0; i < trnsWords.length; i++){
						System.out.print(i + ":" + trnsWords[i] + " ");
					}
					System.out.println();
					System.out.println("Align    : " + align);
					System.out.println("OOVs     : " + errorsWithoutAlignment);
					System.out.println("OOVs(wA) : " + errorsWithAlignment);
					System.out.println("------------------");
					cnt++;
				}
			}
		}
		catch (Exception e){
			e.printStackTrace();
		}
	}
}
