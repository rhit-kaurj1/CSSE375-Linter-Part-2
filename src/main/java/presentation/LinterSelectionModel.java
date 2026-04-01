package presentation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JCheckBox;

import domain.Linter;

final class LinterSelectionModel {
    private final Map<Linter, JCheckBox> linterCheckBoxes;

    LinterSelectionModel(List<Linter> allAvailableLinters) {
        this.linterCheckBoxes = new LinkedHashMap<>();
        for (Linter linter : allAvailableLinters) {
            this.linterCheckBoxes.put(linter, new JCheckBox(linter.getClass().getSimpleName(), false));
        }
    }

    List<JCheckBox> getCheckBoxesInDisplayOrder() {
        return new ArrayList<>(linterCheckBoxes.values());
    }

    List<Linter> getSelectedLinters() {
        List<Linter> selectedLinters = new ArrayList<>();
        for (Map.Entry<Linter, JCheckBox> entry : linterCheckBoxes.entrySet()) {
            if (entry.getValue().isSelected()) {
                selectedLinters.add(entry.getKey());
            }
        }
        return selectedLinters;
    }

    void selectAll() {
        for (JCheckBox checkBox : linterCheckBoxes.values()) {
            checkBox.setSelected(true);
        }
    }

    void deselectAll() {
        for (JCheckBox checkBox : linterCheckBoxes.values()) {
            checkBox.setSelected(false);
        }
    }
}