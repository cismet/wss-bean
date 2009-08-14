package net.environmatics.acs.accessor.utils;

import java.util.HashMap;
import java.util.Map;

import net.environmatics.acs.accessor.interfaces.AuthenticationMethod;
import org.apache.commons.httpclient.NameValuePair;
import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;

/**
 * Adds all neccessary namespaces to a DocumentFactory which can be accessed 
 * from other classes. Also implements a method for generating a WSS doService 
 * request. <br>
 * Used for convencience.
 */
public class DOMHelper {

    
    /** OpenGIS OWS Namespace */
    public static final Namespace NAMESPACE_OWS = new Namespace("ows", "http://www.opengis.net/ows");

    /** GDI NRW Authn Namespace */
    public static final Namespace NAMESPACE_AUTHEN = new Namespace("authn", "http://www.gdi-nrw.org/authentication");
    /** GDI NRW WAS Namespace */
    public static final Namespace NAMESPACE_WAS = new Namespace("was", "http://www.gdi-nrw.org/was");
    /** GDI NRW WSS Namespace */
    public static final Namespace NAMESPACE_WSS = new Namespace("wss", "http://www.gdi-nrw.org/wss");
    
    /** W3C XSI Namespace */
    public static final Namespace NAMESPACE_XSI = new Namespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");

    private static DocumentFactory documentFactory = DocumentFactory.getInstance();
 
    static {
        Map namespaces = new HashMap();
        
        namespaces.put(NAMESPACE_OWS.getPrefix(), NAMESPACE_OWS.getURI());
        
        namespaces.put(NAMESPACE_AUTHEN.getPrefix(), NAMESPACE_AUTHEN.getURI());
        namespaces.put(NAMESPACE_WAS.getPrefix(), NAMESPACE_WAS.getURI());
        namespaces.put(NAMESPACE_WSS.getPrefix(), NAMESPACE_WSS.getURI());
        
        namespaces.put(NAMESPACE_XSI.getPrefix(), NAMESPACE_XSI.getURI());
        
        documentFactory.setXPathNamespaceURIs(namespaces);
    }

    /* Will not be called */
    private DOMHelper() { /* Not used */ }

    
    /**
     * Returns a DocumentFactory with all neccessary namespaces
     * @return DocumentFactory
     */
    public static DocumentFactory getDocumentFactory() {
        return documentFactory;
    }
    
    
    /**
     * Generates a Document with a WSS doService request.
     * 
     * @param dcp_type <b>Must</b> be 
     *      {@link net.environmatics.acs.accessor.interfaces.WSSAccessor#DCP_HTTP_GET HTTP GET} or
     *      {@link net.environmatics.acs.accessor.interfaces.WSSAccessor#DCP_HTTP_POST HTTP POST}
     * @param request Request to a secured service
     * @param authMethod The authentication method that should be used.
     * @param requestParams An array with request parameters, i.e. HTTP_Header / Mime-Type: text/xml
     * @param facadeURL String with an URL to facade
     * @return Document containing all neccessary information for the WSS doService request
     */
    public static Document generateDoService(String dcp_type, String request, AuthenticationMethod authMethod, NameValuePair[] requestParams, String facadeURL) {
        
        /* Only HTTP POST and HTTP GET are supported */
        if (!dcp_type.equals("HTTP_POST") && !dcp_type.equals("HTTP_GET")) {
            throw new IllegalArgumentException(dcp_type + " not supported!");
        }
        
        DocumentFactory docFactory = getDocumentFactory();

        // Generating the service request based on the input
        Element elServiceRequest = docFactory.createElement(new QName("ServiceRequest", DOMHelper.NAMESPACE_WSS));
        elServiceRequest.add(docFactory.createAttribute(elServiceRequest, "DCP", dcp_type));
            
        // Adding the request parameters to the request
        for (NameValuePair nvp : requestParams) {
            Element elRequestParameter = docFactory.createElement(new QName("RequestParameter", DOMHelper.NAMESPACE_WSS));
            elRequestParameter.add(docFactory.createAttribute(elRequestParameter, "id", nvp.getName()));
            elRequestParameter.addText(nvp.getValue());
            elServiceRequest.add(elRequestParameter);
        }
            
        // Adding payload
        Element elPayload = docFactory.createElement(new QName("Payload", DOMHelper.NAMESPACE_WSS));

        // Determine the payload
        if (dcp_type.equals("HTTP_GET")) {
            elPayload.addText(request);
        } else {
            elPayload.addCDATA(request);
        }

        elServiceRequest.add(elPayload);
        
        // FacadeURL
        Element elFacadeURL = docFactory.createElement(new QName("FacadeURL", DOMHelper.NAMESPACE_WSS));
        elFacadeURL.addText(facadeURL);

        
        Element elDoService = docFactory.createElement(new QName("DoService"));

        // Adding Namespaces
        elDoService.add(DOMHelper.NAMESPACE_AUTHEN);
        elDoService.add(DOMHelper.NAMESPACE_WSS);
        elDoService.add(DOMHelper.NAMESPACE_OWS);
        elDoService.add(DOMHelper.NAMESPACE_XSI);
        
        // Adding doService Attributes
        elDoService.addAttribute("service", "WSS");
        elDoService.addAttribute("version", "1.0");
        
        // Adding doService elements
        elDoService.add(authMethod.asDOM4jElement());   // Adding authentication data
        elDoService.add(elServiceRequest);              // Adding ServiceRequest
        elDoService.add(elFacadeURL);                   // Adding FacadeURL 
        
        // Creating the Document
        Document requestDoc = docFactory.createDocument();
        requestDoc.add(elDoService);
        return requestDoc;
    }
}
