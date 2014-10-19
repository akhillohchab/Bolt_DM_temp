package com.sri.bolt.ui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.sri.bolt.workflow.task.WorkflowTaskType;

public class CustomWorkflowDialog extends JDialog {
   public CustomWorkflowDialog(List<WorkflowTaskType> workflowTaskList) {
      super(null, "CHOOSE WORKFLOW START AND SESSION DATA FILE", ModalityType.DOCUMENT_MODAL);
      JPanel contentPane = new JPanel();
      workflowTasksCombo = new JComboBox();
      JButton chooseSessionDataButton = new JButton("Choose Session Data");
      chooseSessionDataButton.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            File parent = getPreviousPath();
            JFileChooser chooser = new JFileChooser();
            if (parent != null) {
               chooser.setCurrentDirectory(parent);
            }
            chooser.setPreferredSize(new Dimension(800, 400));
            int returnVal = chooser.showOpenDialog(CustomWorkflowDialog.this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
               chosenSessionData = chooser.getSelectedFile();
            }
         }
      });

      JButton chooseAudioFileButton = new JButton("Choose Audio Data");
      chooseAudioFileButton.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            File parent = getPreviousPath();
            JFileChooser chooser = new JFileChooser();
            if (parent != null) {
               chooser.setCurrentDirectory(parent);
            }
            chooser.setPreferredSize(new Dimension(800, 400));
            int returnVal = chooser.showOpenDialog(CustomWorkflowDialog.this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
               chosenAudioFile = chooser.getSelectedFile();
            }
         }
      });

      JButton okayButton = new JButton("OK");
      okayButton.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            String selectedObj = (String) workflowTasksCombo.getSelectedItem();
            chosenTaskType = workflowTasks.get(selectedObj);
            if (chosenSessionData == null) {
               JOptionPane.showMessageDialog(CustomWorkflowDialog.this, "No Session Data Chosen!");
            } else if (chosenAudioFile == null) {
               JOptionPane.showMessageDialog(CustomWorkflowDialog.this, "No Audio Data Chosen!");
            } else {
               setSuccess(true);
               setVisible(false);
            }
         }
      });

      JButton cancelButton = new JButton("cancel");
      cancelButton.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            setSuccess(false);
            setVisible(false);
         }
      });

      workflowTasks = new HashMap<String, WorkflowTaskType>();
      for (WorkflowTaskType type : workflowTaskList) {
         if (type != WorkflowTaskType.AUDIO) {
            String name = com.sri.bolt.workflow.Util.getNameForWorkflowType(type);
            workflowTasksCombo.addItem(name);
            workflowTasks.put(name, type);
         }
      }

      contentPane.add(workflowTasksCombo);
      contentPane.add(chooseSessionDataButton);
      contentPane.add(chooseAudioFileButton);
      contentPane.add(okayButton);
      contentPane.add(cancelButton);
      setContentPane(contentPane);
      setLocationRelativeTo(null);

      pack();
   }

   public File getChosenAudioFile() {
      return chosenAudioFile;
   }

   public void setChosenAudioFile(File chosenAudioFile) {
      this.chosenAudioFile = chosenAudioFile;
   }

   public File getChosenSessionData() {
      return chosenSessionData;
   }

   public void setChosenSessionData(File chosenSessionData) {
      this.chosenSessionData = chosenSessionData;
   }

   public WorkflowTaskType getChosenTaskType() {
      return chosenTaskType;
   }

   public void setChosenTaskType(WorkflowTaskType chosenTaskType) {
      this.chosenTaskType = chosenTaskType;
   }

   public boolean isSuccess() {
      return success;
   }

   public void setSuccess(boolean success) {
      this.success = success;
   }

   private File getPreviousPath() {
      if (chosenSessionData != null) {
         return chosenSessionData.getParentFile();
      } else if (chosenAudioFile != null) {
         return chosenAudioFile.getParentFile();
      } else {
         return null;
      }
   }

   private File chosenSessionData;
   private File chosenAudioFile;
   private WorkflowTaskType chosenTaskType;
   private boolean success = false;

   private JComboBox workflowTasksCombo;
   private Map<String, WorkflowTaskType> workflowTasks;
}
