package datastorage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class OutputExporter {

    public void export(Path outputPath, String outputText) throws IOException {
        String textToWrite = outputText == null ? "" : outputText;
        Files.writeString(outputPath, textToWrite, StandardCharsets.UTF_8);
    }
}