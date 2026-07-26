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
 *  2. **Meaning (MIDI)** — the score it produces actually sounds like the score that was
 *     written (docs/AUDIO_DEFECT_PLAN.md §4).
 *  3. **Meaning (claim vs sound)** — the pitches the exercise claims (`expectPitch`) are
 *     the pitches that sound, and every note stays inside the declared `fretRange`
 *     (DESIGN.md §3.4, docs/PHASE2_GUITAR_PLAN.md §3).
 *
 * The second group exists because syntax alone proved insufficient: `-.2` parses fine, as
 * a tie to *string 2* rather than the intended "tie of duration 2". With no note on
 * string 2 to tie to, alphaTab sounded a phantom B3 that droned for eleven beats under
 * the rest of the exercise. The checks mirror ExerciseMidiSemanticsTest in :app, which is
 * the CI gatekeeper; this tool is the fast local loop.
 *
 * Pass `--pitches` to also print each note's fingering and pitch, which is the quickest
 * way to see what a draft exercise actually sounds like while writing it.
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
        boolean withPitchDump = false;
        for (String arg : args) {
            if (arg.equals("--pitches")) {
                withPitchDump = true;
            } else {
                files.add(Path.of(arg));
            }
        }

        int passed = 0;
        int failed = 0;

        for (Path file : files) {
            String label = file.toString();
            try {
                Map<String, Object> doc = loadYaml(file);
                String alphatex = extractAlphatex(doc);
                Score score = parse(alphatex);
                int barCount = (int) score.getMasterBars().getLength();

                List<String> problems = new ArrayList<>();
                problems.addAll(checkTies(alphatex));
                problems.addAll(checkNoOverlappingNotes(score));
                problems.addAll(checkBarDurations(score));
                problems.addAll(checkFingering(score, doc));

                if (withPitchDump) {
                    dumpPitches(score);
                }

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

    /**
     * The third verification layer (DESIGN.md §3.4): does the exercise sound what it says
     * it sounds?
     *
     * alphaTex derives pitch from the fingering, so those two can never contradict each
     * other — but a `3.5` mistyped as `3.4` parses, sounds a clean single note, and leaves
     * the prose calling F3 "ド". The only way to catch that is to make the content state
     * its claim (`expectPitch`) and check the claim against the score. `fretRange` is the
     * same idea for position: declared, so a stray 7th fret fails instead of widening.
     */
    @SuppressWarnings("unchecked")
    private static List<String> checkFingering(Score score, Map<String, Object> doc) {
        List<String> problems = new ArrayList<>();
        var bars = score.getTracks().get(0).getStaves().get(0).getBars();

        Object rangeObject = doc.get("fretRange");
        if (rangeObject instanceof Map) {
            Map<String, Object> range = (Map<String, Object>) rangeObject;
            int min = ((Number) range.get("min")).intValue();
            int max = ((Number) range.get("max")).intValue();
            for (int b = 0; b < bars.getLength(); b++) {
                var beats = bars.get(b).getVoices().get(0).getBeats();
                for (int bi = 0; bi < beats.getLength(); bi++) {
                    var notes = beats.get(bi).getNotes();
                    for (int ni = 0; ni < notes.getLength(); ni++) {
                        Fingering n = describe(notes.get(ni));
                        if (n.fret() < min || n.fret() > max) {
                            problems.add(String.format(
                                    "%d小節目%d拍目が宣言した範囲外です (%d弦%dフレット、fretRange %d..%d)",
                                    b + 1, bi + 1, n.string(), n.fret(), min, max));
                        }
                    }
                }
            }
        }

        Object focusObject = doc.get("focusElements");
        if (!(focusObject instanceof List)) {
            return problems;
        }
        for (Object element : (List<Object>) focusObject) {
            if (!(element instanceof Map)) {
                continue;
            }
            Map<String, Object> focus = (Map<String, Object>) element;
            Object expect = focus.get("expectPitch");
            if (expect == null) {
                continue;
            }
            int barIndex = ((Number) focus.getOrDefault("barIndex", 0)).intValue();
            int beatIndex = ((Number) focus.getOrDefault("beatIndex", 0)).intValue();
            Object noteIndexObject = focus.get("noteIndex");
            String where = String.format("bar=%d beat=%d note=%s", barIndex, beatIndex, noteIndexObject);

            Integer expected = parsePitch(String.valueOf(expect));
            if (expected == null) {
                problems.add("expectPitch '" + expect + "' が音名として読めません (" + where + ")");
                continue;
            }
            if (noteIndexObject == null) {
                problems.add("expectPitch があるのに noteIndex が null です (" + where + ")");
                continue;
            }
            int noteIndex = ((Number) noteIndexObject).intValue();

            if (barIndex >= bars.getLength()) {
                problems.add("focusElements が存在しない小節を指しています (" + where + ")");
                continue;
            }
            var beats = bars.get(barIndex).getVoices().get(0).getBeats();
            if (beatIndex >= beats.getLength()) {
                problems.add("focusElements が存在しない拍を指しています (" + where + ")");
                continue;
            }
            var notes = beats.get(beatIndex).getNotes();
            if (noteIndex >= notes.getLength()) {
                problems.add("focusElements が存在しない音符を指しています (" + where + ")");
                continue;
            }

            Fingering actual = describe(notes.get(noteIndex));
            if (actual.realValue() != expected) {
                problems.add(String.format(
                        "%d小節目%d拍目は %s と書かれていますが、実際に鳴るのは %s です "
                                + "(%d弦%dフレット。%s)",
                        barIndex + 1, beatIndex + 1, pitchName(expected), pitchName(actual.realValue()),
                        actual.string(), actual.fret(), where));
            }
        }
        return problems;
    }

    /** Prints every note's fingering and pitch — the fast way to see what a draft sounds like. */
    private static void dumpPitches(Score score) {
        var bars = score.getTracks().get(0).getStaves().get(0).getBars();
        for (int b = 0; b < bars.getLength(); b++) {
            var beats = bars.get(b).getVoices().get(0).getBeats();
            for (int bi = 0; bi < beats.getLength(); bi++) {
                var notes = beats.get(bi).getNotes();
                for (int ni = 0; ni < notes.getLength(); ni++) {
                    Fingering n = describe(notes.get(ni));
                    System.out.printf("      bar=%d beat=%d note=%d  %d弦%dフレット  %s (記譜 %s)%n",
                            b, bi, ni, n.string(), n.fret(),
                            pitchName(n.realValue()), pitchName(n.displayValue()));
                }
            }
        }
    }

    /** One note, with the string number already flipped to the guitarist's numbering. */
    private record Fingering(int string, int fret, int realValue, int displayValue) {}

    /**
     * alphaTab numbers strings the opposite way round from alphaTex and from guitarists:
     * its {@code Note.string == 1} is the *lowest*-sounding string, where `0.6.4` in
     * alphaTex and "the 6th string" in conversation both mean that same low E. Everything
     * outside alphaTab's own model speaks the guitarist's numbering, so the flip happens
     * here, once. (`:app` does the same flip in ScoreLocator.)
     */
    private static Fingering describe(alphaTab.model.Note note) {
        int stringCount = (int) note.getBeat().getVoice().getBar().getStaff().getTuning().getLength();
        return new Fingering(
                stringCount + 1 - (int) note.getString(),
                (int) note.getFret(),
                (int) note.getRealValue(),
                (int) note.getDisplayValue());
    }

    private static final String[] PITCH_CLASSES =
            {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};

    private static final Pattern SCIENTIFIC = Pattern.compile("^([A-Ga-g])([#♯b♭]?)(-?\\d+)$");

    private static String pitchName(int midi) {
        return PITCH_CLASSES[Math.floorMod(midi, 12)] + (Math.floorDiv(midi, 12) - 1);
    }

    /** Scientific pitch notation -> MIDI, or null. Mirrors NoteNames.toMidi in `:core`. */
    private static Integer parsePitch(String name) {
        Matcher m = SCIENTIFIC.matcher(name.trim());
        if (!m.matches()) {
            return null;
        }
        int base = switch (Character.toUpperCase(m.group(1).charAt(0))) {
            case 'C' -> 0;
            case 'D' -> 2;
            case 'E' -> 4;
            case 'F' -> 5;
            case 'G' -> 7;
            case 'A' -> 9;
            case 'B' -> 11;
            default -> -1;
        };
        if (base < 0) {
            return null;
        }
        int accidental = switch (m.group(2)) {
            case "#", "♯" -> 1;
            case "b", "♭" -> -1;
            default -> 0;
        };
        return (Integer.parseInt(m.group(3)) + 1) * 12 + base + accidental;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> loadYaml(Path file) throws IOException {
        String content = Files.readString(file);
        Object loaded = new Yaml().load(content);
        if (!(loaded instanceof Map)) {
            throw new IllegalArgumentException("YAML root is not a mapping");
        }
        return (Map<String, Object>) loaded;
    }

    private static String extractAlphatex(Map<String, Object> map) {
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
