/*
 * Parses Huawei managed object tree Configuration Data dump from  XML to csv
 * @version 1.0.0
 * @since 1.0.0
 */
package com.bodastage.boda_huaweicmmotreeparser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Stack;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/**
 *
 * @author info@bodastage.com
 */
public class HuaweiCMMOTreeParser {
    /**
     * Tracks Managed Object attributes to write to file. This is dictated by 
     * the first instance of the MO found. 
     * @TODO: Handle this better.
     *
     * @since 1.0.0
     */
    private Map<String, Stack> moColumns = new LinkedHashMap<String, Stack>();
    
    /**
     * This holds a map of the Managed Object Instances (MOIs) to the respective
     * csv print writers.
     * 
     * @since 1.0.0
     */
    private Map<String, PrintWriter> moiPrintWriters 
            = new LinkedHashMap<String, PrintWriter>();
    
    /**
     * Tag data.
     *
     * @since 1.0.0
     * @version 1.0.0
     */
    private String tagData = "";
    
    /**
     * This is used when subsituting a parameter value with the value indicated
     * in comments.
     * 
     * @since 1.0.0
     */
    private String previousTag;
    
    /**
     * Output directory.
     *
     * @since 1.0.0
     */
    private String outputDirectory = "/tmp";
    
    /**
     * Parser start time. 
     * 
     * @since 1.0.4
     * @version 1.0.0
     */
    final long startTime = System.currentTimeMillis();
    
    /**
     * Tracks how deep a class tag is in the hierarch.
     * 
     * @since 1.0.0
     * @version 1.0.0
     */
    private int classDepth = 0;
    
    /**
     * The base file name of the file being parsed.
     * 
     * @since 1.0.0
     */
    private String baseFileName = "";
    
    /**
     * The file to be parsed.
     * 
     * @since 1.0.0
     */
    private String dataFile;
    
    /**
     * The nodename.
     * 
     * @since 1.0.0
     * @version 1.0.0
     */
    private String nodeName;
    
    /**
     * The holds the parameters and corresponding values for the moi tag  
     * currently being processed.
     * 
     * @since 1.0.0
     */
    private Map<String,String> moiParameterValueMap 
            = new LinkedHashMap<String, String>();
    
    /**
     * The holds the parameters and corresponding values for the moi tag  
     * currently being processed.
     * 
     * @since 1.0.0
     * @version 1.0.0
     */
    private Map<String,LinkedHashMap<String,String>> classNameAttrsMap 
            = new LinkedHashMap<String, LinkedHashMap<String,String>>();
    
    /**
     * ClassName tag stack. 
     * 
     * @version 1.0.0
     * @since 1.0.0
     */
    private Stack classNameStack = new Stack();
    
    /**
     * Current className MO attribute.
     * 
     * @since 1.0.0
     * @version 1.0.0
     */
    private String className = null;

    /**
     * Current attr tag's name attribute.
     * 
     * @since 1.0.0
     * @version 1.0.0
     */
    private String moAttrName = null;
    
    /**
     * The parser's entry point.
     * 
     * @param filename 
     */
    public void parse() 
    throws XMLStreamException, FileNotFoundException, UnsupportedEncodingException
    {
            XMLInputFactory factory = XMLInputFactory.newInstance();

            XMLEventReader eventReader = factory.createXMLEventReader(
                    new FileReader(this.dataFile));
            baseFileName = getFileBasename(this.dataFile);

            while (eventReader.hasNext()) {
                XMLEvent event = eventReader.nextEvent();
                
                switch (event.getEventType()) {
                    case XMLStreamConstants.START_ELEMENT:
                        startElementEvent(event);
                        break;
                    case XMLStreamConstants.SPACE:
                    case XMLStreamConstants.CHARACTERS:
                        characterEvent(event);
                        break;
                    case XMLStreamConstants.END_ELEMENT:
                        endELementEvent(event);
                        break;
                    case XMLStreamConstants.COMMENT:
                        if(moiParameterValueMap.containsKey(this.previousTag)){
                            String comment 
                                    = ((javax.xml.stream.events.Comment) event).getText();
                            moiParameterValueMap.put(previousTag,comment);
                        }
                        break;
                }
            }
            //
            closeMOPWMap();
    }
            
    /**
     * Handle start element event.
     *
     * @param xmlEvent
     *
     * @since 1.0.0
     * @version 1.0.0
     *
     */
    public void startElementEvent(XMLEvent xmlEvent) throws FileNotFoundException {
        
        StartElement startElement = xmlEvent.asStartElement();
        String qName = startElement.getName().getLocalPart();
        String prefix = startElement.getName().getPrefix();
        
        Iterator<Attribute> attributes = startElement.getAttributes();
        if(qName.equals("MO")){
            classDepth++;

            while (attributes.hasNext()) {
                Attribute attribute = attributes.next();
                String attrName = attribute.getName().getLocalPart();
                String attrValue =  attribute.getValue();
                if (attrName.equals("className")) {
                    className = attrValue;
                    LinkedHashMap<String,String> Lhm = new LinkedHashMap<String,String>();
                    classNameAttrsMap.put(className,Lhm);    
                }
            }
        }
        
        //attr
        if(qName.equals("attr")){            
            Attribute attribute = attributes.next();
            String attrName = attribute.getName().getLocalPart();
            if (attrName.equals("name")) {
                moAttrName = attribute.getValue();
            }
        }
    }
           
    /**
     * Handle character events.
     *
     * @param xmlEvent
     * @version 1.0.0
     * @since 1.0.0
     */
    public void characterEvent(XMLEvent xmlEvent) {
        Characters characters = xmlEvent.asCharacters();
        if(!characters.isWhiteSpace()){
            tagData = characters.getData(); 
        }
    }  
    
    /**
     * Get file base name.
     * 
     * @since 1.0.0
     */
     public String getFileBasename(String filename){
        try{
            return new File(filename).getName();
        }catch(Exception e ){
            return filename;
        }
    }
     
    public void endELementEvent(XMLEvent xmlEvent)
            throws FileNotFoundException, UnsupportedEncodingException {
        EndElement endElement = xmlEvent.asEndElement();
        String prefix = endElement.getName().getPrefix();
        String qName = endElement.getName().getLocalPart();
        
        if(qName.equals("attr")){
            LinkedHashMap<String,String> Lhm = classNameAttrsMap.get(className);
            Lhm.put(moAttrName, tagData);
            classNameAttrsMap.put(className,Lhm);
            
            if( classDepth == 1 && moAttrName.equals("name")){
                this.nodeName = tagData;
            }
            tagData = "";
            return;
        }
        
        if(qName.equals("MO")){
            classDepth--;
            String paramNames = "FileName,NODENAME";
            String paramValues = baseFileName+","+toCSVFormat(this.nodeName);
        
            if(!moiPrintWriters.containsKey(className)){
                String moiFile = outputDirectory + File.separatorChar + className +  ".csv";
                moiPrintWriters.put(className, new PrintWriter(moiFile));
                
                Stack moiAttributes = new Stack();
                moiParameterValueMap = classNameAttrsMap.get(className);
                Iterator<Map.Entry<String, String>> iter 
                        = moiParameterValueMap.entrySet().iterator();

                String pName = paramNames;
                while (iter.hasNext()) {
                    Map.Entry<String, String> me = iter.next();
                    moiAttributes.push(me.getKey());
                    pName += "," + me.getKey();
                }
                
                moColumns.put(className, moiAttributes);
                moiPrintWriters.get(className).println(pName);
            }
            
            Stack moiAttributes = moColumns.get(className);
            moiParameterValueMap = classNameAttrsMap.get(className);
            //System.out.println(moiParameterValueMap.toString());
            for(int i = 0; i< moiAttributes.size(); i++){
                String moiName = moiAttributes.get(i).toString();
                
                if( moiParameterValueMap.containsKey(moiName) ){
                    paramValues += "," + toCSVFormat(moiParameterValueMap.get(moiName));
                }else{
                    paramValues += ",";
                }   
            }
            
            
            PrintWriter pw = moiPrintWriters.get(className);
            pw.println(paramValues);
            
            moiParameterValueMap.clear();
            classNameAttrsMap.get(className).clear();
            return;
        }
    }
    
    /**
     * Print program's execution time.
     * 
     * @since 1.0.0
     */
    public void printExecutionTime(){
        float runningTime = System.currentTimeMillis() - startTime;
        
        String s = "Parsing completed. ";
        s = s + "Total time:";
        
        //Get hours
        if( runningTime > 1000*60*60 ){
            int hrs = (int) Math.floor(runningTime/(1000*60*60));
            s = s + hrs + " hours ";
            runningTime = runningTime - (hrs*1000*60*60);
        }
        
        //Get minutes
        if(runningTime > 1000*60){
            int mins = (int) Math.floor(runningTime/(1000*60));
            s = s + mins + " minutes ";
            runningTime = runningTime - (mins*1000*60);
        }
        
        //Get seconds
        if(runningTime > 1000){
            int secs = (int) Math.floor(runningTime/(1000));
            s = s + secs + " seconds ";
            runningTime = runningTime - (secs/1000);
        }
        
        //Get milliseconds
        if(runningTime > 0 ){
            int msecs = (int) Math.floor(runningTime/(1000));
            s = s + msecs + " milliseconds ";
            runningTime = runningTime - (msecs/1000);
        }

        
        System.out.println(s);
    }
    
    /**
     * Close file print writers.
     *
     * @since 1.0.0
     * @version 1.0.0
     */
    public void closeMOPWMap() {
        Iterator<Map.Entry<String, PrintWriter>> iter
                = moiPrintWriters.entrySet().iterator();
        while (iter.hasNext()) {
            iter.next().getValue().close();
        }
        moiPrintWriters.clear();
    }
    
    /**
     * Process given string into a format acceptable for CSV format.
     *
     * @since 1.0.0
     * @param s String
     * @return String Formated version of input string
     */
    public String toCSVFormat(String s) {
        String csvValue = s;

        //Check if value contains comma
        if (s.contains(",")) {
            csvValue = "\"" + s + "\"";
        }

        if (s.contains("\"")) {
            csvValue = "\"" + s.replace("\"", "\"\"") + "\"";
        }

        return csvValue;
    }
    
    /**
     * Set the output directory.
     * 
     * @since 1.0.0
     * @version 1.0.0
     * @param directoryName 
     */
    public void setOutputDirectory(String directoryName ){
        this.outputDirectory = directoryName;
    }
     
    /**
     * Set name of file to parser.
     * 
     * @since 1.0.0
     * @version 1.0.0
     * @param directoryName 
     */
    public void setFileName(String filename ){
        this.dataFile = filename;
    }
}
