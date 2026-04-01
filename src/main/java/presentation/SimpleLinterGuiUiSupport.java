package presentation;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

final class SimpleLinterGuiUiSupport {
    JFileChooser createAddFilesChooser() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select File(s) to Lint");
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
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
}