package matcher.mapping;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import net.fabricmc.mappingio.tree.HierarchyInfoProvider;
import net.fabricmc.mappingio.tree.MappingTreeView;
import net.fabricmc.mappingio.tree.MappingTreeView.MethodMappingView;

import matcher.Util;
import matcher.mapping.Matcher2MioHierarchyProvider.HierarchyData;
import matcher.type.ClassEnv;
import matcher.type.ClassInstance;
import matcher.type.FieldInstance;
import matcher.type.LocalClassEnv;
import matcher.type.MethodInstance;
import matcher.type.MethodVarInstance;

public class Matcher2MioHierarchyProvider implements HierarchyInfoProvider<HierarchyData> {
	public Matcher2MioHierarchyProvider(LocalClassEnv env, String namespace) {
		this.env = env;
		this.namespace = namespace;
	}

	@Override
	public String getNamespace() {
		return namespace;
	}

	@Override
	public String resolveField(String owner, String name, String desc) {
		ClassInstance cls = env.getClsByName(owner);
		if (cls == null) return null;

		FieldInstance field = cls.resolveField(name, desc);

		return field != null ? field.getOwner().getName() : null;
	}

	@Override
	public String resolveMethod(String owner, String name, String desc) {
		ClassInstance cls = env.getClsByName(owner);
		if (cls == null) return null;

		MethodInstance method = cls.resolveMethod(name, desc, cls.isInterface());

		return method != null ? method.getOwner().getName() : null;
	}

	@Override
	public HierarchyData getMethodHierarchy(String owner, String name, String desc) {
		ClassInstance cls = env.getClsByName(owner);
		if (cls == null) return null;

		MethodInstance method = cls.resolveMethod(name, desc, cls.isInterface());
		if (method == null) return null;

		assert !method.isStatic() || method.getAllHierarchyMembers().size() == 1;

		return getMethodHierarchy0(method);
	}

	private HierarchyData getMethodHierarchy0(MethodInstance method) {
		Set<MethodInstance> immediateHierarchy = method.getAllHierarchyMembers();
		Queue<Set<MethodInstance>> toCheck = new ArrayDeque<>(immediateHierarchy.size());
		Set<Set<MethodInstance>> checked = new HashSet<>();
		toCheck.add(immediateHierarchy);

		do {
			Set<MethodInstance> currentHierarchy;

			while ((currentHierarchy = toCheck.poll()) != null) {
				if (checked.contains(currentHierarchy)) {
					continue;
				}

				for (MethodInstance m : currentHierarchy) {
					if (m.isSynthetic()) {
						Set<MethodInstance> targets = m.getRefsOut();

						for (MethodInstance target : targets) {
							if (isPotentialBridge(m, target)) {
								toCheck.add(target.getAllHierarchyMembers());
							}
						}
					}

					Set<MethodInstance> sources = findSyntheticSources(m);

					for (MethodInstance source : sources) {
						if (isPotentialBridge(source, m)) {
							toCheck.add(source.getAllHierarchyMembers());
						}
					}
				}

				checked.add(currentHierarchy);
			}
		} while (!toCheck.isEmpty());

		Set<MethodInstance> fullHierarchy = Util.newIdentityHashSet();

		for (Set<MethodInstance> hierarchy : checked) {
			fullHierarchy.addAll(hierarchy);
		}

		return new HierarchyData(fullHierarchy);
	}

	private Set<MethodInstance> findSyntheticSources(MethodInstance method) {
		Set<MethodInstance> sources = Collections.emptySet();

		for (MethodInstance refIn : method.getRefsIn()) {
			if (refIn.isSynthetic()) {
				if (sources.isEmpty()) {
					sources = new HashSet<>();
				}

				sources.add(refIn);
			}
		}

		return sources;
	}

	private boolean isPotentialBridge(MethodInstance bridgeMethod, MethodInstance bridgedMethod) {
		if (!bridgeMethod.isSynthetic()) return false;

		if (bridgeMethod.isPrivate() || bridgeMethod.isFinal() || bridgeMethod.isStatic()) {
			return false;
		}

		// Bridge method's target must be in the same class or in a parent class
		if (!bridgedMethod.getOwner().isAssignableFrom(bridgeMethod.getOwner())) {
			return false;
		}

		MethodVarInstance[] bridgeParams = bridgeMethod.getArgs();
		MethodVarInstance[] bridgedParams = bridgedMethod.getArgs();

		if (bridgeParams.length != bridgedParams.length) {
			return false;
		}

		for (int i = 0; i < bridgeParams.length; i++) {
			if (!areTypesBridgeCompatible(bridgeParams[i].getType(), bridgedParams[i].getType())) {
				return false;
			}
		}

		return areTypesBridgeCompatible(bridgeMethod.getRetType(), bridgedMethod.getRetType());
	}

	private boolean areTypesBridgeCompatible(ClassInstance bridgeType, ClassInstance bridgedType) {
		if (bridgeType.equals(bridgedType)) {
			return true;
		}

		boolean bridgedExtendsBridge = bridgeType.isAssignableFrom(bridgedType);

		// If not equal, types in bridge method descriptor should always be less specific than in the bridged method
		assert bridgedExtendsBridge || !bridgedType.isAssignableFrom(bridgeType);

		return bridgedExtendsBridge;
	}

	@Override
	public int getHierarchySize(HierarchyData hierarchy) {
		return hierarchy != null ? hierarchy.methods.size() : 0;
	}

	@Override
	public Collection<? extends MethodMappingView> getHierarchyMethods(HierarchyData hierarchy, MappingTreeView tree) {
		if (hierarchy == null) return Collections.emptyList();

		List<MethodMappingView> ret = new ArrayList<>(hierarchy.methods.size());
		int ns = tree.getNamespaceId(namespace);
		assert ns != MappingTreeView.NULL_NAMESPACE_ID;

		for (MethodInstance method : hierarchy.methods) {
			MethodMappingView m = tree.getMethod(method.getOwner().getName(), method.getName(), method.getDesc(), ns);
			if (m != null) ret.add(m);
		}

		return ret;
	}

	public static final class HierarchyData {
		HierarchyData(Collection<MethodInstance> methods) {
			this.methods = methods;
		}

		final Collection<MethodInstance> methods;
	}

	private final ClassEnv env;
	private final String namespace;
}
