package com.sri.bolt.ui;

import com.sri.bolt.EvalType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class NewSessionDialog extends JDialog {
   public NewSessionDialog(String trialId) {
      super();
      this.trialId = trialId;
      this.setTitle("NIST Evaluation Trial");

      JPanel spacerPanel = new JPanel();
      spacerPanel.setLayout(new BoxLayout(spacerPanel, BoxLayout.X_AXIS));
      JPanel inputPanel = new JPanel();
      inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS));

      spacerPanel.add(Box.createRigidArea(new Dimension(20, 0)));
      spacerPanel.add(inputPanel);
      spacerPanel.add(Box.createRigidArea(new Dimension(20, 0)));

      inputPanel.add(new Label("Enter NIST Evaluation Trial Id:"));
      inputPanel.add(new Label("Example: S-S01F01-A1000"));
      inputPanel.add(Box.createRigidArea(new Dimension(0, 3)));

      final TextField trialField = new TextField(trialId);
      inputPanel.add(trialField);
      inputPanel.add(Box.createRigidArea(new Dimension(0, 5)));

      String[] taskStrings = {"Task 1 - Full Clarification", "Task 2 - No Clarification", "Task 3 - Activity B Retest"};
      evalSelectorBox = new JComboBox(taskStrings);
      evalSelectorBox.setSelectedIndex(0);
      inputPanel.add(evalSelectorBox);
      inputPanel.add(Box.createRigidArea(new Dimension(0, 5)));

      JPanel buttonPanel = new JPanel();
      JButton okButton = new JButton("OK");
      JButton cancelButton = new JButton("Cancel");
      buttonPanel.add(okButton);
      buttonPanel.add(cancelButton);
      inputPanel.add(buttonPanel);
      okButton.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            NewSessionDialog.this.trialId = trialField.getText();
            isOk = true;
            if (evalSelectorBox.getSelectedIndex() == 0) {
               evalType = EvalType.WITH_CLARIFICATION;
            } else if (evalSelectorBox.getSelectedIndex() == 1) {
               evalType = EvalType.NO_CLARIFICATION;
            }  else if (evalSelectorBox.getSelectedIndex() == 2) {
               evalType = EvalType.ACTIVITY_B_RETEST;
            }
            setVisible(false);
         }
      });
      cancelButton.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            NewSessionDialog.this.trialId = "";
            isOk = false;
            setVisible(false);
         }
      });

      this.add(spacerPanel);

      isOk = false;
      pack();

   }

   public EvalType getEvalType() {
      return evalType;
   }

   public String getTrialId() {
      return trialId;
   }

   public boolean showDialog() {
      setLocationRelativeTo(null);
      setVisible(true);
      return isOk;
   }

   private JComboBox evalSelectorBox;
   private EvalType evalType;
   private boolean isOk;
   private String trialId;
}
