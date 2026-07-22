import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

import alphaTab.Settings;
import alphaTab.importer.AlphaTexImporter;
import alphaTab.model.Score;

/**
 * Local alphaTex validation tool (docs/IMPLEMENTATION_PLAN.md §6.5).
 *
 * For each YAML file given on the command line, extracts the top-level
 * "alphatex" field and parses it with alphaTab's AlphaTexImporter (the same
 * parser :app uses at runtime). Reports success/failure per file and exits
 * non-zero if any file failed.
 */
public final class TexCheck {

    public static void main(String[] args) throws IOException {
        List<Path> files = new ArrayList<>();
        for (String arg : args) {
            files.add(Path.of(arg));
        }

        int passed = 0;
        int failed = 0;

        for (Path file : files) {
            String label = file.toString();
            try {
                String alphatex = extractAlphatex(file);
                Score score = parse(alphatex);
                int barCount = (int) score.getMasterBars().getLength();
                System.out.println("PASS  " + label + "  (" + barCount + " bar(s))");
                passed++;
            } catch (Throwable e) {
                // alphaTab's AlphaTabError (e.g. UnsupportedFormatError) extends
                // java.lang.Throwable directly rather than Exception, so this
                // must catch Throwable to report per-file failures instead of
                // aborting the whole run.
                System.out.println("FAIL  " + label);
                // alphaTab puts the human-readable line/column diagnostics
                // (AT2xx codes) in the *cause*'s toString(), not getMessage() -
                // e.g. AlphaTexErrorWithDiagnostics wrapped by an
                // UnsupportedFormatError whose own getMessage() is generic.
                for (Throwable t = e; t != null; t = t.getCause()) {
                    for (String line : t.toString().split("\n")) {
                        System.out.println("      " + line);
                    }
                }
                failed++;
            }
        }

        System.out.println();
        System.out.println(passed + " passed, " + failed + " failed");

        if (failed > 0) {
            System.exit(1);
        }
    }

    @SuppressWarnings("unchecked")
    private static String extractAlphatex(Path file) throws IOException {
        String content = Files.readString(file);
        Yaml yaml = new Yaml();
        Object loaded = yaml.load(content);
        if (!(loaded instanceof Map)) {
            throw new IllegalArgumentException("YAML root is not a mapping");
        }
        Map<String, Object> map = (Map<String, Object>) loaded;
        Object alphatex = map.get("alphatex");
        if (alphatex == null) {
            throw new IllegalArgumentException("no top-level 'alphatex' key");
        }
        String text = String.valueOf(alphatex);
        if (text.isBlank()) {
            throw new IllegalArgumentException("'alphatex' field is empty");
        }
        return text;
    }

    private static Score parse(String alphatex) {
        AlphaTexImporter importer = new AlphaTexImporter();
        importer.initFromString(alphatex, new Settings());
        return importer.readScore();
    }

    private TexCheck() {
    }
}
