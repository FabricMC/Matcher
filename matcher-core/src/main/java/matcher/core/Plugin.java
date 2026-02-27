package matcher.core;

public interface Plugin {
	String getName();
	String getVersion();
	void init(int pluginApiVersion);
}
