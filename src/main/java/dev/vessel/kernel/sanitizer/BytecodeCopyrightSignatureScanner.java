package dev.vessel.kernel.sanitizer;

import org.objectweb.asm.ClassReader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Bytecode scanner analyzing compiled Java .class structures to detect embedded copyright notices,
 * license declarations, and potential copyleft contamination signatures.
 * <p>
 * Mitigates: <b>ERR-97</b>, <b>OPS-0351..0500</b>.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class BytecodeCopyrightSignatureScanner {

    private final List<CopyrightSignaturePattern> standardPatterns;

    /**
     * Constructs a scanner with default industry-standard license patterns.
     */
    public BytecodeCopyrightSignatureScanner() {
        List<CopyrightSignaturePattern> patterns = new ArrayList<>();
        patterns.add(new CopyrightSignaturePattern(
                "GPL_3_0_DECLARATION",
                ".*(GNU General Public License.*version 3|GPL-3\\.0|GPLv3).*",
                SoftwareLicenseKind.GPL_3_0,
                true
        ));
        patterns.add(new CopyrightSignaturePattern(
                "GPL_2_0_DECLARATION",
                ".*(GNU General Public License.*version 2|GPL-2\\.0|GPLv2).*",
                SoftwareLicenseKind.GPL_2_0,
                true
        ));
        patterns.add(new CopyrightSignaturePattern(
                "LGPL_DECLARATION",
                ".*(GNU Lesser General Public License|LGPL-2\\.1|LGPL-3\\.0).*",
                SoftwareLicenseKind.LGPL_3_0,
                true
        ));
        patterns.add(new CopyrightSignaturePattern(
                "APACHE_2_0_DECLARATION",
                ".*(Apache License.*2\\.0|Apache-2\\.0).*",
                SoftwareLicenseKind.APACHE_2_0,
                false
        ));
        patterns.add(new CopyrightSignaturePattern(
                "MIT_DECLARATION",
                ".*(MIT License|SPDX-License-Identifier: MIT).*",
                SoftwareLicenseKind.MIT,
                false
        ));
        patterns.add(new CopyrightSignaturePattern(
                "GENERIC_COPYRIGHT",
                ".*(Copyright \\(C\\)|Copyright ©|\\(c\\) 20\\d\\d).*",
                SoftwareLicenseKind.UNKNOWN,
                false
        ));
        this.standardPatterns = Collections.unmodifiableList(patterns);
    }

    /**
     * Scans raw class bytecode to identify embedded copyright notices or copyleft declarations.
     *
     * @param classBytes raw .class file binary content
     * @return audit scan report
     * @throws BytecodeScanException if bytecode cannot be parsed
     */
    public BytecodeCopyrightScanReport scanClassBytecode(byte[] classBytes) {
        return scanClassBytecode(null, classBytes);
    }

    /**
     * Scans raw class bytecode with a known hint class name.
     *
     * @param classNameHint optional class name hint
     * @param classBytes    raw .class file binary content
     * @return audit scan report
     * @throws BytecodeScanException if bytecode cannot be parsed
     */
    public BytecodeCopyrightScanReport scanClassBytecode(String classNameHint, byte[] classBytes) {
        if (classBytes == null || classBytes.length == 0) {
            throw new BytecodeScanException("Class bytecode cannot be null or empty");
        }

        try {
            ClassReader reader = new ClassReader(classBytes);
            List<String> extractedStrings = new ArrayList<>();
            BytecodeStringExtractorClassVisitor visitor = new BytecodeStringExtractorClassVisitor(extractedStrings);
            reader.accept(visitor, 0);

            String targetClassName = (classNameHint != null) ? classNameHint : visitor.getClassName();
            if (targetClassName == null) {
                targetClassName = "UnknownClass";
            }

            List<CopyrightSignatureRecord> matches = new ArrayList<>();
            boolean hasCopyleftThreat = false;

            for (String str : extractedStrings) {
                for (CopyrightSignaturePattern pattern : standardPatterns) {
                    Pattern compiled = Pattern.compile(pattern.regexPattern(), Pattern.CASE_INSENSITIVE);
                    if (compiled.matcher(str).matches()) {
                        CopyrightSignatureRecord record = new CopyrightSignatureRecord(
                                targetClassName,
                                str,
                                pattern.identifier(),
                                pattern.impliedLicense(),
                                pattern.isCopyleftThreat()
                        );
                        matches.add(record);
                        if (pattern.isCopyleftThreat()) {
                            hasCopyleftThreat = true;
                        }
                    }
                }
            }

            return new BytecodeCopyrightScanReport(1, matches, hasCopyleftThreat);
        } catch (Exception ex) {
            throw new BytecodeScanException("Failed to scan bytecode for copyright signatures", ex);
        }
    }

    /**
     * Scans multiple compiled classes and aggregates license findings.
     *
     * @param classesMap mapping of class name to binary bytecode
     * @return aggregate report
     */
    public BytecodeCopyrightScanReport scanMultipleClasses(Map<String, byte[]> classesMap) {
        if (classesMap == null || classesMap.isEmpty()) {
            return new BytecodeCopyrightScanReport(0, List.of(), false);
        }

        List<CopyrightSignatureRecord> allMatches = new ArrayList<>();
        boolean anyCopyleftThreat = false;
        int count = 0;

        for (Map.Entry<String, byte[]> entry : classesMap.entrySet()) {
            BytecodeCopyrightScanReport singleReport = scanClassBytecode(entry.getKey(), entry.getValue());
            allMatches.addAll(singleReport.detectedSignatures());
            if (singleReport.hasCopyleftThreat()) {
                anyCopyleftThreat = true;
            }
            count++;
        }

        return new BytecodeCopyrightScanReport(count, allMatches, anyCopyleftThreat);
    }
}
