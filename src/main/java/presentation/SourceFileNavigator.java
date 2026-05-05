package presentation;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

final class SourceFileNavigator {
    void openSourceFile(JFrame parent, File file, int lineNumber) throws IOException {
        if (file == null || !file.exists() || !file.isFile()) {
            String filePath = file == null ? "<unknown>" : file.getPath();
            throw new IOException("File does not exist: " + filePath);
        }

        List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        if (lineNumber < 1 || lineNumber > lines.size()) {
            throw new IOException("Line " + lineNumber + " is not available in " + file.getPath());
        }

        JTextArea sourceArea = new JTextArea(String.join(System.lineSeparator(), lines));
        sourceArea.setEditable(false);
        sourceArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        int selectionStart = calculateSelectionStart(lines, lineNumber);
        int selectionEnd = selectionStart + lines.get(lineNumber - 1).length();
        sourceArea.setCaretPosition(selectionStart);
        sourceArea.setSelectionStart(selectionStart);
        sourceArea.setSelectionEnd(selectionEnd);

        JScrollPane scrollPane = new JScrollPane(sourceArea);
        scrollPane.setPreferredSize(new Dimension(900, 600));

        JDialog dialog = new JDialog(parent, file.getName() + " - line " + lineNumber, true);
        dialog.setLayout(new BorderLayout());
        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    private int calculateSelectionStart(List<String> lines, int lineNumber) {
        int selectionStart = 0;
        for (int i = 0; i < lineNumber - 1; i++) {
            selectionStart += lines.get(i).length();
            selectionStart += System.lineSeparator().length();
        }
        return selectionStart;
    }
}