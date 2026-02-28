module matcher {
	exports matcher.model.config;
	exports matcher.gui.srcprocess;
	exports matcher.gui;
	exports matcher.gui.ui;
	exports matcher.gui.ui.menu;
	exports matcher.gui.ui.tab;
	exports matcher.model.type;
	exports matcher.model;
	exports matcher.model.bcremap;
	exports matcher.model.classifier;
	exports matcher.model.mapping;
	exports matcher.core;
	exports matcher.core.serdes;

	requires transitive org.slf4j;
	requires cfr;
	requires com.github.javaparser.core;
	requires org.vineflower.vineflower;
	requires java.prefs;
	requires transitive javafx.base;
	requires transitive javafx.controls;
	requires transitive javafx.graphics;
	requires transitive javafx.web;
	requires transitive org.objectweb.asm;
	requires transitive org.objectweb.asm.tree;
	requires org.objectweb.asm.commons;
	requires org.objectweb.asm.tree.analysis;
	requires org.objectweb.asm.util;
	requires procyon.compilertools;
	requires jadx.core;
	requires jadx.plugins.java_input;
	requires transitive net.fabricmc.mappingio;
	requires jcommander;
	requires java.net.http;

	uses matcher.core.Plugin;
}
