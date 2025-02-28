//
// This file was generated by the Eclipse Implementation of JAXB, v2.3.7 
// See https://eclipse-ee4j.github.io/jaxb-ri 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2024.08.01 at 08:03:28 AM CEST 
//


package at.erpel.schemas._1p0.messaging.message;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the at.erpel.schemas._1p0.messaging.message package. 
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

    private final static QName _ErpelMessage_QNAME = new QName("http://erpel.at/schemas/1p0/messaging/message", "ErpelMessage");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: at.erpel.schemas._1p0.messaging.message
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link ErpelMessageType }
     * 
     */
    public ErpelMessageType createErpelMessageType() {
        return new ErpelMessageType();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ErpelMessageType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link ErpelMessageType }{@code >}
     */
    @XmlElementDecl(namespace = "http://erpel.at/schemas/1p0/messaging/message", name = "ErpelMessage")
    public JAXBElement<ErpelMessageType> createErpelMessage(ErpelMessageType value) {
        return new JAXBElement<ErpelMessageType>(_ErpelMessage_QNAME, ErpelMessageType.class, null, value);
    }

}
