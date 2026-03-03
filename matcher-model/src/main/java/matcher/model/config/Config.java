package matcher.model.config;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public final class Config {
	public static void init(String themeId) {
		Preferences prefs = Preferences.userRoot(); // in ~/.java/.userPrefs

		try {
			if (prefs.nodeExists(USER_PREF_FOLDER)) {
				prefs = prefs.node(USER_PREF_FOLDER);

				if (prefs.nodeExists(LAST_PROJECT_SETUP_KEY)) setProjectConfig(new ProjectConfig.Builder(prefs.node(LAST_PROJECT_SETUP_KEY)).build());
				setInputDirs(loadList(prefs, LAST_INPUT_DIRS_KEY, Config::deserializePath));
				setVerifyInputFiles(prefs.getBoolean(LAST_VERIFY_INPUT_FILES_KEY, true));
				setUidConfig(new UidConfig(prefs));
				setTheme(Theme.getById(prefs.get(THEME_KEY, Theme.getDefault().getId())));
			}
		} catch (BackingStoreException e) {
			// ignored
		}

		if (themeId != null) {
			Theme theme = Theme.getById(themeId);

			if (theme == null) {
				System.err.println("Startup arg '--theme' couldn't be applied, as there exists no theme with ID " + themeId + "!");
			} else {
				setTheme(theme);
			}
		}
	}

	private Config() { }

	public static ProjectConfig getProjectConfig() {
		return projectConfig;
	}

	public static boolean getVerifyInputFiles() {
		return verifyInputFiles;
	}

	public static List<Path> getInputDirs() {
		return INPUT_DIRS;
	}

	public static UidConfig getUidConfig() {
		return uidConfig;
	}

	public static Theme getTheme() {
		return theme != null ? theme : Theme.getDefault();
	}

	public static boolean setProjectConfig(ProjectConfig config) {
		if (!config.isValid()) return false;

		projectConfig = config;

		return true;
	}

	public static void setInputDirs(List<Path> dirs) {
		INPUT_DIRS.clear();
		INPUT_DIRS.addAll(dirs);
	}

	public static void setVerifyInputFiles(boolean value) {
		verifyInputFiles = value;
	}

	public static boolean setUidConfig(UidConfig config) {
		if (!config.isValid()) return false;

		uidConfig = config;

		return true;
	}

	public static void setTheme(Theme value) {
		if (value != null) {
			theme = value;
		}
	}

	public static void saveTheme() {
		Preferences root = Preferences.userRoot().node(USER_PREF_FOLDER);

		try {
			root.put(THEME_KEY, getTheme().getId());
			root.flush();
		} catch (BackingStoreException e) {
			// ignored
		}
	}

	public static void saveAsLast() {
		Preferences root = Preferences.userRoot().node(USER_PREF_FOLDER);

		try {
			if (projectConfig.isValid()) projectConfig.save(root.node(LAST_PROJECT_SETUP_KEY));
			saveList(root.node(LAST_INPUT_DIRS_KEY), INPUT_DIRS);
			root.putBoolean(LAST_VERIFY_INPUT_FILES_KEY, verifyInputFiles);
			uidConfig.save(root);

			root.flush();
		} catch (BackingStoreException e) {
			throw new RuntimeException(e);
		}
	}

	static <T> List<T> loadList(Preferences parent, String key, Function<String, T> deserializer) throws BackingStoreException {
		if (!parent.nodeExists(key)) return Collections.emptyList();

		parent = parent.node(key);
		List<T> ret = new ArrayList<>();
		String value;

		for (int i = 0; (value = parent.get(Integer.toString(i), null)) != null; i++) {
			ret.add(deserializer.apply(value));
		}

		return ret;
	}

	static void saveList(Preferences parent, List<?> list) throws BackingStoreException {
		parent.clear();

		for (int i = 0; i < list.size(); i++) {
			parent.put(Integer.toString(i), list.get(i).toString());
		}
	}

	static Path deserializePath(String path) {
		return Path.of(path);
	}

	private static final String USER_PREF_FOLDER = "player-obf-matcher";
	private static final String LAST_PROJECT_SETUP_KEY = "last-project-setup";
	private static final String LAST_INPUT_DIRS_KEY = "last-input-dirs";
	private static final String LAST_VERIFY_INPUT_FILES_KEY = "last-verify-input-files";
	private static final String THEME_KEY = "theme";

	private static ProjectConfig projectConfig = ProjectConfig.EMPTY;
	private static final List<Path> INPUT_DIRS = new ArrayList<>();
	private static boolean verifyInputFiles = true;
	private static UidConfig uidConfig = new UidConfig();
	private static Theme theme;
}
