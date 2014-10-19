package com.sri.bolt.dm;

import com.sri.bolt.message.BoltMessages.DmClarifySegment;
import com.sri.bolt.message.BoltMessages.ErrorSegmentAnnotation;
import com.sri.bolt.message.BoltMessages.ErrorSegmentAttributeType;

import java.util.ArrayList;
import java.util.List;

public class DmResponse {
   private List<DmSegment> segments;
   private ErrorSegmentAttributeType targetedAttribute;

   // ACTION_CLARIFY = 0;
   // ACTION_CONFIRM = 1;
   // ACTION_CONFIRM_ATTRIBUTE = 2;
   // ACTION_ASK_REPHRASE_PART = 3;
   // ACTION_REJECT = 4;
   // ACTION_SPELL = 5;
   // ACTION_DISAMBIGUATE = 6;
   // ACTION_ASK_REPEAT_PART = 7;
   // ACTION_TRANSLATE (added)
   // ACTION_PREAMBLE_SORRY (added)

   public DmResponse() {
      segments = new ArrayList<DmSegment>();
   }

   public void addSegment(DmSegment segment) {
      segments.add(segment);
   }

   public List<DmSegment> getSegments() {
      return segments;
   }

   public ErrorSegmentAttributeType getTargetedAttribute() {
      return targetedAttribute;
   }

   public void setTargetedAttribute(ErrorSegmentAttributeType targetedAttribute) {
      this.targetedAttribute = targetedAttribute;
   }

   public String toString() {
      String result = "";
      for (DmSegment segment : segments)
         result += segment.toString() + " ";
      return result;
   }

   public List<DmClarifySegment> getClarifySegments(String workingUtterance, ErrorSegmentAnnotation errorSegment,
                                                    int errorSegmentIndex) {
      List<DmClarifySegment> results = new ArrayList<DmClarifySegment>();
      for (DmSegment segment : segments) {
         DmClarifySegment dmClarifySegment = segment.convertToDmClarifySegment(workingUtterance, errorSegment,
                 errorSegmentIndex);
         results.add(dmClarifySegment);
      }
      return results;
   }

   public DmResponse mergeWith(DmResponse dmResponse) {
      DmResponse newDmResponse = new DmResponse();
      if (targetedAttribute != null)
         newDmResponse.setTargetedAttribute(targetedAttribute);
      if (dmResponse.getTargetedAttribute() != null)
         newDmResponse.setTargetedAttribute(dmResponse.getTargetedAttribute());
      for (DmSegment segment : segments)
         newDmResponse.addSegment(segment);
      for (DmSegment segment : dmResponse.getSegments())
         newDmResponse.addSegment(segment);
      return newDmResponse;
   }

}
