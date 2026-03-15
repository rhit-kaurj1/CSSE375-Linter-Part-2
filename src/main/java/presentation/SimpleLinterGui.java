package presentation;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;

import datastorage.ConfigLoader;
import domain.Linter;
import domain.LinterConfig;

public class SimpleLinterGui extends JFrame {
    private static final String DEFAULT_CONFIG_PATH = "linter.properties";

    private final DefaultListModel<File> fileListModel;
    private final JList<File> fileList;
    private final JTextArea resultArea;
    private final JLabel statusLabel;
    private final JButton addFilesButton;
    private final JButton removeSelectedButton;
    private final JButton clearAllButton;
    private final JButton runAllLintersButton;
    private final JButton saveOutputButton;

    private final LinterFactory linterFactory;
    private final LinterRunner linterRunner;
    private final ConfigLoader configLoader;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SimpleLinterGui gui = new SimpleLinterGui();
            gui.setVisible(true);
        });
    }

    public SimpleLinterGui() {
        super("Simple GUI Linter Runner");

        this.fileListModel = new DefaultListModel<>();
        this.fileList = new JList<>(fileListModel);
        this.resultArea = new JTextArea();
        this.statusLabel = new JLabel("Ready");

        this.addFilesButton = new JButton("Add Files");
        this.removeSelectedButton = new JButton("Remove Selected");
        this.clearAllButton = new JButton("Clear All");
        this.runAllLintersButton = new JButton("Run All Linters");
        this.saveOutputButton = new JButton("Save Output");

        this.linterFactory = new LinterFactory();
        this.linterRunner = new LinterRunner();
        this.configLoader = new ConfigLoader();

        configureWindow();
        configureComponents();
        wireActions();
    }

    private void configureWindow() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        setSize(new Dimension(1000, 650));
        setLocationRelativeTo(null);
    }

    private void configureComponents() {
        fileList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

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

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actionPanel.add(runAllLintersButton);
        actionPanel.add(saveOutputButton);

        JPanel resultsPanel = new JPanel(new BorderLayout(5, 5));
        resultsPanel.add(new JLabel("Results"), BorderLayout.NORTH);
        resultsPanel.add(resultsScrollPane, BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, filePanel, resultsPanel);
        splitPane.setResizeWeight(0.35);

        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        bottomPanel.add(actionPanel, BorderLayout.WEST);
        bottomPanel.add(statusLabel, BorderLayout.EAST);

        add(splitPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void wireActions() {
        addFilesButton.addActionListener(e -> onAddFiles());
        removeSelectedButton.addActionListener(e -> onRemoveSelected());
        clearAllButton.addActionListener(e -> onClearAll());
        runAllLintersButton.addActionListener(e -> onRunAllLinters());
        saveOutputButton.addActionListener(e -> onSaveOutput());
    }

    private void onAddFiles() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select File(s) to Lint");
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setAcceptAllFileFilterUsed(true);
        chooser.addChoosableFileFilter(new FileNameExtensionFilter("Java/Class Files", "java", "class"));

        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File[] selectedFiles = chooser.getSelectedFiles();
        for (File file : selectedFiles) {
            if (file == null) {
                continue;
            }
            if (containsFile(file)) {
                continue;
            }
            fileListModel.addElement(file);
        }
        statusLabel.setText("Selected files: " + fileListModel.size());
    }

    private void onRemoveSelected() {
        List<File> selected = fileList.getSelectedValuesList();
        for (File file : selected) {
            fileListModel.removeElement(file);
        }
        statusLabel.setText("Selected files: " + fileListModel.size());
    }

    private void onClearAll() {
        fileListModel.clear();
        statusLabel.setText("Ready");
    }

    private void onRunAllLinters() {
        List<File> selectedFiles = getSelectedFiles();
        if (selectedFiles.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please add at least one file.", "No Files Selected",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<File> readableFiles = new ArrayList<>();
        List<File> skippedFiles = new ArrayList<>();
        for (File file : selectedFiles) {
            if (file.exists() && file.isFile() && file.canRead()) {
                readableFiles.add(file);
            } else {
                skippedFiles.add(file);
            }
        }

        if (readableFiles.isEmpty()) {
            resultArea.setText("No readable files selected.\n");
            appendSkippedFiles(skippedFiles);
            return;
        }

        setRunningState(true);
        resultArea.setText("Running all linters...\n");

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                LinterConfig config = configLoader.loadConfig(DEFAULT_CONFIG_PATH);
                LinterConfig runAllConfig = new LinterConfig(
                        null,
                        config.getTooManyParametersLimit(),
                        config.getSrpLcomThreshold());

                List<Linter> availableLinters = linterFactory.createLinters(runAllConfig);
                String output = linterRunner.runAllLinters(readableFiles, availableLinters);
                return addRunSummary(output, readableFiles.size(), skippedFiles);
            }

            @Override
            protected void done() {
                try {
                    resultArea.setText(get());
                    statusLabel.setText("Completed");
                } catch (Exception ex) {
                    resultArea.setText("Lint run failed: " + ex.getMessage());
                    statusLabel.setText("Failed");
                } finally {
                    setRunningState(false);
                }
            }
        };
        worker.execute();
    }

    private void onSaveOutput() {
        String output = resultArea.getText();
        if (output == null || output.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "No output to save.", "Nothing to Save", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Linter Output");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setSelectedFile(new File("linter-output.txt"));

        int result = chooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        Path outputPath = chooser.getSelectedFile().toPath();
        try {
            Files.writeString(outputPath, output, StandardCharsets.UTF_8);
            statusLabel.setText("Saved: " + outputPath.getFileName());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Could not save output: " + ex.getMessage(), "Save Failed",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean containsFile(File candidate) {
        String candidatePath = candidate.getAbsolutePath();
        for (int i = 0; i < fileListModel.size(); i++) {
            File existing = fileListModel.getElementAt(i);
            if (existing.getAbsolutePath().equals(candidatePath)) {
                return true;
            }
        }
        return false;
    }

    private List<File> getSelectedFiles() {
        List<File> files = new ArrayList<>();
        for (int i = 0; i < fileListModel.size(); i++) {
            files.add(fileListModel.getElementAt(i));
        }
        return files;
    }

    private String addRunSummary(String linterOutput, int processedCount, List<File> skippedFiles) {
        StringBuilder summary = new StringBuilder();
        summary.append("Files processed: ").append(processedCount).append(System.lineSeparator());
        summary.append("Files skipped: ").append(skippedFiles.size()).append(System.lineSeparator())
                .append(System.lineSeparator());

        if (!skippedFiles.isEmpty()) {
            summary.append("Skipped files:").append(System.lineSeparator());
            for (File skippedFile : skippedFiles) {
                summary.append("- ").append(skippedFile.getPath()).append(System.lineSeparator());
            }
            summary.append(System.lineSeparator());
        }

        if (linterOutput == null || linterOutput.trim().isEmpty()) {
            summary.append("No issues found.");
            return summary.toString();
        }

        summary.append(linterOutput);
        return summary.toString();
    }

    private void appendSkippedFiles(List<File> skippedFiles) {
        if (skippedFiles.isEmpty()) {
            return;
        }
        resultArea.append("Skipped files:\n");
        for (File file : skippedFiles) {
            resultArea.append("- " + file.getPath() + "\n");
        }
    }

    private void setRunningState(boolean running) {
        addFilesButton.setEnabled(!running);
        removeSelectedButton.setEnabled(!running);
        clearAllButton.setEnabled(!running);
        runAllLintersButton.setEnabled(!running);
        saveOutputButton.setEnabled(!running);

        if (running) {
            statusLabel.setText("Running...");
        }
    }
}
