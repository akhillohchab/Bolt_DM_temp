//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2013.09.26 at 10:12:59 AM PDT 
//


package com.sri.bolt.xml;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for interruptT.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="interruptT">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="moveon"/>
 *     &lt;enumeration value="bargein"/>
 *     &lt;enumeration value="rollback"/>
 *     &lt;enumeration value="other"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "interruptT")
@XmlEnum
public enum InterruptT {

    @XmlEnumValue("moveon")
    MOVEON("moveon"),
    @XmlEnumValue("bargein")
    BARGEIN("bargein"),
    @XmlEnumValue("rollback")
    ROLLBACK("rollback"),
    @XmlEnumValue("other")
    OTHER("other");
    private final String value;

    InterruptT(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static InterruptT fromValue(String v) {
        for (InterruptT c: InterruptT.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
