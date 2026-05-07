package presentation;

import java.awt.Component;
import java.io.File;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

/**
 * Custom cell renderer for FileSelectionModel that displays folders and files
 * distinctly, with folder indicators and child file counts.
 */
final class FileListCellRenderer extends DefaultListCellRenderer {
    private static final long serialVersionUID = 1L;

    @Override
    public Component getListCellRendererComponent(
            JList<?> list,
            Object value,
            int index,
            boolean isSelected,
            boolean cellHasFocus) {

        if (!(value instanceof File)) {
            return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        }

        File file = (File) value;
        String displayText = buildDisplayText(file);

        return super.getListCellRendererComponent(list, displayText, index, isSelected, cellHasFocus);
    }

    private String buildDisplayText(File file) {
        if (file.isDirectory()) {
            int fileCount = countFiles(file);
            return "📁 [Folder] " + file.getName() + " (" + fileCount + " file(s))";
        } else {
            return file.getName();
        }
    }

    private int countFiles(File dir) {
        int count = 0;
        File[] children = dir.listFiles();
        if (children == null) {
            return 0;
        }
        for (File child : children) {
            if (child.isFile()) {
                count++;
            } else if (child.isDirectory()) {
                count += countFiles(child);
            }
        }
        return count;
    }
}
