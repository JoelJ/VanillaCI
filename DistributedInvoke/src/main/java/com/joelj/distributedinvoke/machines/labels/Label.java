package com.joelj.distributedinvoke.machines.labels;

import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.List;

/**
 * Used to give arbitrary metadata to machines and jobs.
 * Intended to be a way to run jobs only on matching machines,
 * however, it will be completely dependant on what various plugins
 * do with this data that really define the behavior of the Label.
 *
 * User: Joel Johnson
 * Date: 3/4/13
 * Time: 7:25 PM
 */
public class Label implements Serializable {
	@NotNull
	public static Expression parse(@NotNull String expression) {
		ImmutableList.Builder<Label> includes = ImmutableList.builder();
		ImmutableList.Builder<Label> excludes = ImmutableList.builder();

		String[] split = expression.split("\\s+");
		if(split != null && split.length > 0) {
			for (String labelString : split) {
				if(labelString == null || labelString.isEmpty()) {
					//Split can have some weird behavior. So make sure we never get bit by that.
					//noinspection UnnecessaryContinue
					continue;
				} else if(labelString.startsWith("!")) {
					Label label = new Label(labelString.substring(1));
					excludes.add(label);
				} else {
					Label label = new Label(labelString);
					includes.add(label);
				}
			}
		}
		return new Expression(includes.build(), excludes.build());
	}

	@NotNull
	private final String name;

	private Label(@NotNull String name) {
		this.name = name;
	}

	@NotNull
	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return "Label{" +
				"name='" + name + '\'' +
				'}';
	}

	public static class Expression {
		@NotNull
		private final List<Label> includes;

		@NotNull
		private final List<Label> excludes;

		private Expression(@NotNull List<Label> includes, @NotNull List<Label> excludes) {
			this.includes = ImmutableList.copyOf(includes);
			this.excludes = ImmutableList.copyOf(excludes);
		}

		@NotNull
		public List<Label> getIncludes() {
			return includes;
		}

		@NotNull
		public List<Label> getExcludes() {
			return excludes;
		}

		public boolean matches(@NotNull String labelStr) {
			for (Label exclude : excludes) {
				if(exclude.getName().equals(labelStr)) {
					return false;
				}
			}

			if(includes.isEmpty()) {
				return true; //We either have nothing or only excludes
			}
			for (Label include : includes) {
				if(include.getName().equals(labelStr)) {
					return true;
				}
			}

			return false;
		}

		@Override
		public String toString() {
			return "Expression{" +
					"includes=" + includes +
					", excludes=" + excludes +
					'}';
		}
	}
}
