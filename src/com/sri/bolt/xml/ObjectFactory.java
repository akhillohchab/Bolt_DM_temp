//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2013.09.26 at 10:12:59 AM PDT 
//


package com.sri.bolt.xml;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the generated package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _TextTSpeech_QNAME = new QName("", "speech");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: generated
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link SystemTurn }
     * 
     */
    public SystemTurn createSystemTurn() {
        return new SystemTurn();
    }

    /**
     * Create an instance of {@link Clarification }
     * 
     */
    public Clarification createClarification() {
        return new Clarification();
    }

    /**
     * Create an instance of {@link AudioT }
     * 
     */
    public AudioT createAudioT() {
        return new AudioT();
    }

    /**
     * Create an instance of {@link TextT }
     * 
     */
    public TextT createTextT() {
        return new TextT();
    }

    /**
     * Create an instance of {@link Dsegment }
     * 
     */
    public Dsegment createDsegment() {
        return new Dsegment();
    }

    /**
     * Create an instance of {@link Logfile }
     * 
     */
    public Logfile createLogfile() {
        return new Logfile();
    }

    /**
     * Create an instance of {@link HumanTurn }
     * 
     */
    public HumanTurn createHumanTurn() {
        return new HumanTurn();
    }

    /**
     * Create an instance of {@link javax.xml.bind.JAXBElement }{@code <}{@link AudioT }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "speech", scope = TextT.class)
    public JAXBElement<AudioT> createTextTSpeech(AudioT value) {
        return new JAXBElement<AudioT>(_TextTSpeech_QNAME, AudioT.class, TextT.class, value);
    }

}
