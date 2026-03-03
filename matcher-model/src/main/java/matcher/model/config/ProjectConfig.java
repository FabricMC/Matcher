package matcher.model.config;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class ProjectConfig {
	public static class Builder {
		public Builder(List<Path> pathsA, List<Path> pathsB) {
			this.pathsA = pathsA;
			this.pathsB = pathsB;
		}

		Builder(Preferences prefs) throws BackingStoreException {
			pathsA = Config.loadList(prefs, PATHS_A_KEY, Config::deserializePath);
			pathsB = Config.loadList(prefs, PATHS_B_KEY, Config::deserializePath);
			classPathA = Config.loadList(prefs, CLASS_PATH_A_KEY, Config::deserializePath);
			classPathB = Config.loadList(prefs, CLASS_PATH_B_KEY, Config::deserializePath);
			sharedClassPath = Config.loadList(prefs, PATHS_SHARED_KEY, Config::deserializePath);
			inputsBeforeClassPath = prefs.getBoolean(INPUTS_BEFORE_CLASS_PATH_KEY, false);

			String storedMappingsPathA = prefs.get(MAPPINGS_PATH_A_KEY, null);
			String storedMappingsPathB = prefs.get(MAPPINGS_PATH_B_KEY, null);
			mappingsPathA = storedMappingsPathA == null ? null : Path.of(storedMappingsPathA);
			mappingsPathB = storedMappingsPathB == null ? null : Path.of(storedMappingsPathB);
			saveUnmappedMatches = prefs.getBoolean(INPUTS_BEFORE_CLASS_PATH_KEY, false);

			nonObfuscatedClassPatternA = prefs.get(NON_OBFUSCATED_CLASS_PATTERN_A_KEY, "");
			nonObfuscatedClassPatternB = prefs.get(NON_OBFUSCATED_CLASS_PATTERN_B_KEY, "");
			nonObfuscatedMemberPatternA = prefs.get(NON_OBFUSCATED_MEMBER_PATTERN_A_KEY, "");
			nonObfuscatedMemberPatternB = prefs.get(NON_OBFUSCATED_MEMBER_PATTERN_B_KEY, "");
		}

		public Builder classPathA(List<Path> classPathA) {
			this.classPathA = classPathA;
			return this;
		}

		public Builder classPathB(List<Path> classPathB) {
			this.classPathB = classPathB;
			return this;
		}

		public Builder sharedClassPath(List<Path> sharedClassPath) {
			this.sharedClassPath = sharedClassPath;
			return this;
		}

		public Builder inputsBeforeClassPath(boolean inputsBeforeClassPath) {
			this.inputsBeforeClassPath = inputsBeforeClassPath;
			return this;
		}

		public Builder mappingsPathA(Path mappingsPathA) {
			this.mappingsPathA = mappingsPathA;
			return this;
		}

		public Builder mappingsPathB(Path mappingsPathB) {
			this.mappingsPathB = mappingsPathB;
			return this;
		}

		public Builder saveUnmappedMatches(boolean saveUnmappedMatches) {
			this.saveUnmappedMatches = saveUnmappedMatches;
			return this;
		}

		public Builder nonObfuscatedClassPatternA(String nonObfuscatedClassPatternA) {
			this.nonObfuscatedClassPatternA = nonObfuscatedClassPatternA;
			return this;
		}

		public Builder nonObfuscatedClassPatternB(String nonObfuscatedClassPatternB) {
			this.nonObfuscatedClassPatternB = nonObfuscatedClassPatternB;
			return this;
		}

		public Builder nonObfuscatedMemberPatternA(String nonObfuscatedMemberPatternA) {
			this.nonObfuscatedMemberPatternA = nonObfuscatedMemberPatternA;
			return this;
		}

		public Builder nonObfuscatedMemberPatternB(String nonObfuscatedMemberPatternB) {
			this.nonObfuscatedMemberPatternB = nonObfuscatedMemberPatternB;
			return this;
		}

		public ProjectConfig build() {
			return new ProjectConfig(pathsA, pathsB, classPathA, classPathB, sharedClassPath, inputsBeforeClassPath, mappingsPathA, mappingsPathB, saveUnmappedMatches,
					nonObfuscatedClassPatternA, nonObfuscatedClassPatternB, nonObfuscatedMemberPatternA, nonObfuscatedMemberPatternB);
		}

		protected final List<Path> pathsA;
		protected final List<Path> pathsB;
		protected List<Path> classPathA;
		protected List<Path> classPathB;
		protected List<Path> sharedClassPath;
		protected boolean inputsBeforeClassPath;
		protected Path mappingsPathA;
		protected Path mappingsPathB;
		protected boolean saveUnmappedMatches = true;
		protected String nonObfuscatedClassPatternA;
		protected String nonObfuscatedClassPatternB;
		protected String nonObfuscatedMemberPatternA;
		protected String nonObfuscatedMemberPatternB;
	}

	private ProjectConfig(List<Path> pathsA, List<Path> pathsB, List<Path> classPathA, List<Path> classPathB,
			List<Path> sharedClassPath, boolean inputsBeforeClassPath, Path mappingsPathA, Path mappingsPathB, boolean saveUnmappedMatches,
			String nonObfuscatedClassesPatternA, String nonObfuscatedClassesPatternB, String nonObfuscatedMemberPatternA, String nonObfuscatedMemberPatternB) {
		this.pathsA = pathsA;
		this.pathsB = pathsB;
		this.classPathA = classPathA;
		this.classPathB = classPathB;
		this.sharedClassPath = sharedClassPath;
		this.inputsBeforeClassPath = inputsBeforeClassPath;
		this.mappingsPathA = mappingsPathA;
		this.mappingsPathB = mappingsPathB;
		this.saveUnmappedMatches = saveUnmappedMatches;
		this.nonObfuscatedClassPatternA = nonObfuscatedClassesPatternA;
		this.nonObfuscatedClassPatternB = nonObfuscatedClassesPatternB;
		this.nonObfuscatedMemberPatternA = nonObfuscatedMemberPatternA;
		this.nonObfuscatedMemberPatternB = nonObfuscatedMemberPatternB;
	}

	public List<Path> getPathsA() {
		return pathsA;
	}

	public List<Path> getPathsB() {
		return pathsB;
	}

	public List<Path> getClassPathA() {
		return classPathA;
	}

	public List<Path> getClassPathB() {
		return classPathB;
	}

	public List<Path> getSharedClassPath() {
		return sharedClassPath;
	}

	public boolean hasInputsBeforeClassPath() {
		return inputsBeforeClassPath;
	}

	public Path getMappingsPathA() {
		return mappingsPathA;
	}

	public Path getMappingsPathB() {
		return mappingsPathB;
	}

	public boolean isSaveUnmappedMatches() {
		return saveUnmappedMatches;
	}

	public String getNonObfuscatedClassPatternA() {
		return nonObfuscatedClassPatternA;
	}

	public String getNonObfuscatedClassPatternB() {
		return nonObfuscatedClassPatternB;
	}

	public String getNonObfuscatedMemberPatternA() {
		return nonObfuscatedMemberPatternA;
	}

	public String getNonObfuscatedMemberPatternB() {
		return nonObfuscatedMemberPatternB;
	}

	public boolean isValid() {
		return !pathsA.isEmpty()
				&& !pathsB.isEmpty()
				&& Collections.disjoint(pathsA, pathsB)
				&& Collections.disjoint(pathsA, sharedClassPath)
				&& Collections.disjoint(pathsB, sharedClassPath)
				// && Collections.disjoint(classPathA, classPathB)
				&& Collections.disjoint(classPathA, pathsA)
				&& Collections.disjoint(classPathB, pathsA)
				&& Collections.disjoint(classPathA, pathsB)
				&& Collections.disjoint(classPathB, pathsB)
				&& Collections.disjoint(classPathA, sharedClassPath)
				&& Collections.disjoint(classPathB, sharedClassPath)
				&& tryCompilePattern(nonObfuscatedClassPatternA)
				&& tryCompilePattern(nonObfuscatedClassPatternB)
				&& tryCompilePattern(nonObfuscatedMemberPatternA)
				&& tryCompilePattern(nonObfuscatedMemberPatternB);
	}

	private static boolean tryCompilePattern(String regex) {
		try {
			Pattern.compile(regex);
			return true;
		} catch (PatternSyntaxException e) {
			return false;
		}
	}

	void save(Preferences prefs) throws BackingStoreException {
		if (!isValid()) return;

		Config.saveList(prefs.node(PATHS_A_KEY), pathsA);
		Config.saveList(prefs.node(PATHS_B_KEY), pathsB);
		Config.saveList(prefs.node(CLASS_PATH_A_KEY), classPathA);
		Config.saveList(prefs.node(CLASS_PATH_B_KEY), classPathB);
		Config.saveList(prefs.node(PATHS_SHARED_KEY), sharedClassPath);
		prefs.putBoolean(INPUTS_BEFORE_CLASS_PATH_KEY, inputsBeforeClassPath);
		if (mappingsPathA != null) prefs.put(MAPPINGS_PATH_A_KEY, mappingsPathA.toString());
		if (mappingsPathB != null) prefs.put(MAPPINGS_PATH_B_KEY, mappingsPathB.toString());
		prefs.putBoolean(SAVE_UNMAPPED_MATCHES_KEY, saveUnmappedMatches);
		prefs.put(NON_OBFUSCATED_CLASS_PATTERN_A_KEY, nonObfuscatedClassPatternA);
		prefs.put(NON_OBFUSCATED_CLASS_PATTERN_B_KEY, nonObfuscatedClassPatternB);
		prefs.put(NON_OBFUSCATED_MEMBER_PATTERN_A_KEY, nonObfuscatedMemberPatternA);
		prefs.put(NON_OBFUSCATED_MEMBER_PATTERN_B_KEY, nonObfuscatedMemberPatternB);
	}

	public static final ProjectConfig EMPTY = new ProjectConfig(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
			false, null, null, true, "", "", "", "");

	private static final String PATHS_A_KEY = "paths-a";
	private static final String PATHS_B_KEY = "paths-b";
	private static final String CLASS_PATH_A_KEY = "class-path-a";
	private static final String CLASS_PATH_B_KEY = "class-path-b";
	private static final String PATHS_SHARED_KEY = "paths-shared";
	private static final String INPUTS_BEFORE_CLASS_PATH_KEY = "inputs-before-classpath";
	private static final String MAPPINGS_PATH_A_KEY = "mappings-path-a";
	private static final String MAPPINGS_PATH_B_KEY = "mappings-path-b";
	private static final String SAVE_UNMAPPED_MATCHES_KEY = "save-unmapped-matches";
	private static final String NON_OBFUSCATED_CLASS_PATTERN_A_KEY = "non-obfuscated-class-pattern-a";
	private static final String NON_OBFUSCATED_CLASS_PATTERN_B_KEY = "non-obfuscated-class-pattern-b";
	private static final String NON_OBFUSCATED_MEMBER_PATTERN_A_KEY = "non-obfuscated-member-pattern-a";
	private static final String NON_OBFUSCATED_MEMBER_PATTERN_B_KEY = "non-obfuscated-member-pattern-b";

	private final List<Path> pathsA;
	private final List<Path> pathsB;
	private final List<Path> classPathA;
	private final List<Path> classPathB;
	private final List<Path> sharedClassPath;
	private final Path mappingsPathA;
	private final Path mappingsPathB;
	private final boolean saveUnmappedMatches;
	private final boolean inputsBeforeClassPath;
	private final String nonObfuscatedClassPatternA;
	private final String nonObfuscatedClassPatternB;
	private final String nonObfuscatedMemberPatternA;
	private final String nonObfuscatedMemberPatternB;
}
