import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.yaml.snakeyaml.Yaml;

import alphaTab.Settings;
import alphaTab.importer.AlphaTexImporter;
import alphaTab.midi.ControllerType;
import alphaTab.midi.IMidiFileHandler;
import alphaTab.midi.MidiFileGenerator;
import alphaTab.model.Bar;
import alphaTab.model.Score;

/**
 * Local alphaTex validation tool (docs/IMPLEMENTATION_PLAN.md §6.5).
 *
 * For each YAML file given on the command line, extracts the top-level "alphatex" field
 * and checks it two ways:
 *
 *  1. **Syntax** — it parses with alphaTab's AlphaTexImporter (the same parser :app uses).
 *  2. **Meaning** — the score it produces actually sounds like the score that was written
 *     (docs/AUDIO_DEFECT_PLAN.md §4).
 *
 * The second group exists because syntax alone proved insufficient: `-.2` parses fine, as
 * a tie to *string 2* rather than the intended "tie of duration 2". With no note on
 * string 2 to tie to, alphaTab sounded a phantom B3 that droned for eleven beats under
 * the rest of the exercise. The checks mirror ExerciseMidiSemanticsTest in :app, which is
 * the CI gatekeeper; this tool is the fast local loop.
 *
 * Reports per-file results and exits non-zero if any file failed.
 */
public final class TexCheck {

    /** `フレット.弦.音価` (a note) or `-.弦[.音価]` (a tie). */
    private static final Pattern NOTE_OR_TIE =
            Pattern.compile("(?<![\\d.])(?:(\\d+)\\.([1-6])\\.(\\d+)|-\\.(\\d+))");

    private record Note(double start, double length, int key) {}

    /** Collects the notes MidiFileGenerator emits; other MIDI events are irrelevant here. */
    private static final class NoteCollector implements IMidiFileHandler {
        final List<Note> notes = new ArrayList<>();

        public void addNote(double track, double start, double length, double key, double velocity, double channel) {
            notes.add(new Note(start, length, (int) key));
        }

        public void addTimeSignature(double a, double b, double c) {}
        public void addRest(double a, double b, double c) {}
        public void addControlChange(double a, double b, double c, ControllerType d, double e) {}
        public void addProgramChange(double a, double b, double c, double d) {}
        public void addTempo(double a, double b) {}
        public void addNoteBend(double a, double b, double c, double d, double e) {}
        public void addBend(double a, double b, double c, double d) {}
        public void finishTrack(double a, double b) {}
        public void addTickShift(double a) {}
    }

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

                List<String> problems = new ArrayList<>();
                problems.addAll(checkTies(alphatex));
                problems.addAll(checkNoOverlappingNotes(score));
                problems.addAll(checkBarDurations(score));

                if (problems.isEmpty()) {
                    System.out.println("PASS  " + label + "  (" + barCount + " bar(s))");
                    passed++;
                } else {
                    System.out.println("FAIL  " + label + "  (" + barCount + " bar(s))");
                    for (String problem : problems) {
                        System.out.println("      " + problem);
                    }
                    failed++;
                }
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

    /**
     * Every `-.<string>` must name the same string as the note before it, otherwise the
     * tie has nothing to extend and alphaTab starts a phantom note on that string instead.
     */
    private static List<String> checkTies(String alphatex) {
        List<String> problems = new ArrayList<>();
        Matcher m = NOTE_OR_TIE.matcher(alphatex);
        Integer previousString = null;

        while (m.find()) {
            if (m.group(2) != null) {
                previousString = Integer.parseInt(m.group(2));
                continue;
            }
            int tie = Integer.parseInt(m.group(4));
            if (previousString == null || tie != previousString) {
                problems.add("タイ `-." + tie + "` の相手がいません（直前の音は"
                        + (previousString == null ? "なし" : previousString + "弦")
                        + "）。alphaTex のタイは `-.<弦>` です");
            }
        }
        return problems;
    }

    /**
     * All bundled exercises are single-voice and single-line (1譜例1概念), so two notes
     * sounding at the same time means something produced a note the score never asked for.
     */
    private static List<String> checkNoOverlappingNotes(Score score) {
        NoteCollector collector = new NoteCollector();
        new MidiFileGenerator(score, new Settings(), collector).generate();

        List<Note> notes = new ArrayList<>(collector.notes);
        notes.sort(Comparator.comparingDouble(Note::start));

        List<String> problems = new ArrayList<>();
        for (int i = 0; i < notes.size(); i++) {
            Note a = notes.get(i);
            for (int j = i + 1; j < notes.size(); j++) {
                Note b = notes.get(j);
                if (b.start() >= a.start() + a.length()) {
                    break;
                }
                problems.add(String.format(
                        "音が重なっています (key=%d start=%d len=%d と key=%d start=%d)",
                        a.key(), (int) a.start(), (int) a.length(), b.key(), (int) b.start()));
            }
        }
        return problems;
    }

    /** A bar whose beats do not add up to its time signature is a miscount. */
    private static List<String> checkBarDurations(Score score) {
        List<String> problems = new ArrayList<>();
        var staves = score.getTracks().get(0).getStaves();

        for (int s = 0; s < staves.getLength(); s++) {
            var bars = staves.get(s).getBars();
            for (int b = 0; b < bars.getLength(); b++) {
                Bar bar = bars.get(b);
                double expected = score.getMasterBars().get(b).calculateDuration(true);
                double actual = bar.getVoices().get(0).calculateDuration();
                if (actual != expected) {
                    problems.add(String.format(
                            "%d小節目の長さが拍子と合いません (期待 %d ticks / 実際 %d ticks)",
                            b + 1, (int) expected, (int) actual));
                }
            }
        }
        return problems;
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
