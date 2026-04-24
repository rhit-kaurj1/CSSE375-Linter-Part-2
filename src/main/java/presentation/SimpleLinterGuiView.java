package presentation;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.File;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;

final class SimpleLinterGuiView {
    private final JFrame frame;

    private final JButton addFilesButton;
    private final JButton removeSelectedButton;
    private final JButton clearAllButton;
    private final JButton runLintersButton;
    private final JButton saveOutputButton;
    private final JButton clearResultsAndLintersButton;
    private final JButton selectAllLintersButton;
    private final JButton deselectAllLintersButton;

    private final JTextArea resultArea;
    private final JLabel statusLabel;

    SimpleLinterGuiView(JFrame frame, JList<File> fileList, List<JCheckBox> linterCheckBoxes) {
        this.frame = frame;
        this.addFilesButton = new JButton("Add Files");
        this.removeSelectedButton = new JButton("Remove Selected");
        this.clearAllButton = new JButton("Clear All");
        this.runLintersButton = new JButton("Run Linters");
        this.saveOutputButton = new JButton("Export");
        this.clearResultsAndLintersButton = new JButton("New Run");
        this.selectAllLintersButton = new JButton("Select All");
        this.deselectAllLintersButton = new JButton("Deselect All");
        this.resultArea = new JTextArea();
        this.statusLabel = new JLabel("Ready");

        configureWindow();
        configureComponents(fileList, linterCheckBoxes);
    }

    JButton getAddFilesButton() {
        return addFilesButton;
    }

    JButton getRemoveSelectedButton() {
        return removeSelectedButton;
    }

    JButton getClearAllButton() {
        return clearAllButton;
    }

    JButton getRunLintersButton() {
        return runLintersButton;
    }

    JButton getSaveOutputButton() {
        return saveOutputButton;
    }

    JButton getClearResultsAndLintersButton() {
        return clearResultsAndLintersButton;
    }

    JButton getSelectAllLintersButton() {
        return selectAllLintersButton;
    }

    JButton getDeselectAllLintersButton() {
        return deselectAllLintersButton;
    }

    JTextArea getResultArea() {
        return resultArea;
    }

    JLabel getStatusLabel() {
        return statusLabel;
    }

    void setRunningState(boolean running) {
        addFilesButton.setEnabled(!running);
        removeSelectedButton.setEnabled(!running);
        clearAllButton.setEnabled(!running);
        runLintersButton.setEnabled(!running);
        saveOutputButton.setEnabled(!running);
        clearResultsAndLintersButton.setEnabled(!running);
        selectAllLintersButton.setEnabled(!running);
        deselectAllLintersButton.setEnabled(!running);

        if (running) {
            statusLabel.setText("Running...");
        }
    }

    private void configureWindow() {
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(10, 10));
        frame.setSize(new Dimension(1000, 650));
        frame.setLocationRelativeTo(null);
    }

    private void configureComponents(JList<File> fileList, List<JCheckBox> linterCheckBoxes) {
        resultArea.setEditable(false);
        resultArea.setLineWrap(false);

        JScrollPane fileListScrollPane = new JScrollPane(fileList);
        fileListScrollPane.setPreferredSize(new Dimension(350, 400));

        JScrollPane resultsScrollPane = new JScrollPane(resultArea);
        resultsScrollPane.setPreferredSize(new Dimension(620, 400));

        JPanel fileButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        fileButtonsPanel.add(addFilesButton);
        fileButtonsPanel.add(removeSelectedButton);
        fileButtonsPanel.add(clearAllButton);

        JPanel filePanel = new JPanel(new BorderLayout(5, 5));
        filePanel.add(new JLabel("Selected Files"), BorderLayout.NORTH);
        filePanel.add(fileListScrollPane, BorderLayout.CENTER);
        filePanel.add(fileButtonsPanel, BorderLayout.SOUTH);

        JPanel checkBoxesPanel = new JPanel();
        checkBoxesPanel.setLayout(new BoxLayout(checkBoxesPanel, BoxLayout.Y_AXIS));
        for (JCheckBox checkBox : linterCheckBoxes) {
            checkBoxesPanel.add(checkBox);
        }
        JScrollPane checkBoxesScrollPane = new JScrollPane(checkBoxesPanel);

        JPanel linterButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        linterButtonsPanel.add(selectAllLintersButton);
        linterButtonsPanel.add(deselectAllLintersButton);

        JPanel linterPanel = new JPanel(new BorderLayout(5, 5));
        linterPanel.add(new JLabel("Select Linters"), BorderLayout.NORTH);
        linterPanel.add(checkBoxesScrollPane, BorderLayout.CENTER);
        linterPanel.add(linterButtonsPanel, BorderLayout.SOUTH);

        JSplitPane leftSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, filePanel, linterPanel);
        leftSplitPane.setResizeWeight(0.5);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actionPanel.add(runLintersButton);
        actionPanel.add(saveOutputButton);
        actionPanel.add(clearResultsAndLintersButton);

        JPanel resultsPanel = new JPanel(new BorderLayout(5, 5));
        resultsPanel.add(new JLabel("Results"), BorderLayout.NORTH);
        resultsPanel.add(resultsScrollPane, BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftSplitPane, resultsPanel);
        splitPane.setResizeWeight(0.35);

        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        bottomPanel.add(actionPanel, BorderLayout.WEST);
        bottomPanel.add(statusLabel, BorderLayout.EAST);

        frame.add(splitPane, BorderLayout.CENTER);
        frame.add(bottomPanel, BorderLayout.SOUTH);
    }
}