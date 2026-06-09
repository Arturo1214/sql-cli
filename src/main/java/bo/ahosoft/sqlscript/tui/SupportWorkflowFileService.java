package bo.ahosoft.sqlscript.tui;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public final class SupportWorkflowFileService {

    public LoadedSqlFile readSqlFile(String typedPath) {
        File file = normalize(typedPath);
        validateSqlFile(file);
        try {
            return new LoadedSqlFile(file, new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new WorkflowFileException("Could not read SQL file: " + ex.getMessage(), ex);
        }
    }

    public File validateExportTarget(String typedPath, boolean overwrite) {
        File target = normalize(typedPath);
        File parent = target.getAbsoluteFile().getParentFile();
        if (parent == null || !parent.exists()) {
            throw new WorkflowFileException("Parent directory does not exist: " + (parent == null ? typedPath : parent.getPath()));
        }
        if (!parent.isDirectory()) {
            throw new WorkflowFileException("Parent path is not a directory: " + parent.getPath());
        }
        if (!parent.canWrite()) {
            throw new WorkflowFileException("Parent directory is not writable: " + parent.getPath());
        }
        if (target.exists()) {
            if (target.isDirectory()) {
                throw new WorkflowFileException("Target path is a directory: " + target.getPath());
            }
            if (!overwrite) {
                throw new WorkflowFileException("Target file already exists: " + target.getPath());
            }
            if (!target.canWrite()) {
                throw new WorkflowFileException("Target file is not writable: " + target.getPath());
            }
        }
        try {
            return target.getCanonicalFile();
        } catch (IOException ex) {
            throw new WorkflowFileException("Could not resolve export path: " + ex.getMessage(), ex);
        }
    }

    private static File normalize(String typedPath) {
        if (typedPath == null || typedPath.trim().isEmpty()) {
            throw new WorkflowFileException("Path is required");
        }
        return new File(typedPath.trim());
    }

    private static void validateSqlFile(File file) {
        if (!file.exists()) {
            throw new WorkflowFileException("File does not exist: " + file.getPath());
        }
        if (file.isDirectory()) {
            throw new WorkflowFileException("Path is a directory: " + file.getPath());
        }
        if (!file.getName().toLowerCase().endsWith(".sql")) {
            throw new WorkflowFileException("Only .sql files are supported: " + file.getPath());
        }
        if (!file.canRead()) {
            throw new WorkflowFileException("File is not readable: " + file.getPath());
        }
    }

    public static final class LoadedSqlFile {

        private final File file;
        private final String content;

        LoadedSqlFile(File file, String content) {
            this.file = file;
            this.content = content == null ? "" : content;
        }

        public File file() {
            return file;
        }

        public String content() {
            return content;
        }
    }

    public static final class WorkflowFileException extends RuntimeException {

        public WorkflowFileException(String message) {
            super(message);
        }

        public WorkflowFileException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
