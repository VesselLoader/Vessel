package dev.vessel.kernel.sanitizer;

/**
 * Categorization of open source and proprietary software licenses
 * for automated compliance and clean-room isolation auditing.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public enum SoftwareLicenseKind {

    /** MIT permissive license. */
    MIT,

    /** Apache License 2.0 permissive license. */
    APACHE_2_0,

    /** BSD 2-Clause Simplified License. */
    BSD_2_CLAUSE,

    /** BSD 3-Clause Revised License. */
    BSD_3_CLAUSE,

    /** GNU Lesser General Public License v2.1 (Weak Copyleft). */
    LGPL_2_1,

    /** GNU Lesser General Public License v3.0 (Weak Copyleft). */
    LGPL_3_0,

    /** GNU General Public License v2.0 (Strong Copyleft). */
    GPL_2_0,

    /** GNU General Public License v3.0 (Strong Copyleft). */
    GPL_3_0,

    /** GNU Affero General Public License v3.0 (Network Copyleft). */
    AGPL_3_0,

    /** Closed-source proprietary or commercial license. */
    PROPRIETARY,

    /** Unrecognized or undeclared license. */
    UNKNOWN;

    /**
     * Checks if this license belongs to the permissive family (e.g. MIT, Apache, BSD).
     *
     * @return true if permissive
     */
    public boolean isPermissive() {
        return this == MIT || this == APACHE_2_0 || this == BSD_2_CLAUSE || this == BSD_3_CLAUSE;
    }

    /**
     * Checks if this license imposes copyleft requirements on derivative works.
     *
     * @return true if copyleft (weak or strong)
     */
    public boolean isCopyleft() {
        return isWeakCopyleft() || isStrongCopyleft();
    }

    /**
     * Checks if this license is a weak copyleft license (e.g. LGPL) allowing dynamic linking.
     *
     * @return true if weak copyleft
     */
    public boolean isWeakCopyleft() {
        return this == LGPL_2_1 || this == LGPL_3_0;
    }

    /**
     * Checks if this license is a strong copyleft license (e.g. GPL, AGPL) requiring entire works to be GPL.
     *
     * @return true if strong copyleft
     */
    public boolean isStrongCopyleft() {
        return this == GPL_2_0 || this == GPL_3_0 || this == AGPL_3_0;
    }
}
