package matcher.model.classifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.VarInsnNode;

import matcher.model.type.ClassEnvironment;
import matcher.model.type.MethodVarInstance;

public final class MethodVarClassifier {
	public static void init() {
		addClassifier(TYPE, 10);
		addClassifier(POSITION, 3);
		addClassifier(LV_INDEX, 2);
		addClassifier(USAGE, 8);
	}

	public static void addClassifier(AbstractClassifier classifier, double weight, ClassifierLevel... levels) {
		if (levels.length == 0) levels = ClassifierLevel.ALL;

		classifier.weight = weight;

		for (ClassifierLevel level : levels) {
			CLASSIFIERS.computeIfAbsent(level, ignore -> new ArrayList<>()).add(classifier);
			MAX_SCORE.put(level, getMaxScore(level) + weight);
		}
	}

	public static double getMaxScore(ClassifierLevel level) {
		return MAX_SCORE.getOrDefault(level, 0.);
	}

	public static List<RankResult<MethodVarInstance>> rank(MethodVarInstance src, MethodVarInstance[] dsts, ClassifierLevel level, ClassEnvironment env, double maxMismatch) {
		return ClassifierUtil.rank(src, dsts, CLASSIFIERS.getOrDefault(level, Collections.emptyList()), ClassifierUtil::checkPotentialEquality, env, maxMismatch);
	}

	private static final Map<ClassifierLevel, List<IClassifier<MethodVarInstance>>> CLASSIFIERS = new EnumMap<>(ClassifierLevel.class);
	private static final Map<ClassifierLevel, Double> MAX_SCORE = new EnumMap<>(ClassifierLevel.class);

	private static final AbstractClassifier TYPE = new AbstractClassifier("type") {
		@Override
		public double getScore(MethodVarInstance argA, MethodVarInstance argB, ClassEnvironment env) {
			return ClassifierUtil.checkPotentialEquality(argA.getType(), argB.getType()) ? 1 : 0;
		}
	};

	private static final AbstractClassifier POSITION = new AbstractClassifier("position") {
		@Override
		public double getScore(MethodVarInstance methodA, MethodVarInstance methodB, ClassEnvironment env) {
			return ClassifierUtil.classifyPosition(methodA, methodB,
					MethodVarInstance::getIndex,
					(a, idx) -> (a.isArg() ? a.getMethod().getArg(idx) : a.getMethod().getVar(idx)),
					a -> (a.isArg() ? a.getMethod().getArgs() : a.getMethod().getVars()));
		}
	};

	private static final AbstractClassifier LV_INDEX = new AbstractClassifier("lv index") {
		@Override
		public double getScore(MethodVarInstance argA, MethodVarInstance argB, ClassEnvironment env) {
			return argA.getLvIndex() == argB.getLvIndex() ? 1 : 0;
		}
	};

	private static final AbstractClassifier USAGE = new AbstractClassifier("usage") {
		@Override
		public double getScore(MethodVarInstance argA, MethodVarInstance argB, ClassEnvironment env) {
			int[] map = ClassifierUtil.mapInsns(argA.getMethod(), argB.getMethod());
			if (map == null) return 1;

			InsnList ilA = argA.getMethod().getAsmNode().instructions;
			InsnList ilB = argB.getMethod().getAsmNode().instructions;
			int matched = 0;
			int mismatched = 0;

			for (int srcIdx = 0; srcIdx < map.length; srcIdx++) {
				int dstIdx = map[srcIdx];
				if (dstIdx < 0) continue;

				AbstractInsnNode inA = ilA.get(srcIdx);
				AbstractInsnNode inB = ilB.get(dstIdx);
				int varA, varB;

				if (inA.getType() == AbstractInsnNode.VAR_INSN) {
					varA = ((VarInsnNode) inA).var;
					varB = ((VarInsnNode) inB).var;
				} else if (inA.getType() == AbstractInsnNode.IINC_INSN) {
					varA = ((IincInsnNode) inA).var;
					varB = ((IincInsnNode) inB).var;
				} else {
					continue;
				}

				if (varA == argA.getLvIndex() && (argA.getStartInsn() < 0 || srcIdx >= argA.getStartInsn() && srcIdx < argA.getEndInsn())) {
					if (varB == argB.getLvIndex() && (argB.getStartInsn() < 0 || dstIdx >= argB.getStartInsn() && dstIdx < argB.getEndInsn())) {
						matched++;
					} else {
						mismatched++;
					}
				}
			}

			if (matched == 0 && mismatched == 0) {
				return 1;
			} else {
				return (double) matched / (matched + mismatched);
			}
		}
	};

	public abstract static class AbstractClassifier implements IClassifier<MethodVarInstance> {
		protected AbstractClassifier(String name) {
			this.name = name;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public double getWeight() {
			return weight;
		}

		private final String name;
		private double weight;
	}

	private MethodVarClassifier() {
	}
}
