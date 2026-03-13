package datastorage;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class FileLoader {
    // CODE SMELL: Hidden mutable state for last processed file; this is not needed for loading behavior.
    // Refactor direction: Make FileLoader stateless and remove getter coupling to internal parsing state.
    private File file;

    public List<File> loadFiles(String input) {
        // CODE SMELL: Parsing raw comma-delimited input here mixes UI parsing with file system concerns.
        // Refactor direction: Separate input parsing from recursive file discovery.
        List<File> files = new ArrayList<>();
        if (input == null || input.trim().isEmpty()) {
            return files;
        }

        String[] paths = input.split(",");
        for (String path : paths) {
            String trimmed = path.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            this.file = new File(trimmed);
            if (this.file.exists()) {
                files.add(this.file);
            } else {
                System.out.println("Skipping missing path: " + this.file.getPath());
            }
        }

        return expandInputFiles(files);
    }

    public File getFile() {
        // CODE SMELL: Potential dead code/feature envy accessor (currently no production caller).
        // Refactor direction: Remove if unused, or replace with explicit parse result object.
        return file;
    }

    private List<File> expandInputFiles(List<File> requestedPaths) {
        List<File> files = new ArrayList<>();
        Set<String> seenPaths = new LinkedHashSet<>();

        for (File path : requestedPaths) {
            collectFiles(path, files, seenPaths);
        }

        return files;
    }

    private void collectFiles(File path, List<File> files, Set<String> seenPaths) {
        // CODE SMELL: Recursive traversal logic is embedded directly, limiting reuse for future selectors.
        // Refactor direction: Extract file-walk strategy/service that can be reused by UI folder selectors.
        if (path.isFile()) {
            String absolutePath = path.getAbsolutePath();
            if (seenPaths.add(absolutePath)) {
                files.add(path);
            }
            return;
        }

        if (!path.isDirectory()) {
            return;
        }

        File[] children = path.listFiles();
        if (children == null) {
            return;
        }

        Arrays.sort(children, (left, right) -> left.getName().compareToIgnoreCase(right.getName()));
        for (File child : children) {
            collectFiles(child, files, seenPaths);
        }
    }
}
