package matcher.cli.provider.builtin;

import java.util.Collections;
import java.util.List;

import com.beust.jcommander.Parameter;

import matcher.cli.provider.CliParameterProvider;
import matcher.core.PluginLoader;

/**
 * Provides the default {@code --additional-plugins} parameter.
 * If the parameter is present, the passed plugins are automatically loaded.
 */
public class AdditionalPluginsCliParameterProvider implements CliParameterProvider {
	@Parameter(names = {BuiltinCliParameters.ADDITIONAL_PLUGINS})
	List<String> additionalPlugins = Collections.emptyList();

	@Override
	public Object getDataHolder() {
		return this;
	}

	@Override
	public void processArgs() {
		PluginLoader.run(additionalPlugins);
	}
}
