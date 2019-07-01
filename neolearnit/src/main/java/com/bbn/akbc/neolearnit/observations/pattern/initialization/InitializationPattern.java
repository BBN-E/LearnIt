package com.bbn.akbc.neolearnit.observations.pattern.initialization;

import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.util.Set;

/**
 * Created by mcrivaro on 8/1/2014.
 */
public abstract class InitializationPattern {
    public abstract boolean matchesPattern(LearnitPattern p);

    protected final Target target;

    protected InitializationPattern(Target t) {
        target = t;
    }

    public static InitializationPattern from(String s, Target t) {
        if (s.charAt(0) == '<')
            return new InitialRegexPattern(s, t);
        else
            return new InitialPropPattern(s, t);
    }

    public static Set<InitializationPattern> getFromFile(File patternFile, Target t) throws IOException {
        ImmutableSet.Builder<InitializationPattern> builder = ImmutableSet.builder();
        for (String line : Files.readLines(patternFile, Charsets.UTF_8)) {
            if (line.length() > 2) {
                builder.add(InitializationPattern.from(line.trim(), t));
            }
        }
        System.out.println(builder.build());
        return builder.build();
    }
}
