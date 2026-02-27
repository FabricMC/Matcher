package matcher.gui;

import java.util.ArrayList;
import java.util.List;

import javafx.application.Application;

import matcher.core.PluginLoader;
import matcher.model.config.Config;

public class Main {
	public static void main(String[] args) {
		List<String> extraPluginPaths = null;
		String themeId = null;

		for (int i = 0; i < args.length; i++) {
			switch (args[i]) {
			case "--additional-plugins":
				extraPluginPaths = new ArrayList<>();

				while (i+1 < args.length && !args[i+1].startsWith("--")) {
					extraPluginPaths.add(args[++i]);
				}

				break;
			case "--theme":
				themeId = args[++i];
				break;
			}
		}

		Config.init(themeId);
		PluginLoader.run(extraPluginPaths);
		Application.launch(MatcherGui.class, args);
	}
}
