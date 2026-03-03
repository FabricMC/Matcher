package matcher.gui.ui.tab;

import java.util.Set;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import matcher.gui.MatcherGui;
import matcher.gui.srcprocess.HtmlUtil;
import matcher.gui.srcprocess.SrcDecorator;
import matcher.gui.srcprocess.SrcDecorator.SrcParseException;
import matcher.gui.ui.ISelectionProvider;
import matcher.model.NameType;
import matcher.model.Util;
import matcher.model.type.ClassInstance;
import matcher.model.type.Decompiler;
import matcher.model.type.FieldInstance;
import matcher.model.type.MatchType;
import matcher.model.type.MemberInstance;
import matcher.model.type.MethodInstance;

public class SourcecodeTab extends WebViewTab {
	public SourcecodeTab(MatcherGui gui, ISelectionProvider selectionProvider, boolean unmatchedTmp) {
		super("source", "ui/templates/CodeViewTemplate.htm");

		this.gui = gui;
		this.selectionProvider = selectionProvider;
		this.unmatchedTmp = unmatchedTmp;

		update();
	}

	@Override
	public void onSelectStateChange(boolean tabSelected) {
		this.tabSelected = tabSelected;
		if (!tabSelected) return;

		if (updateNeeded != UPDATE_NONE) update();

		if (selectedMember instanceof MethodInstance method) {
			onMethodSelect(method);
		} else if (selectedMember instanceof FieldInstance field) {
			onFieldSelect(field);
		}
	}

	@Override
	public void onClassSelect(ClassInstance cls) {
		selectedClass = cls;
		if (updateNeeded == UPDATE_NONE) updateNeeded = UPDATE_RESET;
		if (tabSelected) update();
	}

	@Override
	public void onMatchChange(Set<MatchType> types) {
		selectedClass = selectionProvider.getSelectedClass();
		updateNeeded = UPDATE_REFRESH;

		if (tabSelected && selectedClass != null) {
			update();
		}
	}

	@Override
	public void onViewChange(ViewChangeCause cause) {
		selectedClass = selectionProvider.getSelectedClass();

		if (cause == ViewChangeCause.THEME_CHANGED) {
			// Update immediately to prevent flashes when switching
			update();
		} else if (selectedClass != null
				&& (cause == ViewChangeCause.NAME_TYPE_CHANGED
				|| cause == ViewChangeCause.DECOMPILER_CHANGED)) {
			updateNeeded = UPDATE_REFRESH;
			if (tabSelected) update();
		}
	}

	private void update() {
		cancelWebViewTasks();

		final int cDecompId = ++decompId;

		if (pendingUpdateTask != null) {
			pendingUpdateTask.cancel(true);
			pendingUpdateTask = null;
		}

		if (selectedClass == null) {
			displayText("no class selected");
			return;
		}

		displayText("decompiling...");

		NameType nameType = gui.getNameType().withUnmatchedTmp(unmatchedTmp);
		Decompiler decompiler = gui.getDecompiler().get();

		// Gui.runAsyncTask(() -> gui.getEnv().decompile(selectedClass, true))
		pendingUpdateTask = MatcherGui.runAsyncTask(() -> SrcDecorator.decorate(gui.getEnv().decompile(decompiler, selectedClass, nameType), selectedClass, nameType))
				.whenComplete((res, exc) -> applyDecompilerResult(res, exc, cDecompId));
	}

	private void applyDecompilerResult(String res, Throwable exc, int cDecompId) {
		if (cDecompId != decompId) {
			if (exc != null) {
				if (exc instanceof SrcParseException) {
					LOGGER.debug("parse error (old task, ignored)", exc);
				} else {
					LOGGER.debug("decompile error (old task, ignored)", exc);
				}
			}

			return;
		}

		if (exc != null) {
			if (exc instanceof SrcParseException parseExc) {
				LOGGER.debug("parse error", exc);
				displayText("parse error: " + parseExc.problems + "\ndecompiled source:\n" + parseExc.source);
			} else {
				LOGGER.error("decompile error", exc);
				displayText("decompile error: " + Util.getStackTrace(exc));
			}
		} else {
			double prevScroll = updateNeeded == UPDATE_REFRESH ? getScrollTop() : 0;

			displayHtml(res);

			if (updateNeeded == UPDATE_REFRESH && prevScroll > 0) {
				setScrollTop(prevScroll);
			}
		}

		updateNeeded = UPDATE_NONE;
	}

	@Override
	public void onMethodSelect(MethodInstance method) {
		selectedMember = method;

		if (tabSelected && method != null) {
			select(HtmlUtil.getId(method));
		}
	}

	@Override
	public void onFieldSelect(FieldInstance field) {
		selectedMember = field;

		if (tabSelected && field != null) {
			select(HtmlUtil.getId(field));
		}
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(SourcecodeTab.class);
	private static final int UPDATE_NONE = 0;
	private static final int UPDATE_RESET = 1;
	private static final int UPDATE_REFRESH = 2; // tries to keep scroll position

	private final MatcherGui gui;
	private final ISelectionProvider selectionProvider;
	private final boolean unmatchedTmp;

	private int decompId;
	private int updateNeeded = UPDATE_NONE;
	private boolean tabSelected;
	private ClassInstance selectedClass;
	private MemberInstance<?> selectedMember;
	private Future<?> pendingUpdateTask;
}
