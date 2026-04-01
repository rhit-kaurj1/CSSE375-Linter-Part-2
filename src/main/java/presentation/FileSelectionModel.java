package presentation;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.ListSelectionModel;

final class FileSelectionModel {
    private final DefaultListModel<File> fileListModel;
    private final JList<File> fileList;

    FileSelectionModel() {
        this.fileListModel = new DefaultListModel<>();
        this.fileList = new JList<>(fileListModel);
        this.fileList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    }

    JList<File> getListComponent() {
        return fileList;
    }

    int size() {
        return fileListModel.size();
    }

    void addFiles(File[] files) {
        for (File file : files) {
            if (file == null || containsFile(file)) {
                continue;
            }
            fileListModel.addElement(file);
        }
    }

    void removeSelected() {
        List<File> selected = fileList.getSelectedValuesList();
        for (File file : selected) {
            fileListModel.removeElement(file);
        }
    }

    void clear() {
        fileListModel.clear();
    }

    List<File> getAllFiles() {
        List<File> files = new ArrayList<>();
        for (int i = 0; i < fileListModel.size(); i++) {
            files.add(fileListModel.getElementAt(i));
        }
        return files;
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
}