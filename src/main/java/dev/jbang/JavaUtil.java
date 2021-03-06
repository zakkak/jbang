package dev.jbang;

import static java.lang.System.getenv;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavaUtil {

	public static final int DEFAULT_JAVA_VERSION = 11;

	private static Integer javaVersion;

	/**
	 * Returns the actual Java version that's going to be used. It either returns
	 * the value of the expected version if it is supplied or the version of the JDK
	 * available in the environment (either JAVA_HOME or what's available on the
	 * PATH)
	 * 
	 * @param requestedVersion The Java version requested by the user
	 * @return The Java version that will be used
	 */
	public static int javaVersion(String requestedVersion) {
		int currentVersion = determineJavaVersion();
		if (requestedVersion != null) {
			if (JavaUtil.satisfiesRequestedVersion(requestedVersion, currentVersion)) {
				return currentVersion;
			} else {
				return JavaUtil.minRequestedVersion(requestedVersion);
			}
		} else {
			if (currentVersion < 8) {
				return DEFAULT_JAVA_VERSION;
			} else {
				return currentVersion;
			}
		}
	}

	/**
	 * Determine the Java version that's found in either the folder pointed at by
	 * JAVA_HOME or on the PATH. In all other cases it's assumed that we know what
	 * the version is because it would be a JDK installed by us. The result of this
	 * call is cached so it can be called multiple times without having to worry
	 * about efficiency.
	 * 
	 * @return The detected Java version or 0 if it couldn't be determined
	 */
	public static int determineJavaVersion() {
		if (javaVersion == null) {
			Path jdkHome = getJdkHome();
			Path javaCmd;
			if (jdkHome != null) {
				javaCmd = jdkHome.resolve("bin").resolve("java").toAbsolutePath();
			} else {
				javaCmd = Paths.get("java");
			}
			javaVersion = determineJavaVersion(javaCmd);
			if (javaVersion != 0) {
				Util.verboseMsg("System Java version detected as " + javaVersion);
			} else {
				Util.verboseMsg("No Java found on the system");
			}
		}
		return javaVersion;
	}

	private static int determineJavaVersion(Path javaCmd) {
		String output = Util.runCommand(javaCmd.toString(), "-version");
		int version = parseJavaOutput(output);
		if (version == 0) {
			version = parseJavaVersion(System.getProperty("java.version"));
		}
		return version;
	}

	/**
	 * Returns the Path to JAVA_HOME
	 * 
	 * @return A Path pointing to JAVA_HOME or null if it isn't defined
	 */
	public static Path getJdkHome() {
		if (getenv("JAVA_HOME") != null) {
			return Paths.get(getenv("JAVA_HOME"));
		} else {
			return null;
		}
	}

	private static final Pattern javaVersionPattern = Pattern.compile("\"([^\"]+)\"");

	private static int parseJavaOutput(String output) {
		if (output != null) {
			Matcher m = javaVersionPattern.matcher(output);
			if (m.find() && m.groupCount() == 2) {
				return parseJavaVersion(m.group(1));
			}
		}
		return 0;
	}

	public static int parseJavaVersion(String version) {
		if (version != null) {
			try {
				String[] nums = version.split("[.-]");
				String num = nums.length > 1 && nums[0].equals("1") ? nums[1] : nums[0];
				return Integer.parseInt(num);
			} catch (NumberFormatException ex) {
				Util.verboseMsg("Couldn't parse Java version", ex);
				// Ignore
			}
		}
		return 0;
	}

	public static boolean satisfiesRequestedVersion(String rv, int v) {
		if (rv == null) {
			return true;
		}
		int reqVer = minRequestedVersion(rv);
		if (rv.endsWith("+")) {
			return v >= reqVer;
		} else {
			return v == reqVer;
		}
	}

	private static int minRequestedVersion(String rv) {
		return Integer.parseInt(rv.endsWith("+") ? rv.substring(0, rv.length() - 1) : rv);
	}

}
