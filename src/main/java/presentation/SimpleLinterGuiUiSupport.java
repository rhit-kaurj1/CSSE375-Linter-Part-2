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
        chooser.setDialogTitle("Export Linter Output");
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
                "Preview Selected Files and Folders",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        return choice == JOptionPane.OK_OPTION;
    }

    void showFilePreview(JFrame parent, List<File> files) {
        JTextArea previewArea = new JTextArea(buildFilePreview(files));
        previewArea.setEditable(false);
        previewArea.setCaretPosition(0);
        previewArea.setLineWrap(false);

        JScrollPane scrollPane = new JScrollPane(previewArea);
        scrollPane.setPreferredSize(new java.awt.Dimension(900, 600));

        JOptionPane.showMessageDialog(
                parent,
                scrollPane,
                "File Preview",
                JOptionPane.PLAIN_MESSAGE);
    }

    String buildFilePreview(List<File> files) {
        if (files.isEmpty()) {
            return "No files selected.";
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < files.size(); i++) {
            File file = files.get(i);
            String typeLabel = file.isDirectory() ? "Folder" : "File";
            builder.append(typeLabel).append(": ").append(file.getPath()).append(System.lineSeparator());
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

        if (file.isDirectory()) {
            return buildDirectorySummary(file);
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

    private String buildDirectorySummary(File dir) {
        StringBuilder summary = new StringBuilder();
        summary.append("<Directory contents:>").append(System.lineSeparator());

        File[] children = dir.listFiles();
        if (children == null) {
            return summary.append("<Unable to list directory contents>").toString();
        }

        int files = 0;
        int folders = 0;
        for (File child : children) {
            if (child.isDirectory()) {
                folders++;
            } else {
                files++;
            }
        }

        summary.append("  Files: ").append(files).append(System.lineSeparator());
        summary.append("  Subdirectories: ").append(folders).append(System.lineSeparator());

        // Show first few entries
        summary.append("  Contents:").append(System.lineSeparator());
        int shown = 0;
        for (File child : children) {
            if (shown >= 10) {
                summary.append("    ... and ").append(children.length - shown).append(" more").append(System.lineSeparator());
                break;
            }
            String prefix = child.isDirectory() ? "📁 " : "📄 ";
            summary.append("    ").append(prefix).append(child.getName()).append(System.lineSeparator());
            shown++;
        }

        return summary.toString();
    }

    /**
     * Validates selected files for linting.
     * Returns a list of error messages if validation fails.
     * Valid items: .java files, .class files, or directories.
     */
    List<String> validateSelectedFiles(File[] files) {
        List<String> errors = new java.util.ArrayList<>();

        if (files == null || files.length == 0) {
            errors.add("No files selected.");
            return errors;
        }

        for (File file : files) {
            String error = validateSingleFile(file);
            if (error != null) {
                errors.add(error);
            }
        }

        return errors;
    }

    private String validateSingleFile(File file) {
        if (file == null) {
            return "Null file entry provided.";
        }

        if (!file.exists()) {
            return "Invalid path (does not exist): " + file.getAbsolutePath();
        }

        if (!file.canRead()) {
            return "Cannot read file (permission denied): " + file.getAbsolutePath();
        }

        // Directories are allowed (will be expanded when linting)
        if (file.isDirectory()) {
            return null;
        }

        // Check file extension
        String fileName = file.getName().toLowerCase(java.util.Locale.ROOT);
        if (fileName.endsWith(".java") || fileName.endsWith(".class")) {
            return null;
        }

        return "Invalid file type: " + file.getName() + " (only .java and .class files are supported)";
    }
}