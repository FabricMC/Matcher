package matcher.model.type;

import matcher.model.NameType;

public interface Decompiler {
	String decompile(ClassInstance cls, ClassFeatureExtractor extractor, NameType nameType);
}
