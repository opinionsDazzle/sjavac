/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
/* Contains sources copyright Fredrik Öhrström 2014, 
 * licensed from Fredrik to you under the above license. */
package com.sun.tools.sjavac;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Utilities.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class Util {

    public static String toFileSystemPath(String pkgId) {
        if (pkgId == null || pkgId.length()==0) return null;
        String pn;
        if (pkgId.charAt(0) == ':') {
            // When the module is the default empty module.
            // Do not prepend the module directory, because there is none.
            // Thus :java.foo.bar translates to java/foo/bar (or \)
            pn = pkgId.substring(1).replace('.',File.separatorChar);
        } else {
            // There is a module. Thus jdk.base:java.foo.bar translates
            // into jdk.base/java/foo/bar
            int cp = pkgId.indexOf(':');
            String mn = pkgId.substring(0,cp);
            pn = mn+File.separatorChar+pkgId.substring(cp+1).replace('.',File.separatorChar);
        }
        return pn;
    }

    public static String justPackageName(String pkgName) {
        int c = pkgName.indexOf(":");
        if (c == -1)
            throw new IllegalArgumentException("Expected ':' in package name (" + pkgName + ")");
        return pkgName.substring(c+1);
    }

    public static String extractStringOption(String opName, String s) {
        return extractStringOption(opName, s, null);
    }

    public static String extractStringOption(String opName, String s, String deflt) {
        if (s == null) return deflt;
        int p = s.indexOf(opName+"=");
        if (p == -1) return deflt;
        p+=opName.length()+1;
        int pe = s.indexOf(',', p);
        if (pe == -1) pe = s.length();
        return s.substring(p, pe);
    }

    public static boolean extractBooleanOption(String opName, String s, boolean deflt) {
       String str = extractStringOption(opName, s);
        return "true".equals(str) ? true
             : "false".equals(str) ? false
             : deflt;
    }

    public static int extractIntOption(String opName, String s) {
        return extractIntOption(opName, s, 0);
    }

    public static int extractIntOption(String opName, String s, int deflt) {
        if (s == null) return deflt;
        int p = s.indexOf(opName+"=");
        if (p == -1) return deflt;
        p+=opName.length()+1;
        int pe = s.indexOf(',', p);
        if (pe == -1) pe = s.length();
        int v = 0;
        try {
            v = Integer.parseInt(s.substring(p, pe));
        } catch (Exception e) {}
        return v;
    }

    /**
     * Clean out unwanted sub options supplied inside a primary option.
     * For example to only had portfile remaining from:
     *    settings="-server:id=foo,portfile=bar"
     * do settings = cleanOptions("-server:",Util.set("-portfile"),settings);
     *    now settings equals "-server:portfile=bar"
     *
     * @param allowsSubOptions A set of the allowed sub options, id portfile etc.
     * @param s The option settings string.
     */
    public static String cleanSubOptions(Set<String> allowedSubOptions, String s) {
        StringBuilder sb = new StringBuilder();
        StringTokenizer st = new StringTokenizer(s, ",");
        while (st.hasMoreTokens()) {
            String o = st.nextToken();
            int p = o.indexOf('=');
            if (p>0) {
                String key = o.substring(0,p);
                String val = o.substring(p+1);
                if (allowedSubOptions.contains(key)) {
                    if (sb.length() > 0) sb.append(',');
                    sb.append(key+"="+val);
                }
            }
        }
        return sb.toString();
    }

    /**
     * Convenience method to create a set with strings.
     */
    public static Set<String> set(String... ss) {
        Set<String> set = new HashSet<>();
        set.addAll(Arrays.asList(ss));
        return set;
    }

    /**
     * Normalize windows drive letter paths to upper case to enable string
     * comparison.
     *
     * @param file File name to normalize
     * @return The normalized string if file has a drive letter at the beginning,
     *         otherwise the original string.
     */
    public static String normalizeDriveLetter(String file) {
        if (file.length() > 2 && file.charAt(1) == ':') {
            return Character.toUpperCase(file.charAt(0)) + file.substring(1);
        } else if (file.length() > 3 && file.charAt(0) == '*'
                   && file.charAt(2) == ':') {
            // Handle a wildcard * at the beginning of the string.
            return file.substring(0, 1) + Character.toUpperCase(file.charAt(1))
                   + file.substring(2);
        }
        return file;
    }

    /**
     * Locate the setting for the server properties.
     */
    public static String findServerSettings(String[] args) {
        for (String s : args) {
            if (s.startsWith("--server:")) {
                return s;
            }
        }
        return null;
    }

    // TODO: Remove when refactoring from java.io.File to java.nio.file.Path.
    public static File pathToFile(Path path) {
        return path == null ? null : path.toFile();
    }

    /**
     * Extract the jar/zip/cym archive file name from a classfile tosttring.
     */
    public static String extractArchive(String c) {
        // Example:
        // ZipFileIndexFileObject[/home/fredrik/bin/jdk1.8.0/lib/ct.sym(META-INF/sym/rt.jar/java/lang/Runnable.class)]
        if (c.startsWith("ZipFileIndexFileObject[")) {
            int p = c.indexOf('(', 23);
            if (p == -1) {
                return null;
            }
            return c.substring(23, p);
        }
        return null;
    }

    /**
     * Utility to add to a Map<String,Set<String>>
     */
    public static void addToMapSet(String k, String v, Map<String,Set<String>> m) {
        Set<String> set = m.get(k);
        if (set == null) {
            set = new HashSet<String>();
            m.put(k, set);
        }
        set.add(v);
    }

    /**
     * Utility to add to a Map<String,List<String>>
     */
    public static void addToMapList(String k, String v, Map<String,List<String>> m) {
        List<String> list = m.get(k);
        if (list == null) {
            list = new ArrayList<String>();
            m.put(k, list);
        }
        list.add(v);
    }

        
}
