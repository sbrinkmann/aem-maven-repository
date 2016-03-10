package io.brinkmann.aem.maven.model;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Splits up a maven version into the parts major, minor and fixlevel version.
 * This makes it possible to compare the _numbers_ with other MavenVersion instances.
 * In contrast to a pure string comparison of the version number, this class detects
 * correctly that a version 1.6 is smaller then the version 1.10
 *
 * Created by sbrinkmann on 10.03.16.
 */
public class MavenVersion implements Comparable<MavenVersion> {

    private Pattern versionNumberRegexPattern = Pattern.compile("(\\d+)(\\.{1}(\\d+)){0,1}(\\.{1}(\\d+)){0,1}.*");

    private String rawVersion;

    private boolean hasVersion;

    private int majorVersion = 0;

    private int minorVersion = 0;

    private int fixVersion = 0;

    public MavenVersion() {

    }

    public MavenVersion(String rawVersion) {
        this.rawVersion = rawVersion;
        processVersioNumber();
    }

    private void processVersioNumber() {
        hasVersion = StringUtils.isNotEmpty(rawVersion);

        if (!hasVersion)
            return;

        Matcher versionNumberMatcher = versionNumberRegexPattern.matcher(rawVersion);

        if (versionNumberMatcher.matches()) {
            if (versionNumberMatcher.groupCount() >= 1) {
                String stringMajorVersion = versionNumberMatcher.group(1);
                if (stringMajorVersion != null) {
                    majorVersion = Integer.valueOf(stringMajorVersion);
                }
            }

            if (versionNumberMatcher.groupCount() >= 3) {
                String stringMinorVersion = versionNumberMatcher.group(3);
                if (stringMinorVersion != null) {
                    minorVersion = Integer.valueOf(stringMinorVersion);
                }
            }

            if (versionNumberMatcher.groupCount() >= 5) {
                String stringFixVersion = versionNumberMatcher.group(5);
                if (stringFixVersion != null) {
                    fixVersion = Integer.valueOf(stringFixVersion);
                }
            }
        }
    }

    public int getFixVersion() {
        return fixVersion;
    }

    public int getMinorVersion() {
        return minorVersion;
    }

    public int getMajorVersion() {
        return majorVersion;
    }

    public String getRawVersion() {
        return rawVersion;
    }

    @Override
    public int compareTo(MavenVersion otherMavenVersion) {
        int result = this.majorVersion - otherMavenVersion.majorVersion;
        if (result == 0) {
            result = this.minorVersion - otherMavenVersion.minorVersion;
        }
        if (result == 0) {
            result = this.fixVersion - otherMavenVersion.fixVersion;
        }
        return result;
    }
}
