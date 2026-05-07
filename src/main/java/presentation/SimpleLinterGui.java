package presentation;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.HyperlinkEvent;

import datastorage.ConfigLoader;
import datastorage.OutputExporter;
import domain.Linter;
import domain.LinterConfig;

public class SimpleLinterGui extends JFrame {
    private static final String DEFAULT_CONFIG_PATH = "linter.properties";

    private final FileSelectionModel fileSelectionModel;
    private final LinterSelectionModel linterSelectionModel;
    private final SimpleLinterGuiView view;
    private final LintRunSummaryFormatter summaryFormatter;
    private final LintRunCoordinator lintRunCoordinator;
    private final OutputExporter outputExporter;
    private final SimpleLinterGuiUiSupport uiSupport;

    private final LinterFactory linterFactory;
    private final LinterRunner linterRunner;
    private final ConfigLoader configLoader;
    private final LintResultHtmlFormatter resultHtmlFormatter;
    private final SourceFileNavigator sourceFileNavigator;

    private String currentRawOutput;

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
        this.outputExporter = new OutputExporter();
        this.uiSupport = new SimpleLinterGuiUiSupport();
        this.resultHtmlFormatter = new LintResultHtmlFormatter();
        this.sourceFileNavigator = new SourceFileNavigator();
        this.currentRawOutput = "";

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
        view.getClearResultsAndLintersButton().addActionListener(e -> onClearResultsAndLinters());
        view.getSelectAllLintersButton().addActionListener(e -> onSelectAllLinters());
        view.getDeselectAllLintersButton().addActionListener(e -> onDeselectAllLinters());
        view.getResultArea().addHyperlinkListener(this::onResultLinkActivated);
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

        File[] selectedFiles = chooser.getSelectedFiles();

        // Validate files before proceeding
        java.util.List<String> validationErrors = uiSupport.validateSelectedFiles(selectedFiles);
        if (!validationErrors.isEmpty()) {
            String errorMessage = buildErrorMessage(validationErrors);
            uiSupport.showError(this, errorMessage, "Invalid File Selection");
            return;
        }

        if (!uiSupport.confirmFilePreview(this, java.util.List.of(selectedFiles))) {
            return;
        }

        fileSelectionModel.addFiles(selectedFiles);
        uiSupport.showSelectedFilesStatus(view, fileSelectionModel.size());
    }

    private String buildErrorMessage(java.util.List<String> errors) {
        StringBuilder message = new StringBuilder();
        message.append("Unable to add the following file(s):\n\n");
        for (String error : errors) {
            message.append("• ").append(error).append("\n");
        }
        return message.toString();
    }

    private void onRemoveSelected() {
        fileSelectionModel.removeSelected();
        uiSupport.showSelectedFilesStatus(view, fileSelectionModel.size());
    }

    private void onClearAll() {
        fileSelectionModel.clear();
        view.getStatusLabel().setText("Ready");
    }

    private void onClearResultsAndLinters() {
        currentRawOutput = "";
        view.setResultHtml(resultHtmlFormatter.toHtml("", List.of()));
        linterSelectionModel.deselectAll();
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
            String message = lintRunCoordinator.formatNoReadableFilesMessage(preparation);
            currentRawOutput = message;
            view.setResultHtml(resultHtmlFormatter.toHtml(message, preparation.getSkippedFiles()));
            return;
        }

        runPreparedLint(preparation);
    }

    private void runPreparedLint(LintRunCoordinator.Preparation preparation) {
        view.setRunningState(true);
        view.setResultHtml(resultHtmlFormatter.toHtml("Running selected linters...", preparation.getReadableFiles()));

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                return lintRunCoordinator.run(preparation, completed -> {
                    // Progress bar removed; no per-linter UI progress updates.
                });
            }

            @Override
            protected void done() {
                try {
                    String result = get();
                    currentRawOutput = result;
                    view.setResultHtml(resultHtmlFormatter.toHtml(result, preparation.getReadableFiles()));
                    view.getStatusLabel().setText("Completed");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    currentRawOutput = "Lint run interrupted: " + ex.getMessage();
                    view.setResultHtml(resultHtmlFormatter.toHtml(currentRawOutput, List.of()));
                    view.getStatusLabel().setText("Interrupted");
                } catch (ExecutionException ex) {
                    currentRawOutput = "Lint run failed: " + ex.getMessage();
                    view.setResultHtml(resultHtmlFormatter.toHtml(currentRawOutput, List.of()));
                    view.getStatusLabel().setText("Failed");
                } finally {
                    view.setRunningState(false);
                }
            }
        };
        worker.execute();
    }

    private void onSaveOutput() {
        String output = currentRawOutput;
        JFileChooser chooser = uiSupport.createSaveOutputChooser();

        int result = chooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        Path outputPath = chooser.getSelectedFile().toPath();
        try {
            outputExporter.export(outputPath, output);
            view.getStatusLabel().setText("Exported: " + outputPath.getFileName());
            uiSupport.showInfo(this, "Output exported successfully.", "Export Complete");
        } catch (IOException ex) {
            uiSupport.showError(this, "Could not export output: " + ex.getMessage(), "Export Failed");
        }
    }

    private void onResultLinkActivated(HyperlinkEvent event) {
        if (event.getEventType() != HyperlinkEvent.EventType.ACTIVATED) {
            return;
        }

        String description = event.getDescription();
        if (description == null || !description.startsWith(LintResultHtmlFormatter.LINK_PREFIX)) {
            return;
        }

        try {
            String filePath = extractLinkValue(description, LintResultHtmlFormatter.FILE_PARAMETER);
            String lineText = extractLinkValue(description, LintResultHtmlFormatter.LINE_PARAMETER);
            File targetFile = new File(URLDecoder.decode(filePath, StandardCharsets.UTF_8));
            int lineNumber = Integer.parseInt(URLDecoder.decode(lineText, StandardCharsets.UTF_8));
            sourceFileNavigator.openSourceFile(this, targetFile, lineNumber);
        } catch (IOException | IllegalArgumentException ex) {
            uiSupport.showError(this, ex.getMessage(), "Cannot Open Source File");
        }
    }

    private String extractLinkValue(String description, String parameterName) {
        String query = description.substring(LintResultHtmlFormatter.LINK_PREFIX.length());
        String[] parts = query.split("&");
        String prefix = parameterName + "=";

        for (String part : parts) {
            if (part.startsWith(prefix)) {
                return part.substring(prefix.length());
            }
        }

        throw new IllegalArgumentException("Missing error location information.");
    }

}