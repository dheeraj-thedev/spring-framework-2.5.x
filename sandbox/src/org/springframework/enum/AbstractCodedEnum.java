/*
 * Copyright 2002-2004 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.springframework.enum;

import java.io.Serializable;
import java.util.Comparator;

import org.springframework.context.MessageSourceResolvable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.comparators.CompoundComparator;
import org.springframework.util.comparators.NullSafeComparator;

/**
 * Abstract base superclass for CodedEnum implementations.
 * 
 * @author Keith Donald
 */
public abstract class AbstractCodedEnum implements CodedEnum,
        MessageSourceResolvable, Serializable, Comparable {
    private Comparable code;

    private String label;

    /**
     * Comparator that sorts enumerations by <code>LABEL_ORDER</code>
     */
    public static final Comparator LABEL_ORDER = new Comparator() {
        public int compare(Object o1, Object o2) {
            AbstractCodedEnum e1 = (AbstractCodedEnum)o1;
            AbstractCodedEnum e2 = (AbstractCodedEnum)o2;
            Comparator c = new NullSafeComparator(String.CASE_INSENSITIVE_ORDER);
            return c.compare(e1.getLabel(), e2.getLabel());
        }
    };

    /**
     * Comparator that sorts enumerations by <code>LABEL_ORDER</code>, then
     * natural order.
     */
    public static final Comparator DEFAULT_ORDER = new CompoundComparator(
            new Comparator[] { LABEL_ORDER, LABEL_ORDER });

    protected AbstractCodedEnum(Comparable code) {
        this(code, null);
    }

    protected AbstractCodedEnum(Comparable code, String label) {
        Assert.notNull(code);
        this.code = code;
        this.label = label;
    }

    public Object getCode() {
        return code;
    }

    public String getKey() {
        return getType() + "." + getCode();
    }

    public String getLabel() {
        return label;
    }

    public boolean equals(Object o) {
        if (!(o instanceof AbstractCodedEnum)) { return false; }
        AbstractCodedEnum e = (AbstractCodedEnum)o;
        return this.code.equals(e.code) && this.getType().equals(e.getType());
    }

    public int compareTo(Object o) {
        AbstractCodedEnum e = (AbstractCodedEnum)o;
        Assert.isTrue(getType().equals(e.getType()),
                "You may only compare enumerations of the same type.");
        return code.compareTo(e.code);
    }

    public int hashCode() {
        return code.hashCode() + getType().hashCode();
    }

    /**
     * The enumeration key is used as the message key for internationalizing
     * enums.
     * 
     * @see org.springframework.context.MessageSourceResolvable#getCodes()
     */
    public String[] getCodes() {
        return new String[] { getKey() };
    }

    public Object[] getArguments() {
        return null;
    }

    public String getType() {
        return ClassUtils.getShortNameAsProperty(getClass());
    }

    public String getDefaultMessage() {
        if (label != null) {
            return getLabel();
        }
        else {
            return String.valueOf(getCode());
        }
    }

    public String toString() {
        return getKey();
    }

}