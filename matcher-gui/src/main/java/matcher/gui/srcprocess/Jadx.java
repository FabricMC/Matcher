package matcher.gui.srcprocess;

import jadx.api.CommentsLevel;
import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.api.impl.NoOpCodeCache;
import jadx.plugins.input.java.JavaInputPlugin;

import matcher.model.NameType;
import matcher.model.Util;
import matcher.model.type.ClassFeatureExtractor;
import matcher.model.type.ClassInstance;
import matcher.model.type.Decompiler;

public class Jadx implements Decompiler {
	@Override
	public String decompile(ClassInstance cls, ClassFeatureExtractor env, NameType nameType) {
		String fullClassName = cls.getName(NameType.PLAIN, true);
		String errorMessage;

		try (JadxDecompiler jadx = new JadxDecompiler(createJadxArgs())) {
			jadx.addCustomCodeLoader(JavaInputPlugin.loadSingleClass(cls.serialize(nameType), fullClassName));
			jadx.load();

			assert jadx.getClassesWithInners().size() == 1;
			return jadx.getClassesWithInners().get(0).getCode();
		} catch (Exception e) {
			errorMessage = Util.getStackTrace(e);
		}

		throw new RuntimeException(errorMessage != null ? errorMessage : "JADX couldn't find the requested class");
	}

	private JadxArgs createJadxArgs() {
		JadxArgs args = new JadxArgs();
		args.setCodeCache(NoOpCodeCache.INSTANCE);
		args.setShowInconsistentCode(true);
		args.setInlineAnonymousClasses(false);
		args.setInlineMethods(false);
		args.setRespectBytecodeAccModifiers(true);
		args.setRenameValid(false);
		args.setCodeIndentStr("\t");
		args.setCommentsLevel(CommentsLevel.INFO);

		return args;
	}
}
