package com.sri.bolt.ui;

import java.awt.FlowLayout;
import javax.swing.JPanel;

public class DualAudioSelectionPanel extends JPanel {
   private AudioSelectionPanel mPanel1;
   private AudioSelectionPanel mPanel2;
   private AudioSelectionPanel mPanel3;

   public DualAudioSelectionPanel(String prefix1, String[] recordChoices1, int recIdx1, String[] playChoices1, int playIdx1,
                                  String prefix2, String[] recordChoices2, int recIdx2, String[] playChoices2, int playIdx2,
                                  String prefix3, String[] recordChoices3, int recIdx3, String[] playChoices3, int playIdx3) {
      super();
      setLayout(new FlowLayout(FlowLayout.CENTER, 5, 0));

      mPanel1 = new AudioSelectionPanel(prefix1, recordChoices1, recIdx1, playChoices1, playIdx1);
      mPanel2 = new AudioSelectionPanel(prefix2, recordChoices2, recIdx2, playChoices2, playIdx2);
      mPanel3 = new AudioSelectionPanel(prefix3, recordChoices3, recIdx3, playChoices3, playIdx3);

      this.add(mPanel1);
      this.add(mPanel2);
      // Third meant for playback only; recordChoices3 should be null
      this.add(mPanel3);

      // If value changed or upon testing output, make sure all displayed levels
      // across panels are correct.
      LevelChangeListener l = new LevelChangeListener() {
         @Override
         public void checkLevels() {
            mPanel1.updateLevels();
            mPanel2.updateLevels();
            mPanel3.updateLevels();
         }
      };

      mPanel1.setListener(l);
      mPanel2.setListener(l);
      mPanel3.setListener(l);
   }

   public String getPlaySelection1() {
      return mPanel1.getPlaySelection();
   }

   public String getRecordSelection1() {
      return mPanel1.getRecordSelection();
   }

   public String getPlaySelection2() {
      return mPanel2.getPlaySelection();
   }

   public String getRecordSelection2() {
      return mPanel2.getRecordSelection();
   }

   public String getPlayMonitorSelection() {
      return mPanel3.getPlaySelection();
   }

}
