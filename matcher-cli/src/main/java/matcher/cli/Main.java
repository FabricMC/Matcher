package matcher.cli;

import matcher.cli.provider.builtin.AutomatchCliCommandProvider;
import matcher.config.Config;

public class Main {
	public static void main(String[] args) {
		Config.init(args);

		// Instantiate the CLI handler. We don't accept unknown parameters,
		// since this is the base implementation where only known
		// providers are registered.
		MatcherCli matcherCli = new MatcherCli(false);

		// Register all default providers.
		matcherCli.registerCommandProvider(new AutomatchCliCommandProvider());

		// Parse, handle errors, delegate to the correct provider.
		matcherCli.processArgs(args);
	}
}
