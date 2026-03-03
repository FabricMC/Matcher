package matcher.gui.srcprocess;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.benf.cfr.reader.api.CfrDriver;
import org.benf.cfr.reader.api.ClassFileSource;
import org.benf.cfr.reader.api.OutputSinkFactory;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import matcher.model.NameType;
import matcher.model.type.ClassFeatureExtractor;
import matcher.model.type.ClassInstance;
import matcher.model.type.Decompiler;

public class Cfr implements Decompiler {
	@Override
	public synchronized String decompile(ClassInstance cls, ClassFeatureExtractor env, NameType nameType) {
		Map<String, String> options = new HashMap<>();

		Sink sink = new Sink();

		CfrDriver driver = new CfrDriver.Builder()
				.withOptions(options)
				.withClassFileSource(new Source(env, nameType))
				.withOutputSink(sink)
				.build();

		driver.analyse(List.of(cls.getName(nameType).concat(FILE_SUFFIX)));

		return sink.toString();
	}

	private record Source(ClassFeatureExtractor env, NameType nameType) implements ClassFileSource {
		@Override
		public void informAnalysisRelativePathDetail(String usePath, String classFilePath) {
			// LOGGER.debug("informAnalysisRelativePathDetail {} {}", usePath, classFilePath);
		}

		@Override
		public Collection<String> addJar(String jarPath) {
			LOGGER.debug("addJar {}", jarPath);

			throw new UnsupportedOperationException();
		}

		@Override
		public String getPossiblyRenamedPath(String path) {
			return path;
		}

		@Override
		public Pair<byte[], String> getClassFileContent(String path) throws IOException {
			if (!path.endsWith(FILE_SUFFIX)) {
				LOGGER.debug("getClassFileContent invalid path: {}", path);
				throw new NoSuchFileException(path);
			}

			String clsName = path.substring(0, path.length() - FILE_SUFFIX.length());
			ClassInstance cls = env.getClsByName(clsName, nameType);

			if (cls == null) {
				LOGGER.debug("getClassFileContent missing cls: {}", clsName);
				throw new NoSuchFileException(path);
			}

			if (cls.getAsmNodes() == null) {
				LOGGER.debug("getClassFileContent unknown cls: {}", clsName);
				throw new NoSuchFileException(path);
			}

			byte[] data = cls.serialize(nameType);

			return Pair.make(data, path);
		}
	}

	private static class Sink implements OutputSinkFactory {
		@Override
		public List<SinkClass> getSupportedSinks(SinkType sinkType, Collection<SinkClass> available) {
			return List.of(SinkClass.STRING);
		}

		@Override
		public <T> OutputSinkFactory.Sink<T> getSink(SinkType sinkType, SinkClass sinkClass) {
			switch (sinkType) {
			case EXCEPTION:
				return str -> LOGGER.error("CFR exception: {}", str);
			case JAVA:
				return sb::append;
			case PROGRESS:
				return str -> LOGGER.debug("CFR progress: {}", str);
			case SUMMARY:
				return str -> LOGGER.debug("CFR summary: {}", str);
			default:
				LOGGER.debug("Unknown CFR sink type: {}", sinkType);
				return str -> LOGGER.debug("{}", str);
			}
		}

		@Override
		public String toString() {
			return sb.toString();
		}

		private final StringBuilder sb = new StringBuilder();
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(Cfr.class);
	private static final String FILE_SUFFIX = ".class";
}
