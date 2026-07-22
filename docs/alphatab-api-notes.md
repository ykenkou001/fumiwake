# alphaTab 1.8.4 Android API ノート（AAR javap 抽出）

> alphatab.net は開発環境から到達不可のため、AAR (net.alphatab:alphaTab:1.8.4) から
> javap で抽出した API 情報を一次資料とする。実装時はこのシグネチャに従うこと。

- minSdk: 26 / AAR 同梱 assets: Bravura.otf, sonivox.sf2, sonivox.sf3（フォント・音源は追加不要）
- res/layout/alphatab_view.xml が存在（AlphaTabView の内部レイアウト）
- 検証済み: AlphaTexImporter は純 JVM で動作（unit test で alphaTex 構文検証可能）
- 検証済み alphaTex 例: '\\title "T" \\tempo 80 . \\ts 4 4 3.3.4 5.3.4 r.4 3.3.4{d} 5.3.8 | 3.3.2 -.2 |'
  （タイは '-.duration'、付点は '{d}'、休符は 'r.duration'、音符は 'fret.string.duration'）

```
// alphaTab.AlphaTabView
public final class alphaTab.AlphaTabView extends android.widget.RelativeLayout {
  public final java.lang.Iterable<alphaTab.model.Track> getTracks();
  public final void setTracks(java.lang.Iterable<alphaTab.model.Track>);
  public final alphaTab.Settings getSettings();
  public final void setSettings(alphaTab.Settings);
  public final alphaTab.IEventEmitter getSettingsChanged();
  public final void setSettingsChanged(alphaTab.IEventEmitter);
  public final int getBarCursorFillColor();
  public final void setBarCursorFillColor(int);
  public final int getBeatCursorFillColor();
  public final void setBeatCursorFillColor(int);
  public final int getSelectionFillColor();
  public final void setSelectionFillColor(int);
  public final alphaTab.AlphaTabApiBase<alphaTab.AlphaTabView> getApi();
  public alphaTab.AlphaTabView(android.content.Context, android.util.AttributeSet);
  public alphaTab.AlphaTabView(android.content.Context, android.util.AttributeSet, int);
  protected void onDetachedFromWindow();
  public final void renderTracks();
}
```

```
// alphaTab.AlphaTabApiBase
public class alphaTab.AlphaTabApiBase<TSettings> {
  public alphaTab.Settings settings;
  public final double getMidiTickShift();
  public final alphaTab.PlayerMode getActualPlayerMode();
  public final alphaTab.platform.IUiFacade<TSettings> getUiFacade();
  public final void setUiFacade(alphaTab.platform.IUiFacade<TSettings>);
  public final alphaTab.platform.IContainer getContainer();
  public final void setContainer(alphaTab.platform.IContainer);
  public final alphaTab.rendering.IScoreRenderer getRenderer();
  public final alphaTab.model.Score getScore();
  public final alphaTab.Settings getSettings();
  public final void setSettings(alphaTab.Settings);
  public final alphaTab.collections.List<alphaTab.model.Track> getTracks();
  public final alphaTab.platform.IContainer getCanvasElement();
  public final void setCanvasElement(alphaTab.platform.IContainer);
  public alphaTab.AlphaTabApiBase(alphaTab.platform.IUiFacade<TSettings>, TSettings);
  public final void destroy();
  public final void updateSettings();
  public final boolean load(java.lang.Object, alphaTab.collections.DoubleList);
  public static boolean load$default(alphaTab.AlphaTabApiBase, java.lang.Object, alphaTab.collections.DoubleList, int, java.lang.Object);
  public final void renderScore(alphaTab.model.Score, alphaTab.collections.DoubleList, alphaTab.rendering.RenderHints);
  public static void renderScore$default(alphaTab.AlphaTabApiBase, alphaTab.model.Score, alphaTab.collections.DoubleList, alphaTab.rendering.RenderHints, int, java.lang.Object);
  public final void renderTracks(alphaTab.collections.List<alphaTab.model.Track>, alphaTab.rendering.RenderHints);
  public static void renderTracks$default(alphaTab.AlphaTabApiBase, alphaTab.collections.List, alphaTab.rendering.RenderHints, int, java.lang.Object);
  public final void triggerResize$android_release();
  public final void tex(java.lang.String, alphaTab.collections.DoubleList);
  public static void tex$default(alphaTab.AlphaTabApiBase, java.lang.String, alphaTab.collections.DoubleList, int, java.lang.Object);
  public final boolean loadSoundFont(java.lang.Object, boolean);
  public static boolean loadSoundFont$default(alphaTab.AlphaTabApiBase, java.lang.Object, boolean, int, java.lang.Object);
  public final void resetSoundFonts();
  public final void render(alphaTab.rendering.RenderHints);
  public static void render$default(alphaTab.AlphaTabApiBase, alphaTab.rendering.RenderHints, int, java.lang.Object);
  public final alphaTab.ICursorHandler getCustomCursorHandler();
  public final void setCustomCursorHandler(alphaTab.ICursorHandler);
  public final alphaTab.IScrollHandler getCustomScrollHandler();
  public final void setCustomScrollHandler(alphaTab.IScrollHandler);
  public final alphaTab.midi.MidiTickLookup getTickCache();
  public final alphaTab.rendering.utils.BoundsLookup getBoundsLookup();
  public final alphaTab.synth.IAlphaSynth getPlayer();
  public final boolean isReadyForPlayback();
  public final alphaTab.synth.PlayerState getPlayerState();
  public final double getMasterVolume();
  public final void setMasterVolume(double);
  public final double getMetronomeVolume();
  public final void setMetronomeVolume(double);
  public final double getCountInVolume();
  public final void setCountInVolume(double);
  public final alphaTab.collections.List<alphaTab.midi.MidiEventType> getMidiEventsPlayedFilter();
  public final void setMidiEventsPlayedFilter(alphaTab.collections.List<alphaTab.midi.MidiEventType>);
  public final double getTickPosition();
  public final void setTickPosition(double);
  public final double getTimePosition();
  public final void setTimePosition(double);
  public final double getEndTick();
  public final double getEndTime();
  public final alphaTab.synth.PlaybackRange getPlaybackRange();
  public final void setPlaybackRange(alphaTab.synth.PlaybackRange);
  public final double getPlaybackSpeed();
  public final void setPlaybackSpeed(double);
  public final boolean isLooping();
```

```
// alphaTab.Settings
public final class alphaTab.Settings {
  public static final alphaTab.Settings$Companion Companion;
  public final alphaTab.CoreSettings getCore();
  public final void setCore(alphaTab.CoreSettings);
  public final alphaTab.DisplaySettings getDisplay();
  public final void setDisplay(alphaTab.DisplaySettings);
  public final alphaTab.NotationSettings getNotation();
  public final void setNotation(alphaTab.NotationSettings);
  public final alphaTab.ImporterSettings getImporter();
  public final void setImporter(alphaTab.ImporterSettings);
  public final alphaTab.PlayerSettings getPlayer();
  public final void setPlayer(alphaTab.PlayerSettings);
  public final alphaTab.ExporterSettings getExporter();
  public final void setExporter(alphaTab.ExporterSettings);
  public final void setSongBookModeSettings();
  public final void handleBackwardsCompatibility$android_release();
  public alphaTab.Settings();
  public static final alphaTab.Settings getSongBook();
  static {};
}
```

```
// alphaTab.CoreSettings
public final class alphaTab.CoreSettings {
  public alphaTab.CoreSettings();
  public final boolean getEnableLazyLoading();
  public final void setEnableLazyLoading(boolean);
  public final java.lang.String getEngine();
  public final void setEngine(java.lang.String);
  public final alphaTab.LogLevel getLogLevel();
  public final void setLogLevel(alphaTab.LogLevel);
  public final boolean getUseWorkers();
  public final void setUseWorkers(boolean);
  public final boolean getIncludeNoteBounds();
  public final void setIncludeNoteBounds(boolean);
}
```

```
// alphaTab.DisplaySettings
public final class alphaTab.DisplaySettings {
  public final double getScale();
  public final void setScale(double);
  public final double getStretchForce();
  public final void setStretchForce(double);
  public final alphaTab.LayoutMode getLayoutMode();
  public final void setLayoutMode(alphaTab.LayoutMode);
  public final alphaTab.StaveProfile getStaveProfile();
  public final void setStaveProfile(alphaTab.StaveProfile);
  public static void getStaveProfile$annotations();
  public final double getBarsPerRow();
  public final void setBarsPerRow(double);
  public final double getStartBar();
  public final void setStartBar(double);
  public final double getBarCount();
  public final void setBarCount(double);
  public final double getBarCountPerPartial();
  public final void setBarCountPerPartial(double);
  public final boolean getJustifyLastSystem();
  public final void setJustifyLastSystem(boolean);
  public final alphaTab.RenderingResources getResources();
  public final void setResources(alphaTab.RenderingResources);
  public final alphaTab.collections.DoubleList getPadding();
  public final void setPadding(alphaTab.collections.DoubleList);
  public final double getFirstSystemPaddingTop();
  public final void setFirstSystemPaddingTop(double);
  public final double getSystemPaddingTop();
  public final void setSystemPaddingTop(double);
  public final double getSystemPaddingBottom();
  public final void setSystemPaddingBottom(double);
  public final double getLastSystemPaddingBottom();
  public final void setLastSystemPaddingBottom(double);
  public final double getSystemLabelPaddingLeft();
  public final void setSystemLabelPaddingLeft(double);
  public final double getSystemLabelPaddingRight();
  public final void setSystemLabelPaddingRight(double);
  public final double getAccoladeBarPaddingRight();
  public final void setAccoladeBarPaddingRight(double);
  public final double getFirstNotationStaffPaddingTop();
  public final void setFirstNotationStaffPaddingTop(double);
  public final double getLastNotationStaffPaddingBottom();
  public final void setLastNotationStaffPaddingBottom(double);
  public final double getNotationStaffPaddingTop();
  public final void setNotationStaffPaddingTop(double);
  public final double getNotationStaffPaddingBottom();
  public final void setNotationStaffPaddingBottom(double);
  public final double getEffectStaffPaddingTop();
  public final void setEffectStaffPaddingTop(double);
  public static void getEffectStaffPaddingTop$annotations();
  public final double getEffectStaffPaddingBottom();
  public final void setEffectStaffPaddingBottom(double);
  public static void getEffectStaffPaddingBottom$annotations();
  public final double getFirstStaffPaddingLeft();
  public final void setFirstStaffPaddingLeft(double);
  public final double getStaffPaddingLeft();
  public final void setStaffPaddingLeft(double);
  public final double getEffectBandPaddingBottom();
  public final void setEffectBandPaddingBottom(double);
  public final double getTrackStaffPaddingBetween();
  public final void setTrackStaffPaddingBetween(double);
```

```
// alphaTab.PlayerSettings
public final class alphaTab.PlayerSettings {
  public final boolean getEnablePlayer();
  public final void setEnablePlayer(boolean);
  public static void getEnablePlayer$annotations();
  public final alphaTab.PlayerMode getPlayerMode();
  public final void setPlayerMode(alphaTab.PlayerMode);
  public final boolean getEnableCursor();
  public final void setEnableCursor(boolean);
  public final boolean getEnableAnimatedBeatCursor();
  public final void setEnableAnimatedBeatCursor(boolean);
  public final boolean getEnableElementHighlighting();
  public final void setEnableElementHighlighting(boolean);
  public final boolean getEnableUserInteraction();
  public final void setEnableUserInteraction(boolean);
  public final double getScrollOffsetX();
  public final void setScrollOffsetX(double);
  public final double getScrollOffsetY();
  public final void setScrollOffsetY(double);
  public final alphaTab.ScrollMode getScrollMode();
  public final void setScrollMode(alphaTab.ScrollMode);
  public final double getScrollSpeed();
  public final void setScrollSpeed(double);
  public final double getSongBookBendDuration();
  public final void setSongBookBendDuration(double);
  public final double getSongBookDipDuration();
  public final void setSongBookDipDuration(double);
  public final alphaTab.VibratoPlaybackSettings getVibrato();
  public final void setVibrato(alphaTab.VibratoPlaybackSettings);
  public final alphaTab.SlidePlaybackSettings getSlide();
  public final void setSlide(alphaTab.SlidePlaybackSettings);
  public final boolean getPlayTripletFeel();
  public final void setPlayTripletFeel(boolean);
  public final double getBufferTimeInMilliseconds();
  public final void setBufferTimeInMilliseconds(double);
  public alphaTab.PlayerSettings();
}
```

```
// alphaTab.PlayerMode
public final class alphaTab.PlayerMode extends java.lang.Enum<alphaTab.PlayerMode> implements alphaTab.core.IAlphaTabEnum {
  public static final alphaTab.PlayerMode$Companion Companion;
  public static final alphaTab.PlayerMode Disabled;
  public static final alphaTab.PlayerMode EnabledAutomatic;
  public static final alphaTab.PlayerMode EnabledSynthesizer;
  public static final alphaTab.PlayerMode EnabledBackingTrack;
  public static final alphaTab.PlayerMode EnabledExternalMedia;
  public int getValue();
  public static alphaTab.PlayerMode[] values();
  public static alphaTab.PlayerMode valueOf(java.lang.String);
  public static kotlin.enums.EnumEntries<alphaTab.PlayerMode> getEntries();
  public static final alphaTab.PlayerMode[] access$getValues$cp();
  static {};
}
```

```
// alphaTab.model.Score
public final class alphaTab.model.Score {
  public static final alphaTab.model.Score$Companion Companion;
  public final java.lang.String getAlbum();
  public final void setAlbum(java.lang.String);
  public final java.lang.String getArtist();
  public final void setArtist(java.lang.String);
  public final java.lang.String getCopyright();
  public final void setCopyright(java.lang.String);
  public final java.lang.String getInstructions();
  public final void setInstructions(java.lang.String);
  public final java.lang.String getMusic();
  public final void setMusic(java.lang.String);
  public final java.lang.String getNotices();
  public final void setNotices(java.lang.String);
  public final java.lang.String getSubTitle();
  public final void setSubTitle(java.lang.String);
  public final java.lang.String getTitle();
  public final void setTitle(java.lang.String);
  public final java.lang.String getWords();
  public final void setWords(java.lang.String);
  public final java.lang.String getTab();
  public final void setTab(java.lang.String);
  public final double getTempo();
  public final java.lang.String getTempoLabel();
  public final alphaTab.collections.List<alphaTab.model.MasterBar> getMasterBars();
  public final void setMasterBars(alphaTab.collections.List<alphaTab.model.MasterBar>);
  public final alphaTab.collections.List<alphaTab.model.Track> getTracks();
  public final void setTracks(alphaTab.collections.List<alphaTab.model.Track>);
  public final double getDefaultSystemsLayout();
  public final void setDefaultSystemsLayout(double);
  public final alphaTab.collections.DoubleList getSystemsLayout();
  public final void setSystemsLayout(alphaTab.collections.DoubleList);
  public final alphaTab.model.RenderStylesheet getStylesheet();
  public final void setStylesheet(alphaTab.model.RenderStylesheet);
  public final alphaTab.model.BackingTrack getBackingTrack();
  public final void setBackingTrack(alphaTab.model.BackingTrack);
  public final alphaTab.model.ScoreStyle getStyle();
  public final void setStyle(alphaTab.model.ScoreStyle);
  public final void rebuildRepeatGroups();
  public final void addMasterBar(alphaTab.model.MasterBar);
  public final void addTrack(alphaTab.model.Track);
  public final void finish(alphaTab.Settings);
  public final void applyFlatSyncPoints(alphaTab.collections.List<alphaTab.model.FlatSyncPoint>);
  public final alphaTab.collections.List<alphaTab.model.FlatSyncPoint> exportFlatSyncPoints();
  public alphaTab.model.Score();
  public static final void resetIds();
  static {};
}
```

```
// alphaTab.model.Track
public final class alphaTab.model.Track {
  public static final alphaTab.model.Track$Companion Companion;
  public alphaTab.model.Score score;
  public final double getIndex();
  public final void setIndex(double);
  public final alphaTab.model.Score getScore();
  public final void setScore(alphaTab.model.Score);
  public final alphaTab.collections.List<alphaTab.model.Staff> getStaves();
  public final void setStaves(alphaTab.collections.List<alphaTab.model.Staff>);
  public final alphaTab.model.PlaybackInformation getPlaybackInfo();
  public final void setPlaybackInfo(alphaTab.model.PlaybackInformation);
  public final alphaTab.model.Color getColor();
  public final void setColor(alphaTab.model.Color);
  public final java.lang.String getName();
  public final void setName(java.lang.String);
  public final boolean isVisibleOnMultiTrack();
  public final void setVisibleOnMultiTrack(boolean);
  public final java.lang.String getShortName();
  public final void setShortName(java.lang.String);
  public final double getDefaultSystemsLayout();
  public final void setDefaultSystemsLayout(double);
  public final alphaTab.collections.DoubleList getSystemsLayout();
  public final void setSystemsLayout(alphaTab.collections.DoubleList);
  public final alphaTab.core.ecmaScript.Set<java.lang.Double> getLineBreaks();
  public final void setLineBreaks(alphaTab.core.ecmaScript.Set<java.lang.Double>);
  public final boolean isPercussion();
  public final void addLineBreaks(double);
  public final alphaTab.collections.List<alphaTab.model.InstrumentArticulation> getPercussionArticulations();
  public final void setPercussionArticulations(alphaTab.collections.List<alphaTab.model.InstrumentArticulation>);
  public final alphaTab.model.TrackStyle getStyle();
  public final void setStyle(alphaTab.model.TrackStyle);
  public final void ensureStaveCount(double);
  public final void addStaff(alphaTab.model.Staff);
  public final void finish(alphaTab.Settings, alphaTab.collections.Map<java.lang.String, java.lang.Object>);
  public static void finish$default(alphaTab.model.Track, alphaTab.Settings, alphaTab.collections.Map, int, java.lang.Object);
  public final void applyLyrics(alphaTab.collections.List<alphaTab.model.Lyrics>);
  public alphaTab.model.Track();
  static {};
}
```

```
// alphaTab.model.Staff
public final class alphaTab.model.Staff {
  public static final alphaTab.model.Staff$Companion Companion;
  public alphaTab.model.Track track;
  public final double getIndex();
  public final void setIndex(double);
  public final alphaTab.model.Track getTrack();
  public final void setTrack(alphaTab.model.Track);
  public final alphaTab.collections.List<alphaTab.model.Bar> getBars();
  public final void setBars(alphaTab.collections.List<alphaTab.model.Bar>);
  public final alphaTab.collections.Map<java.lang.String, alphaTab.model.Chord> getChords();
  public final void setChords(alphaTab.collections.Map<java.lang.String, alphaTab.model.Chord>);
  public final double getCapo();
  public final void setCapo(double);
  public final double getTranspositionPitch();
  public final void setTranspositionPitch(double);
  public final double getDisplayTranspositionPitch();
  public final void setDisplayTranspositionPitch(double);
  public final alphaTab.model.Tuning getStringTuning();
  public final void setStringTuning(alphaTab.model.Tuning);
  public final alphaTab.collections.DoubleList getTuning();
  public final java.lang.String getTuningName();
  public final boolean isStringed();
  public final boolean getShowSlash();
  public final void setShowSlash(boolean);
  public final boolean getShowNumbered();
  public final void setShowNumbered(boolean);
  public final boolean getShowTablature();
  public final void setShowTablature(boolean);
  public final boolean getShowStandardNotation();
  public final void setShowStandardNotation(boolean);
  public final boolean isPercussion();
  public final void setPercussion(boolean);
  public final double getStandardNotationLineCount();
  public final void setStandardNotationLineCount(double);
  public final alphaTab.core.ecmaScript.Set<java.lang.Double> getFilledVoices();
  public final void finish(alphaTab.Settings, alphaTab.collections.Map<java.lang.String, java.lang.Object>);
  public static void finish$default(alphaTab.model.Staff, alphaTab.Settings, alphaTab.collections.Map, int, java.lang.Object);
  public final void addChord(java.lang.String, alphaTab.model.Chord);
  public final boolean hasChord(java.lang.String);
  public final alphaTab.model.Chord getChord(java.lang.String);
  public final void addBar(alphaTab.model.Bar);
  public alphaTab.model.Staff();
  public static final double getDefaultStandardNotationLineCount();
  public static final double access$getDefaultStandardNotationLineCount$cp();
  static {};
}
```

```
// alphaTab.model.Bar
public final class alphaTab.model.Bar {
  public static final alphaTab.model.Bar$Companion Companion;
  public alphaTab.model.Staff staff;
  public final double getId();
  public final void setId(double);
  public final double getIndex();
  public final void setIndex(double);
  public final alphaTab.model.Bar getNextBar();
  public final void setNextBar(alphaTab.model.Bar);
  public final alphaTab.model.Bar getPreviousBar();
  public final void setPreviousBar(alphaTab.model.Bar);
  public final alphaTab.model.Clef getClef();
  public final void setClef(alphaTab.model.Clef);
  public final alphaTab.model.Ottavia getClefOttava();
  public final void setClefOttava(alphaTab.model.Ottavia);
  public final alphaTab.model.Staff getStaff();
  public final void setStaff(alphaTab.model.Staff);
  public final alphaTab.collections.List<alphaTab.model.Voice> getVoices();
  public final void setVoices(alphaTab.collections.List<alphaTab.model.Voice>);
  public final alphaTab.model.SimileMark getSimileMark();
  public final void setSimileMark(alphaTab.model.SimileMark);
  public final boolean isMultiVoice();
  public final alphaTab.core.ecmaScript.Set<java.lang.Double> getFilledVoices();
  public final double getDisplayScale();
  public final void setDisplayScale(double);
  public final double getDisplayWidth();
  public final void setDisplayWidth(double);
  public final alphaTab.collections.List<alphaTab.model.SustainPedalMarker> getSustainPedals();
  public final void setSustainPedals(alphaTab.collections.List<alphaTab.model.SustainPedalMarker>);
  public final alphaTab.model.MasterBar getMasterBar();
  public final boolean isEmpty();
  public final boolean getHasChanges();
  public final boolean isRestOnly();
  public final alphaTab.model.BarLineStyle getBarLineLeft();
  public final void setBarLineLeft(alphaTab.model.BarLineStyle);
  public final alphaTab.model.BarLineStyle getBarLineRight();
  public final void setBarLineRight(alphaTab.model.BarLineStyle);
  public final alphaTab.model.KeySignature getKeySignature();
  public final void setKeySignature(alphaTab.model.KeySignature);
  public final alphaTab.model.KeySignatureType getKeySignatureType();
  public final void setKeySignatureType(alphaTab.model.KeySignatureType);
  public final alphaTab.model.BarNumberDisplay getBarNumberDisplay();
  public final void setBarNumberDisplay(alphaTab.model.BarNumberDisplay);
  public final alphaTab.model.Duration getShortestDuration$android_release();
  public final void setShortestDuration$android_release(alphaTab.model.Duration);
  public final alphaTab.model.BarLineStyle getActualBarLineLeft(boolean);
  public final alphaTab.model.BarLineStyle getActualBarLineRight();
  public final alphaTab.model.BarStyle getStyle();
  public final void setStyle(alphaTab.model.BarStyle);
  public final void addVoice(alphaTab.model.Voice);
  public final void finish(alphaTab.Settings, alphaTab.collections.Map<java.lang.String, java.lang.Object>);
  public static void finish$default(alphaTab.model.Bar, alphaTab.Settings, alphaTab.collections.Map, int, java.lang.Object);
  public final double calculateDuration();
  public alphaTab.model.Bar();
  public static final void resetIds$android_release();
  public static final void access$set_globalBarId$cp(double);
  static {};
}
```

```
// alphaTab.model.Voice
public final class alphaTab.model.Voice {
  public static final alphaTab.model.Voice$Companion Companion;
  public alphaTab.model.Bar bar;
  public final double getId();
  public final void setId(double);
  public final double getIndex();
  public final void setIndex(double);
  public final alphaTab.model.Bar getBar();
  public final void setBar(alphaTab.model.Bar);
  public final alphaTab.collections.List<alphaTab.model.Beat> getBeats();
  public final void setBeats(alphaTab.collections.List<alphaTab.model.Beat>);
  public final boolean isEmpty();
  public final alphaTab.model.VoiceStyle getStyle();
  public final void setStyle(alphaTab.model.VoiceStyle);
  public final void forceNonEmpty$android_release();
  public final boolean isRestOnly();
  public final alphaTab.model.Duration getShortestDuration$android_release();
  public final void setShortestDuration$android_release(alphaTab.model.Duration);
  public final void insertBeat(alphaTab.model.Beat, alphaTab.model.Beat);
  public final void addBeat(alphaTab.model.Beat);
  static void _chain$default(alphaTab.model.Voice, alphaTab.model.Beat, alphaTab.collections.Map, int, java.lang.Object);
  public final void addGraceBeat(alphaTab.model.Beat);
  public final alphaTab.model.Beat getBeatAtPlaybackStart(double);
  public final void finish(alphaTab.Settings, alphaTab.collections.Map<java.lang.String, java.lang.Object>);
  public static void finish$default(alphaTab.model.Voice, alphaTab.Settings, alphaTab.collections.Map, int, java.lang.Object);
  public final double calculateDuration();
  public alphaTab.model.Voice();
  public static final void resetIds$android_release();
  public static final void access$set_globalVoiceId$cp(double);
  static {};
}
```

```
// alphaTab.model.Beat
public final class alphaTab.model.Beat {
  public static final alphaTab.model.Beat$Companion Companion;
  public alphaTab.model.Voice voice;
  public final double getId();
  public final void setId(double);
  public final double getIndex();
  public final void setIndex(double);
  public final alphaTab.model.Beat getPreviousBeat();
  public final void setPreviousBeat(alphaTab.model.Beat);
  public final alphaTab.model.Beat getNextBeat();
  public final void setNextBeat(alphaTab.model.Beat);
  public final boolean isLastOfVoice();
  public final alphaTab.model.Voice getVoice();
  public final void setVoice(alphaTab.model.Voice);
  public final alphaTab.collections.List<alphaTab.model.Note> getNotes();
  public final void setNotes(alphaTab.collections.List<alphaTab.model.Note>);
  public final alphaTab.collections.DoubleObjectMap<alphaTab.model.Note> getNoteStringLookup();
  public final void setNoteStringLookup(alphaTab.collections.DoubleObjectMap<alphaTab.model.Note>);
  public final alphaTab.collections.DoubleObjectMap<alphaTab.model.Note> getNoteValueLookup();
  public final void setNoteValueLookup(alphaTab.collections.DoubleObjectMap<alphaTab.model.Note>);
  public final boolean isEmpty();
  public final void setEmpty(boolean);
  public final alphaTab.model.BendStyle getWhammyStyle();
  public final void setWhammyStyle(alphaTab.model.BendStyle);
  public final alphaTab.model.Ottavia getOttava();
  public final void setOttava(alphaTab.model.Ottavia);
  public final alphaTab.model.Fermata getFermata();
  public final void setFermata(alphaTab.model.Fermata);
  public final boolean isLegatoOrigin();
  public final void setLegatoOrigin(boolean);
  public final boolean isLegatoDestination();
  public final alphaTab.model.Note getMinNote();
  public final void setMinNote(alphaTab.model.Note);
  public final alphaTab.model.Note getMaxNote();
  public final void setMaxNote(alphaTab.model.Note);
  public final alphaTab.model.Note getMaxStringNote();
  public final void setMaxStringNote(alphaTab.model.Note);
  public final alphaTab.model.Note getMinStringNote();
  public final void setMinStringNote(alphaTab.model.Note);
  public final alphaTab.model.Duration getDuration();
  public final void setDuration(alphaTab.model.Duration);
  public final boolean isRest();
  public final boolean isFullBarRest();
  public final boolean isLetRing();
  public final void setLetRing(boolean);
  public final boolean isPalmMute();
  public final void setPalmMute(boolean);
  public final alphaTab.collections.List<alphaTab.model.Automation> getAutomations();
  public final void setAutomations(alphaTab.collections.List<alphaTab.model.Automation>);
  public final double getDots();
  public final void setDots(double);
  public final boolean getFadeIn();
  public final void setFadeIn(boolean);
  public final alphaTab.model.FadeType getFade();
  public final void setFade(alphaTab.model.FadeType);
  public final alphaTab.collections.List<java.lang.String> getLyrics();
  public final void setLyrics(alphaTab.collections.List<java.lang.String>);
  public final boolean getHasRasgueado();
  public final boolean getPop();
  public final void setPop(boolean);
```

```
// alphaTab.model.Note
public final class alphaTab.model.Note {
  public static final alphaTab.model.Note$Companion Companion;
  public alphaTab.model.Beat beat;
  public final double getId();
  public final void setId(double);
  public final double getIndex();
  public final void setIndex(double);
  public final alphaTab.model.AccentuationType getAccentuated();
  public final void setAccentuated(alphaTab.model.AccentuationType);
  public final alphaTab.model.BendType getBendType();
  public final void setBendType(alphaTab.model.BendType);
  public final alphaTab.model.BendStyle getBendStyle();
  public final void setBendStyle(alphaTab.model.BendStyle);
  public final alphaTab.model.Note getBendOrigin();
  public final void setBendOrigin(alphaTab.model.Note);
  public final boolean isContinuedBend();
  public final void setContinuedBend(boolean);
  public final alphaTab.collections.List<alphaTab.model.BendPoint> getBendPoints();
  public final void setBendPoints(alphaTab.collections.List<alphaTab.model.BendPoint>);
  public final alphaTab.model.BendPoint getMaxBendPoint();
  public final void setMaxBendPoint(alphaTab.model.BendPoint);
  public final boolean getHasBend();
  public final boolean isStringed();
  public final double getFret();
  public final void setFret(double);
  public final double getString();
  public final void setString(double);
  public final boolean getShowStringNumber();
  public final void setShowStringNumber(boolean);
  public final boolean isPiano();
  public final double getOctave();
  public final void setOctave(double);
  public final double getTone();
  public final void setTone(double);
  public final boolean isPercussion();
  public final double getElement();
  public final double getVariation();
  public final double getPercussionArticulation();
  public final void setPercussionArticulation(double);
  public final boolean isVisible();
  public final void setVisible(boolean);
  public final boolean isLeftHandTapped();
  public final void setLeftHandTapped(boolean);
  public final boolean isHammerPullOrigin();
  public final void setHammerPullOrigin(boolean);
  public final boolean isHammerPullDestination();
  public final alphaTab.model.Note getHammerPullOrigin();
  public final void setHammerPullOrigin(alphaTab.model.Note);
  public final alphaTab.model.Note getHammerPullDestination();
  public final void setHammerPullDestination(alphaTab.model.Note);
  public final boolean isSlurOrigin();
  public final boolean isSlurDestination();
  public final void setSlurDestination(boolean);
  public final alphaTab.model.Note getSlurOrigin();
  public final void setSlurOrigin(alphaTab.model.Note);
  public final alphaTab.model.Note getSlurDestination();
  public final void setSlurDestination(alphaTab.model.Note);
  public final boolean isHarmonic();
  public final alphaTab.model.HarmonicType getHarmonicType();
  public final void setHarmonicType(alphaTab.model.HarmonicType);
```

```
// alphaTab.model.Duration
public final class alphaTab.model.Duration extends java.lang.Enum<alphaTab.model.Duration> implements alphaTab.core.IAlphaTabEnum {
  public static final alphaTab.model.Duration$Companion Companion;
  public static final alphaTab.model.Duration QuadrupleWhole;
  public static final alphaTab.model.Duration DoubleWhole;
  public static final alphaTab.model.Duration Whole;
  public static final alphaTab.model.Duration Half;
  public static final alphaTab.model.Duration Quarter;
  public static final alphaTab.model.Duration Eighth;
  public static final alphaTab.model.Duration Sixteenth;
  public static final alphaTab.model.Duration ThirtySecond;
  public static final alphaTab.model.Duration SixtyFourth;
  public static final alphaTab.model.Duration OneHundredTwentyEighth;
  public static final alphaTab.model.Duration TwoHundredFiftySixth;
  public int getValue();
  public static alphaTab.model.Duration[] values();
  public static alphaTab.model.Duration valueOf(java.lang.String);
  public static kotlin.enums.EnumEntries<alphaTab.model.Duration> getEntries();
  public static final alphaTab.model.Duration[] access$getValues$cp();
  static {};
}
```

```
// alphaTab.model.MasterBar
public final class alphaTab.model.MasterBar {
  public static final alphaTab.model.MasterBar$Companion Companion;
  public alphaTab.model.RepeatGroup repeatGroup;
  public alphaTab.model.Score score;
  public final double getAlternateEndings();
  public final void setAlternateEndings(double);
  public final alphaTab.model.MasterBar getNextMasterBar();
  public final void setNextMasterBar(alphaTab.model.MasterBar);
  public final alphaTab.model.MasterBar getPreviousMasterBar();
  public final void setPreviousMasterBar(alphaTab.model.MasterBar);
  public final double getIndex();
  public final void setIndex(double);
  public final boolean getHasChanges();
  public final alphaTab.model.KeySignature getKeySignature();
  public final void setKeySignature(alphaTab.model.KeySignature);
  public final alphaTab.model.KeySignatureType getKeySignatureType();
  public final void setKeySignatureType(alphaTab.model.KeySignatureType);
  public final boolean isDoubleBar();
  public final void setDoubleBar(boolean);
  public static void isDoubleBar$annotations();
  public final boolean isRepeatStart();
  public final void setRepeatStart(boolean);
  public final boolean isRepeatEnd();
  public final double getRepeatCount();
  public final void setRepeatCount(double);
  public final alphaTab.model.RepeatGroup getRepeatGroup();
  public final void setRepeatGroup(alphaTab.model.RepeatGroup);
  public final double getTimeSignatureNumerator();
  public final void setTimeSignatureNumerator(double);
  public final double getTimeSignatureDenominator();
  public final void setTimeSignatureDenominator(double);
  public final boolean getTimeSignatureCommon();
  public final void setTimeSignatureCommon(boolean);
  public final alphaTab.model.BeamingRules getBeamingRules();
  public final void setBeamingRules(alphaTab.model.BeamingRules);
  public final alphaTab.model.BeamingRules getActualBeamingRules$android_release();
  public final void setActualBeamingRules$android_release(alphaTab.model.BeamingRules);
  public final boolean isFreeTime();
  public final void setFreeTime(boolean);
  public final alphaTab.model.TripletFeel getTripletFeel();
  public final void setTripletFeel(alphaTab.model.TripletFeel);
  public final alphaTab.model.Section getSection();
  public final void setSection(alphaTab.model.Section);
  public final boolean isSectionStart();
  public final alphaTab.model.Automation getTempoAutomation();
  public final alphaTab.collections.List<alphaTab.model.Automation> getTempoAutomations();
  public final void setTempoAutomations(alphaTab.collections.List<alphaTab.model.Automation>);
  public final alphaTab.collections.List<alphaTab.model.Automation> getSyncPoints();
  public final void setSyncPoints(alphaTab.collections.List<alphaTab.model.Automation>);
  public final alphaTab.model.Score getScore();
  public final void setScore(alphaTab.model.Score);
  public final alphaTab.collections.DoubleObjectMap<alphaTab.model.Fermata> getFermata();
  public final void setFermata(alphaTab.collections.DoubleObjectMap<alphaTab.model.Fermata>);
  public final double getStart();
  public final void setStart(double);
  public final boolean isAnacrusis();
  public final void setAnacrusis(boolean);
  public final double getDisplayScale();
  public final void setDisplayScale(double);
  public final double getDisplayWidth();
```

```
// alphaTab.rendering.utils.BoundsLookup
public final class alphaTab.rendering.utils.BoundsLookup {
  public static final alphaTab.rendering.utils.BoundsLookup$Companion Companion;
  public final alphaTab.collections.Map<java.lang.String, java.lang.Object> toJson();
  public final alphaTab.collections.List<alphaTab.rendering.utils.StaffSystemBounds> getStaffSystems();
  public final void setStaffSystems(alphaTab.collections.List<alphaTab.rendering.utils.StaffSystemBounds>);
  public final boolean isFinished();
  public final void setFinished(boolean);
  public final void finish(double);
  public static void finish$default(alphaTab.rendering.utils.BoundsLookup, double, int, java.lang.Object);
  public final void resetForPartialUpdate$android_release();
  public final void clearFromMasterBar$android_release(double);
  public final void addStaffSystem(alphaTab.rendering.utils.StaffSystemBounds);
  public final void addMasterBar(alphaTab.rendering.utils.MasterBarBounds);
  public final void addBeat(alphaTab.rendering.utils.BeatBounds);
  public final alphaTab.rendering.utils.MasterBarBounds findMasterBarByIndex(double);
  public final alphaTab.rendering.utils.MasterBarBounds findMasterBar(alphaTab.model.MasterBar);
  public final alphaTab.rendering.utils.BeatBounds findBeat(alphaTab.model.Beat);
  public final alphaTab.collections.List<alphaTab.rendering.utils.BeatBounds> findBeats(alphaTab.model.Beat);
  public final alphaTab.model.Beat getBeatAtPos(double, double);
  public final alphaTab.model.Note getNoteAtPos(alphaTab.model.Beat, double, double);
  public alphaTab.rendering.utils.BoundsLookup();
  public static final alphaTab.rendering.utils.BoundsLookup fromJson(alphaTab.collections.Map<java.lang.String, java.lang.Object>, alphaTab.model.Score);
  static {};
}
```

```
// alphaTab.rendering.utils.MasterBarBounds
public final class alphaTab.rendering.utils.MasterBarBounds {
  public alphaTab.rendering.utils.Bounds visualBounds;
  public alphaTab.rendering.utils.Bounds realBounds;
  public alphaTab.rendering.utils.Bounds lineAlignedBounds;
  public final double getIndex();
  public final void setIndex(double);
  public final boolean isFirstOfLine();
  public final void setFirstOfLine(boolean);
  public final alphaTab.rendering.utils.Bounds getVisualBounds();
  public final void setVisualBounds(alphaTab.rendering.utils.Bounds);
  public final alphaTab.rendering.utils.Bounds getRealBounds();
  public final void setRealBounds(alphaTab.rendering.utils.Bounds);
  public final alphaTab.rendering.utils.Bounds getLineAlignedBounds();
  public final void setLineAlignedBounds(alphaTab.rendering.utils.Bounds);
  public final alphaTab.collections.List<alphaTab.rendering.utils.BarBounds> getBars();
  public final void setBars(alphaTab.collections.List<alphaTab.rendering.utils.BarBounds>);
  public final alphaTab.rendering.utils.StaffSystemBounds getStaffSystemBounds();
  public final void setStaffSystemBounds(alphaTab.rendering.utils.StaffSystemBounds);
  public final alphaTab.rendering.utils.StaffSystemBounds getStaveGroupBounds();
  public final void addBar(alphaTab.rendering.utils.BarBounds);
  public final alphaTab.model.Beat findBeatAtPos(double);
  public final void finish(double);
  public static void finish$default(alphaTab.rendering.utils.MasterBarBounds, double, int, java.lang.Object);
  public final void addBeat(alphaTab.rendering.utils.BeatBounds);
  public alphaTab.rendering.utils.MasterBarBounds();
}
```

```
// alphaTab.rendering.utils.BarBounds
public final class alphaTab.rendering.utils.BarBounds {
  public alphaTab.rendering.utils.MasterBarBounds masterBarBounds;
  public alphaTab.rendering.utils.Bounds visualBounds;
  public alphaTab.rendering.utils.Bounds realBounds;
  public alphaTab.model.Bar bar;
  public final alphaTab.rendering.utils.MasterBarBounds getMasterBarBounds();
  public final void setMasterBarBounds(alphaTab.rendering.utils.MasterBarBounds);
  public final alphaTab.rendering.utils.Bounds getVisualBounds();
  public final void setVisualBounds(alphaTab.rendering.utils.Bounds);
  public final alphaTab.rendering.utils.Bounds getRealBounds();
  public final void setRealBounds(alphaTab.rendering.utils.Bounds);
  public final alphaTab.model.Bar getBar();
  public final void setBar(alphaTab.model.Bar);
  public final alphaTab.collections.List<alphaTab.rendering.utils.BeatBounds> getBeats();
  public final void setBeats(alphaTab.collections.List<alphaTab.rendering.utils.BeatBounds>);
  public final void addBeat(alphaTab.rendering.utils.BeatBounds);
  public final alphaTab.rendering.utils.BeatBounds findBeatAtPos(double);
  public final void finish(double);
  public static void finish$default(alphaTab.rendering.utils.BarBounds, double, int, java.lang.Object);
  public alphaTab.rendering.utils.BarBounds();
}
```

```
// alphaTab.rendering.utils.BeatBounds
public final class alphaTab.rendering.utils.BeatBounds {
  public alphaTab.rendering.utils.BarBounds barBounds;
  public alphaTab.rendering.utils.Bounds visualBounds;
  public alphaTab.rendering.utils.Bounds realBounds;
  public alphaTab.model.Beat beat;
  public final alphaTab.rendering.utils.BarBounds getBarBounds();
  public final void setBarBounds(alphaTab.rendering.utils.BarBounds);
  public final alphaTab.rendering.utils.Bounds getVisualBounds();
  public final void setVisualBounds(alphaTab.rendering.utils.Bounds);
  public final double getOnNotesX();
  public final void setOnNotesX(double);
  public final alphaTab.rendering.utils.Bounds getRealBounds();
  public final void setRealBounds(alphaTab.rendering.utils.Bounds);
  public final alphaTab.model.Beat getBeat();
  public final void setBeat(alphaTab.model.Beat);
  public final alphaTab.collections.List<alphaTab.rendering.utils.NoteBounds> getNotes();
  public final void setNotes(alphaTab.collections.List<alphaTab.rendering.utils.NoteBounds>);
  public final void addNote(alphaTab.rendering.utils.NoteBounds);
  public final alphaTab.model.Note findNoteAtPos(double, double);
  public final void finish(double);
  public static void finish$default(alphaTab.rendering.utils.BeatBounds, double, int, java.lang.Object);
  public alphaTab.rendering.utils.BeatBounds();
}
```

```
// alphaTab.rendering.utils.NoteBounds
public final class alphaTab.rendering.utils.NoteBounds {
  public alphaTab.rendering.utils.BeatBounds beatBounds;
  public alphaTab.rendering.utils.Bounds noteHeadBounds;
  public alphaTab.model.Note note;
  public final alphaTab.rendering.utils.BeatBounds getBeatBounds();
  public final void setBeatBounds(alphaTab.rendering.utils.BeatBounds);
  public final alphaTab.rendering.utils.Bounds getNoteHeadBounds();
  public final void setNoteHeadBounds(alphaTab.rendering.utils.Bounds);
  public final alphaTab.model.Note getNote();
  public final void setNote(alphaTab.model.Note);
  public final void finish(double);
  public static void finish$default(alphaTab.rendering.utils.NoteBounds, double, int, java.lang.Object);
  public alphaTab.rendering.utils.NoteBounds();
}
```

```
// alphaTab.rendering.utils.Bounds
public final class alphaTab.rendering.utils.Bounds {
  public final double getX();
  public final void setX(double);
  public final double getY();
  public final void setY(double);
  public final double getW();
  public final void setW(double);
  public final double getH();
  public final void setH(double);
  public final void scaleWith(double);
  public alphaTab.rendering.utils.Bounds(double, double, double, double);
  public alphaTab.rendering.utils.Bounds(double, double, double, double, int, kotlin.jvm.internal.DefaultConstructorMarker);
}
```

```
// alphaTab.synth.PlaybackRange
public final class alphaTab.synth.PlaybackRange {
  public final double getStartTick();
  public final void setStartTick(double);
  public final double getEndTick();
  public final void setEndTick(double);
  public alphaTab.synth.PlaybackRange();
}
```

```
// alphaTab.synth.IAlphaSynth
public interface alphaTab.synth.IAlphaSynth {
  public abstract boolean isReady();
  public abstract boolean isReadyForPlayback();
  public abstract alphaTab.synth.PlayerState getState();
  public abstract alphaTab.LogLevel getLogLevel();
  public abstract void setLogLevel(alphaTab.LogLevel);
  public abstract double getMasterVolume();
  public abstract void setMasterVolume(double);
  public abstract double getMetronomeVolume();
  public abstract void setMetronomeVolume(double);
  public abstract double getPlaybackSpeed();
  public abstract void setPlaybackSpeed(double);
  public abstract double getTickPosition();
  public abstract void setTickPosition(double);
  public abstract double getTimePosition();
  public abstract void setTimePosition(double);
  public abstract alphaTab.synth.PositionChangedEventArgs getLoadedMidiInfo();
  public abstract alphaTab.synth.PositionChangedEventArgs getCurrentPosition();
  public abstract alphaTab.synth.PlaybackRange getPlaybackRange();
  public abstract void setPlaybackRange(alphaTab.synth.PlaybackRange);
  public abstract boolean isLooping();
  public abstract void setLooping(boolean);
  public abstract double getCountInVolume();
  public abstract void setCountInVolume(double);
  public abstract alphaTab.collections.List<alphaTab.midi.MidiEventType> getMidiEventsPlayedFilter();
  public abstract void setMidiEventsPlayedFilter(alphaTab.collections.List<alphaTab.midi.MidiEventType>);
  public abstract alphaTab.synth.ISynthOutput getOutput();
  public abstract void destroy();
  public abstract boolean play();
  public abstract void pause();
  public abstract void playPause();
  public abstract void stop();
  public abstract void playOneTimeMidiFile(alphaTab.midi.MidiFile);
  public abstract void loadSoundFont(alphaTab.core.ecmaScript.Uint8Array, boolean);
  public abstract void resetSoundFonts();
  public abstract void loadMidiFile(alphaTab.midi.MidiFile);
  public abstract void loadBackingTrack(alphaTab.model.Score);
  public abstract void updateSyncPoints(alphaTab.collections.List<alphaTab.synth.BackingTrackSyncPoint>);
  public abstract void applyTranspositionPitches(alphaTab.collections.DoubleDoubleMap);
  public abstract void setChannelTranspositionPitch(double, double);
  public abstract void setChannelMute(double, boolean);
  public abstract void resetChannelStates();
  public abstract void setChannelSolo(double, boolean);
  public abstract void setChannelVolume(double, double);
  public abstract alphaTab.IEventEmitter getReady();
  public abstract alphaTab.IEventEmitter getReadyForPlayback();
  public abstract alphaTab.IEventEmitter getFinished();
  public abstract alphaTab.IEventEmitter getSoundFontLoaded();
  public abstract alphaTab.IEventEmitterOfT<java.lang.Throwable> getSoundFontLoadFailed();
  public abstract alphaTab.IEventEmitterOfT<alphaTab.synth.PositionChangedEventArgs> getMidiLoaded();
  public abstract alphaTab.IEventEmitterOfT<java.lang.Throwable> getMidiLoadFailed();
  public abstract alphaTab.IEventEmitterOfT<alphaTab.synth.PlayerStateChangedEventArgs> getStateChanged();
  public abstract alphaTab.IEventEmitterOfT<alphaTab.synth.PositionChangedEventArgs> getPositionChanged();
  public abstract alphaTab.IEventEmitterOfT<alphaTab.synth.MidiEventsPlayedEventArgs> getMidiEventsPlayed();
  public abstract alphaTab.IEventEmitterOfT<alphaTab.synth.PlaybackRangeChangedEventArgs> getPlaybackRangeChanged();
}
```

```
// alphaTab.synth.PlayerState
public final class alphaTab.synth.PlayerState extends java.lang.Enum<alphaTab.synth.PlayerState> implements alphaTab.core.IAlphaTabEnum {
  public static final alphaTab.synth.PlayerState$Companion Companion;
  public static final alphaTab.synth.PlayerState Paused;
  public static final alphaTab.synth.PlayerState Playing;
  public int getValue();
  public static alphaTab.synth.PlayerState[] values();
  public static alphaTab.synth.PlayerState valueOf(java.lang.String);
  public static kotlin.enums.EnumEntries<alphaTab.synth.PlayerState> getEntries();
  public static final alphaTab.synth.PlayerState[] access$getValues$cp();
  static {};
}
```

```
// alphaTab.importer.AlphaTexImporter
public final class alphaTab.importer.AlphaTexImporter extends alphaTab.importer.ScoreImporter implements alphaTab.importer.alphaTex.IAlphaTexImporter {
  public alphaTab.importer.alphaTex.IAlphaTexImporterState getState();
  public final alphaTab.importer.alphaTex.AlphaTexScoreNode getScoreNode();
  public java.lang.String getName();
  public final alphaTab.importer.alphaTex.AlphaTexDiagnosticBag getLexerDiagnostics();
  public final alphaTab.importer.alphaTex.AlphaTexDiagnosticBag getParserDiagnostics();
  public final alphaTab.importer.alphaTex.AlphaTexParser getParser();
  public alphaTab.importer.alphaTex.AlphaTexParseMode getParseMode();
  public final boolean getLogErrors();
  public final void setLogErrors(boolean);
  public final alphaTab.importer.alphaTex.AlphaTexDiagnosticBag getSemanticDiagnostics();
  public final void setSemanticDiagnostics(alphaTab.importer.alphaTex.AlphaTexDiagnosticBag);
  public void addSemanticDiagnostic(alphaTab.importer.alphaTex.AlphaTexDiagnostic);
  public final void initFromString(java.lang.String, alphaTab.Settings);
  public alphaTab.model.Score readScore();
  public final alphaTab.importer.alphaTex.AlphaTexStaffNoteKind getStaffNoteKind$android_release(alphaTab.model.Staff);
  public void applyStaffNoteKind(alphaTab.model.Staff, alphaTab.importer.alphaTex.AlphaTexStaffNoteKind);
  public alphaTab.model.Staff startNewStaff();
  public final void applyPercussionStaff(alphaTab.model.Staff);
  public alphaTab.model.Track startNewTrack();
  public void startNewVoice();
  public alphaTab.importer.AlphaTexImporter();
}
```

```
// alphaTab.importer.ScoreLoader
public final class alphaTab.importer.ScoreLoader {
  public static final alphaTab.importer.ScoreLoader$Companion Companion;
  public alphaTab.importer.ScoreLoader();
  public static final alphaTab.model.Score loadAlphaTex(java.lang.String, alphaTab.Settings);
  public static final alphaTab.model.Score loadScoreFromBytes(alphaTab.core.ecmaScript.Uint8Array, alphaTab.Settings);
  static {};
}
```

```
// alphaTab.EventEmitter
public final class alphaTab.EventEmitter implements alphaTab.IEventEmitter {
  public alphaTab.EventEmitter(kotlin.jvm.functions.Function0<java.lang.Boolean>);
  public alphaTab.EventEmitter(kotlin.jvm.functions.Function0, int, kotlin.jvm.internal.DefaultConstructorMarker);
  public kotlin.jvm.functions.Function0<kotlin.Unit> on(kotlin.jvm.functions.Function0<kotlin.Unit>);
  public void off(kotlin.jvm.functions.Function0<kotlin.Unit>);
  public final void trigger();
}
```

```
// alphaTab.EventEmitterOfT
public final class alphaTab.EventEmitterOfT<T> implements alphaTab.IEventEmitterOfT<T> {
  public alphaTab.EventEmitterOfT(kotlin.jvm.functions.Function0<? extends T>);
  public alphaTab.EventEmitterOfT(kotlin.jvm.functions.Function0, int, kotlin.jvm.internal.DefaultConstructorMarker);
  public kotlin.jvm.functions.Function0<kotlin.Unit> on(kotlin.jvm.functions.Function1<? super T, kotlin.Unit>);
  public void off(kotlin.jvm.functions.Function1<? super T, kotlin.Unit>);
  public final void trigger(T);
}
```

```
// alphaTab.platform.android.AndroidMouseEventArgs
public final class alphaTab.platform.android.AndroidMouseEventArgs implements alphaTab.platform.IMouseEventArgs {
  public alphaTab.platform.android.AndroidMouseEventArgs(android.view.MotionEvent);
  public final boolean getDefaultPrevented();
  public boolean isLeftMouseButton();
  public double getX(alphaTab.platform.IContainer);
  public double getY(alphaTab.platform.IContainer);
  public void preventDefault();
}
```

