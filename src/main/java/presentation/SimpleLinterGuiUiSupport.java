package presentation;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.filechooser.FileNameExtensionFilter;

final class SimpleLinterGuiUiSupport {
    JFileChooser createAddFilesChooser() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select File(s) or Folder(s) to Lint");
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        chooser.setAcceptAllFileFilterUsed(true);
        chooser.addChoosableFileFilter(new FileNameExtensionFilter("Java/Class Files", "java", "class"));
        return chooser;
    }

    JFileChooser createSaveOutputChooser() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Linter Output");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setSelectedFile(new File("linter-output.txt"));
        return chooser;
    }

    void showWarning(JFrame parent, String message, String title) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.WARNING_MESSAGE);
    }

    void showInfo(JFrame parent, String message, String title) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    void showError(JFrame parent, String message, String title) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.ERROR_MESSAGE);
    }

    void showSelectedFilesStatus(SimpleLinterGuiView view, int selectedFileCount) {
        view.getStatusLabel().setText("Selected files: " + selectedFileCount);
    }

    boolean confirmFilePreview(JFrame parent, List<File> files) {
        JTextArea previewArea = new JTextArea(buildFilePreview(files));
        previewArea.setEditable(false);
        previewArea.setCaretPosition(0);
        previewArea.setLineWrap(false);

        JScrollPane scrollPane = new JScrollPane(previewArea);
        scrollPane.setPreferredSize(new java.awt.Dimension(800, 500));

        int choice = JOptionPane.showConfirmDialog(
                parent,
                scrollPane,
                "Preview Selected Files",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        return choice == JOptionPane.OK_OPTION;
    }

    String buildFilePreview(List<File> files) {
        if (files.isEmpty()) {
            return "No files selected.";
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < files.size(); i++) {
            File file = files.get(i);
            builder.append("File: ").append(file.getPath()).append(System.lineSeparator());
            builder.append("--------------------").append(System.lineSeparator());
            builder.append(readPreviewContent(file)).append(System.lineSeparator());
            if (i < files.size() - 1) {
                builder.append(System.lineSeparator());
            }
        }
        return builder.toString();
    }

    private String readPreviewContent(File file) {
        if (file == null) {
            return "<No file provided>";
        }

        if (!file.exists()) {
            return "<File does not exist>";
        }

        if (file.getName().endsWith(".class")) {
            return "<Binary .class file preview unavailable>";
        }

        try {
            return Files.readString(file.toPath(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            return "<Unable to read file: " + ex.getMessage() + ">";
        }
    }
}