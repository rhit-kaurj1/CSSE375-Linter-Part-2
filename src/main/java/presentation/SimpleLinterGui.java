package presentation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import datastorage.ConfigLoader;
import domain.Linter;
import domain.LinterConfig;

public class SimpleLinterGui extends JFrame {
    private static final String DEFAULT_CONFIG_PATH = "linter.properties";

    private final FileSelectionModel fileSelectionModel;
    private final LinterSelectionModel linterSelectionModel;
    private final SimpleLinterGuiView view;
    private final LintRunSummaryFormatter summaryFormatter;
    private final LintRunCoordinator lintRunCoordinator;
    private final SimpleLinterGuiUiSupport uiSupport;

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

        this.linterFactory = new LinterFactory();
        this.linterRunner = new LinterRunner();
        this.configLoader = new ConfigLoader();
        this.fileSelectionModel = new FileSelectionModel();
        this.summaryFormatter = new LintRunSummaryFormatter();
        this.lintRunCoordinator = new LintRunCoordinator(this.linterRunner, this.summaryFormatter);
        this.uiSupport = new SimpleLinterGuiUiSupport();

        LinterConfig config = this.configLoader.loadConfig(DEFAULT_CONFIG_PATH);
        LinterConfig runAllConfig = new LinterConfig(
                null,
                config.getTooManyParametersLimit(),
                config.getSrpLcomThreshold());
        List<Linter> allAvailableLinters = this.linterFactory.createLinters(runAllConfig);
        this.linterSelectionModel = new LinterSelectionModel(allAvailableLinters);
        this.view = new SimpleLinterGuiView(
                this,
                this.fileSelectionModel.getListComponent(),
                this.linterSelectionModel.getCheckBoxesInDisplayOrder());

        wireActions();
    }

    private void wireActions() {
        view.getAddFilesButton().addActionListener(e -> onAddFiles());
        view.getRemoveSelectedButton().addActionListener(e -> onRemoveSelected());
        view.getClearAllButton().addActionListener(e -> onClearAll());
        view.getRunLintersButton().addActionListener(e -> onRunLinters());
        view.getSaveOutputButton().addActionListener(e -> onSaveOutput());
        view.getSelectAllLintersButton().addActionListener(e -> onSelectAllLinters());
        view.getDeselectAllLintersButton().addActionListener(e -> onDeselectAllLinters());
    }

    private void onSelectAllLinters() {
        linterSelectionModel.selectAll();
    }

    private void onDeselectAllLinters() {
        linterSelectionModel.deselectAll();
    }

    private void onAddFiles() {
        JFileChooser chooser = uiSupport.createAddFilesChooser();

        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        fileSelectionModel.addFiles(chooser.getSelectedFiles());
        uiSupport.showSelectedFilesStatus(view, fileSelectionModel.size());
    }

    private void onRemoveSelected() {
        fileSelectionModel.removeSelected();
        uiSupport.showSelectedFilesStatus(view, fileSelectionModel.size());
    }

    private void onClearAll() {
        fileSelectionModel.clear();
        view.getStatusLabel().setText("Ready");
    }

    private void onRunLinters() {
        LintRunCoordinator.Preparation preparation = lintRunCoordinator.prepare(
                fileSelectionModel.getAllFiles(),
                linterSelectionModel.getSelectedLinters());

        if (preparation.getStatus() == LintRunCoordinator.PreparationStatus.NO_FILES) {
            uiSupport.showWarning(this, "Please add at least one file.", "No Files Selected");
            return;
        }

        if (preparation.getStatus() == LintRunCoordinator.PreparationStatus.NO_LINTERS) {
            uiSupport.showWarning(this, "Please select at least one linter.", "No Linters Selected");
            return;
        }

        if (preparation.getStatus() == LintRunCoordinator.PreparationStatus.NO_READABLE_FILES) {
            view.getResultArea().setText(lintRunCoordinator.formatNoReadableFilesMessage(preparation));
            return;
        }

        runPreparedLint(preparation);
    }

    private void runPreparedLint(LintRunCoordinator.Preparation preparation) {
        view.setRunningState(true);
        view.getResultArea().setText("Running selected linters...\n");

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                return lintRunCoordinator.run(preparation);
            }

            @Override
            protected void done() {
                try {
                    view.getResultArea().setText(get());
                    view.getStatusLabel().setText("Completed");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    view.getResultArea().setText("Lint run interrupted: " + ex.getMessage());
                    view.getStatusLabel().setText("Interrupted");
                } catch (ExecutionException ex) {
                    view.getResultArea().setText("Lint run failed: " + ex.getMessage());
                    view.getStatusLabel().setText("Failed");
                } finally {
                    view.setRunningState(false);
                }
            }
        };
        worker.execute();
    }

    private void onSaveOutput() {
        String output = view.getResultArea().getText();
        if (output == null || output.trim().isEmpty()) {
            uiSupport.showInfo(this, "No output to save.", "Nothing to Save");
            return;
        }

        JFileChooser chooser = uiSupport.createSaveOutputChooser();

        int result = chooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        Path outputPath = chooser.getSelectedFile().toPath();
        try {
            Files.writeString(outputPath, output, StandardCharsets.UTF_8);
            view.getStatusLabel().setText("Saved: " + outputPath.getFileName());
        } catch (IOException ex) {
            uiSupport.showError(this, "Could not save output: " + ex.getMessage(), "Save Failed");
        }
    }

}