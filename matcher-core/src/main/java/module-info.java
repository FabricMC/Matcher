module matcher.core {
	requires transitive matcher.model;

	uses matcher.core.Plugin;

	exports matcher.core;
	exports matcher.core.serdes;
}
