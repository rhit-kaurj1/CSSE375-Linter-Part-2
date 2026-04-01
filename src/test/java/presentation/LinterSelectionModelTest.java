package presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;

import javax.swing.JCheckBox;

import org.junit.jupiter.api.Test;

import domain.Linter;

class LinterSelectionModelTest {

    @Test
    void selectAllMarksEveryLinterAsSelected() {
        FirstTestLinter first = new FirstTestLinter();
        SecondTestLinter second = new SecondTestLinter();
        LinterSelectionModel model = new LinterSelectionModel(List.of(first, second));

        model.selectAll();

        assertEquals(2, model.getSelectedLinters().size());
        assertTrue(model.getSelectedLinters().contains(first));
        assertTrue(model.getSelectedLinters().contains(second));
    }

    @Test
    void deselectAllClearsEverySelectedLinter() {
        LinterSelectionModel model = new LinterSelectionModel(List.of(new FirstTestLinter(), new SecondTestLinter()));
        model.selectAll();

        model.deselectAll();

        assertTrue(model.getSelectedLinters().isEmpty());
    }

    @Test
    void getSelectedLintersReturnsOnlyCheckedLinters() {
        FirstTestLinter first = new FirstTestLinter();
        SecondTestLinter second = new SecondTestLinter();
        LinterSelectionModel model = new LinterSelectionModel(List.of(first, second));

        List<JCheckBox> checkBoxes = model.getCheckBoxesInDisplayOrder();
        checkBoxes.get(0).setSelected(true);

        assertIterableEquals(List.of(first), model.getSelectedLinters());
    }

    private static final class FirstTestLinter implements Linter {
        @Override
        public String lint(List<File> files) {
            return "";
        }
    }

    private static final class SecondTestLinter implements Linter {
        @Override
        public String lint(List<File> files) {
            return "";
        }
    }
}