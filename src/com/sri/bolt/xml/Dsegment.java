//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2013.09.26 at 10:12:59 AM PDT 
//


package com.sri.bolt.xml;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{}human_turn" minOccurs="0"/>
 *         &lt;element ref="{}clarification" maxOccurs="10" minOccurs="0"/>
 *         &lt;element ref="{}system_turn" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="syscomment" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "humanTurn",
    "clarification",
    "systemTurn"
})
@XmlRootElement(name = "dsegment")
public class Dsegment {

    @XmlElement(name = "human_turn")
    protected HumanTurn humanTurn;
    protected List<Clarification> clarification;
    @XmlElement(name = "system_turn")
    protected SystemTurn systemTurn;
    @XmlAttribute
    protected String syscomment;

    /**
     * Gets the value of the humanTurn property.
     * 
     * @return
     *     possible object is
     *     {@link HumanTurn }
     *     
     */
    public HumanTurn getHumanTurn() {
        return humanTurn;
    }

    /**
     * Sets the value of the humanTurn property.
     * 
     * @param value
     *     allowed object is
     *     {@link HumanTurn }
     *     
     */
    public void setHumanTurn(HumanTurn value) {
        this.humanTurn = value;
    }

    /**
     * Gets the value of the clarification property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the clarification property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getClarification().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Clarification }
     * 
     * 
     */
    public List<Clarification> getClarification() {
        if (clarification == null) {
            clarification = new ArrayList<Clarification>();
        }
        return this.clarification;
    }

    /**
     * Gets the value of the systemTurn property.
     * 
     * @return
     *     possible object is
     *     {@link SystemTurn }
     *     
     */
    public SystemTurn getSystemTurn() {
        return systemTurn;
    }

    /**
     * Sets the value of the systemTurn property.
     * 
     * @param value
     *     allowed object is
     *     {@link SystemTurn }
     *     
     */
    public void setSystemTurn(SystemTurn value) {
        this.systemTurn = value;
    }

    /**
     * Gets the value of the syscomment property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSyscomment() {
        return syscomment;
    }

    /**
     * Sets the value of the syscomment property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSyscomment(String value) {
        this.syscomment = value;
    }

}
