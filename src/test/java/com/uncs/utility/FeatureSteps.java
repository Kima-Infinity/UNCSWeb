package com.uncs.utility;

import io.cucumber.java.Scenario;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The steps of a scenario, read from the feature file it came from.
 *
 * The obvious way to do this was to listen for Cucumber's TestStepFinished events, which
 * carry each step's text and how it went. That does not work: Cucumber emits every one of
 * them after the After hooks have run, so a hook building a failure report always sees an
 * empty list. It was worth finding out rather than assuming - the events arrive, just too
 * late to be of any use to the thing that needs them.
 *
 * So the steps come from the source instead. That loses which step failed, which the
 * exception says anyway, and gains something better for the reader: the steps exactly as a
 * person would follow them, with an outline's placeholders filled in from the example row
 * that actually ran.
 */
public final class FeatureSteps {

	/** Words that start a step. Anything else ends the scenario. */
	private static final List<String> KEYWORDS =
			List.of("Given ", "When ", "Then ", "And ", "But ", "* ");

	private FeatureSteps() {
	}

	/**
	 * The steps that make up {@code scenario}, in order, ready to be followed.
	 *
	 * @return the steps, or an empty list if the feature file cannot be read
	 */
	public static List<String> of(Scenario scenario) {

		try {
			Path feature = Paths.get(scenario.getUri());
			List<String> lines = Files.readAllLines(feature);

			// getLine() is the line that was run: the scenario's own line, or for an outline
			// the example row. Either way the steps are above it or just below the scenario
			// heading, so find the heading first.
			int ranAt = scenario.getLine() - 1;
			int heading = headingAbove(lines, ranAt);

			if (heading < 0) {
				return List.of();
			}

			List<String> steps = stepsBelow(lines, heading);

			Map<String, String> example = exampleValues(lines, heading, ranAt);

			if (example.isEmpty()) {
				return steps;
			}

			List<String> filled = new ArrayList<>();

			for (String step : steps) {
				String text = step;
				for (Map.Entry<String, String> cell : example.entrySet()) {
					text = text.replace("<" + cell.getKey() + ">", cell.getValue());
				}
				filled.add(text);
			}

			return filled;

		} catch (Exception cannotRead) {
			System.out.println("Could not read the steps from the feature file: " + cannotRead.getMessage());
			return List.of();
		}
	}

	/** The Scenario or Scenario Outline line at or above {@code from}. */
	private static int headingAbove(List<String> lines, int from) {

		for (int i = Math.min(from, lines.size() - 1); i >= 0; i--) {
			String line = lines.get(i).trim();
			if (line.startsWith("Scenario:") || line.startsWith("Scenario Outline:")) {
				return i;
			}
		}

		return -1;
	}

	/** The step lines under a heading, stopping at Examples or the next scenario. */
	private static List<String> stepsBelow(List<String> lines, int heading) {

		List<String> steps = new ArrayList<>();

		for (int i = heading + 1; i < lines.size(); i++) {
			String line = lines.get(i).trim();

			if (line.startsWith("Scenario") || line.startsWith("Examples:") || line.startsWith("@")) {
				break;
			}

			if (KEYWORDS.stream().anyMatch(line::startsWith)) {
				steps.add(line);
			}
		}

		return steps;
	}

	/**
	 * The example row that was run, as column name to value.
	 *
	 * Empty for a plain scenario, which is the signal to leave the steps as they are.
	 */
	private static Map<String, String> exampleValues(List<String> lines, int heading, int ranAt) {

		Map<String, String> values = new LinkedHashMap<>();

		if (ranAt <= heading || ranAt >= lines.size()) {
			return values;
		}

		String row = lines.get(ranAt).trim();

		if (!row.startsWith("|")) {
			return values;
		}

		// The header is the first table row under Examples, above the row that ran.
		int headerLine = -1;

		for (int i = ranAt - 1; i > heading; i--) {
			String line = lines.get(i).trim();
			if (line.startsWith("Examples:")) {
				break;
			}
			if (line.startsWith("|")) {
				headerLine = i;
			}
		}

		if (headerLine < 0) {
			return values;
		}

		String[] names = cells(lines.get(headerLine));
		String[] cells = cells(row);

		for (int i = 0; i < names.length && i < cells.length; i++) {
			values.put(names[i], cells[i]);
		}

		return values;
	}

	private static String[] cells(String tableRow) {

		String trimmed = tableRow.trim();

		if (trimmed.startsWith("|")) {
			trimmed = trimmed.substring(1);
		}
		if (trimmed.endsWith("|")) {
			trimmed = trimmed.substring(0, trimmed.length() - 1);
		}

		String[] cells = trimmed.split("\\|");

		for (int i = 0; i < cells.length; i++) {
			cells[i] = cells[i].trim();
		}

		return cells;
	}
}
