/*
 * Parses Huawei managed object tree Configuration Data dump from  XML to csv
 * @version 1.0.0
 * @since 1.0.0
 */
package com.bodastage.boda_huaweicmmotreeparser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;

/**
 *
 * @author info@bodastage.com
 */
public class HuaweiCMMOTreeParser {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(HuaweiCMMOTreeParser.class);
    
    /**
     * Current release version 
     * 
     * Since 1.3.0
     */
    final static String VERSION = "2.0.0";
    
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
     * File containing a list of parameters to export
     * 
     * @since 1.2.0
     */
    private String parameterFile = null;
    
    /**
     * Set the parameter file name 
     * 
     * @param filename 
     */
    public void setParameterFile(String filename){
        parameterFile = filename;
    }
    
    /**
     * Parser states. Currently there are only 2: extraction and parsing
     * 
     */
    private int parserState = ParserStates.EXTRACTING_PARAMETERS;
    
   /**
     * The file/directory to be parsed.
     * 
     * @since 1.1.0
     */
    private String dataSource;
    
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
     * Extract managed objects and their parameters
     */
    private Boolean extractParametersOnly = false;
    
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
    public void parseFile(String filename) 
    throws XMLStreamException, FileNotFoundException, UnsupportedEncodingException
    {
            XMLInputFactory factory = XMLInputFactory.newInstance();

            XMLEventReader eventReader = factory.createXMLEventReader(
                    new FileReader(filename));
            baseFileName = getFileBasename(filename);

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

    }
            
    /**
     * Reset parser variables before next file
     */
    public void resetVariables(){
        classDepth = 0;
        className = null;
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
     
    /**
     * Determines if the source data file is a regular file or a directory and 
     * parses it accordingly
     * 
     * @since 1.1.0
     * @version 1.0.0
     * @throws XMLStreamException
     * @throws FileNotFoundException
     * @throws UnsupportedEncodingException
     */
    public void processFileOrDirectory()
            throws XMLStreamException, FileNotFoundException, UnsupportedEncodingException {
        //this.dataFILe;
        Path file = Paths.get(this.dataSource);
        boolean isRegularExecutableFile = Files.isRegularFile(file)
                & Files.isReadable(file);

        boolean isReadableDirectory = Files.isDirectory(file)
                & Files.isReadable(file);

        if (isRegularExecutableFile) {
            this.setFileName(this.dataSource);
            baseFileName =  getFileBasename(this.dataFile);
            if( parserState == ParserStates.EXTRACTING_PARAMETERS){
                System.out.print("Extracting parameters from " + this.baseFileName + "...");
            }else{
                System.out.print("Parsing " + this.baseFileName + "...");
            }
            this.parseFile(this.dataSource);
            
            if( parserState == ParserStates.EXTRACTING_PARAMETERS){
                 System.out.println("Done.");
            }else{
                System.out.println("Done.");
                //System.out.println(this.baseFileName + " successfully parsed.\n");
            }
        }

        if (isReadableDirectory) {

            File directory = new File(this.dataSource);

            //get all the files from a directory
            File[] fList = directory.listFiles();

            for (File f : fList) {
                this.setFileName(f.getAbsolutePath());
                try {
                    
                    //@TODO: Duplicate call in parseFile. Remove!
                    baseFileName =  getFileBasename(this.dataFile);
                    if( parserState == ParserStates.EXTRACTING_PARAMETERS){
                        System.out.print("Extracting parameters from " + this.baseFileName + "...");
                    }else{
                        System.out.print("Parsing " + this.baseFileName + "...");
                    }
                    
                    //Parse
                    this.parseFile(f.getAbsolutePath());
                    if( parserState == ParserStates.EXTRACTING_PARAMETERS){
                         System.out.println("Done.");
                    }else{
                        System.out.println("Done.");
                        //System.out.println(this.baseFileName + " successfully parsed.\n");
                    }
                   
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                    System.out.println("Skipping file: " + this.baseFileName + "\n");
                }
            }
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
            
            if(parameterFile == null && parserState == ParserStates.EXTRACTING_PARAMETERS){
                Stack columns = new Stack();
                if(!moColumns.containsKey(className)){
                    moColumns.put(className,new Stack());
                }
                
                columns = moColumns.get(className);
                Iterator<Map.Entry<String, String>> iter 
                            = moiParameterValueMap.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry<String, String> me = iter.next();
                    if( ! columns.contains(me.getKey())){
                        columns.push(me.getKey());
                    }       
                }
            }
            
            if( parserState == ParserStates.EXTRACTING_VALUES){
                String paramNames = "FILENAME,NODENAME";
                String paramValues = baseFileName+","+toCSVFormat(this.nodeName);
                
                //If MO is not in parameterFile, continue
                if(!moColumns.containsKey(className) && parameterFile != null){
                    moiParameterValueMap.clear();
                    return;
                }
                
                //Create MO.csv file if it does not exist yet 
                //...and add the column headers
                if(!moiPrintWriters.containsKey(className)){
                    String moiFile = outputDirectory + File.separatorChar + className +  ".csv";
                    moiPrintWriters.put(className, new PrintWriter(moiFile));
                    
                    //the MO parameters from moColumns
                    String pName = paramNames;
                    Stack columns = moColumns.get(className);

                    //Write headers
                    for(int i =0; i < columns.size(); i++){
                        String p = columns.get(i).toString();

                        //Skip default paramters
                        if( p.toLowerCase().equals("filename") || 
                            p.toLowerCase().equals("datetime") ||
                            p.toLowerCase().equals("nodename")
                            ) continue;
                        pName += "," + columns.get(i);
                    }
                    moiPrintWriters.get(className).println(pName);
                }
                
                //Wite the values to 
                Stack moiAttributes = moColumns.get(className);
                moiParameterValueMap = classNameAttrsMap.get(className);
                //System.out.println(moiParameterValueMap.toString());
                for(int i = 0; i< moiAttributes.size(); i++){
                    String moiName = moiAttributes.get(i).toString();

                    //Skip default paramters
                     if( moiName.toLowerCase().equals("filename") || 
                         moiName.toLowerCase().equals("datetime") ||
                         moiName.toLowerCase().equals("nodename")
                         ) continue;

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
        
        
        //This is section is from previous implementation
//        if(qName.equals("MO")){
//            classDepth--;
//            String paramNames = "FILENAME,NODENAME";
//            String paramValues = baseFileName+","+toCSVFormat(this.nodeName);
//        
//            if(!moiPrintWriters.containsKey(className)){
//                String moiFile = outputDirectory + File.separatorChar + className +  ".csv";
//                moiPrintWriters.put(className, new PrintWriter(moiFile));
//                
//                Stack moiAttributes = new Stack();
//                moiParameterValueMap = classNameAttrsMap.get(className);
//                Iterator<Map.Entry<String, String>> iter 
//                        = moiParameterValueMap.entrySet().iterator();
//
//                String pName = paramNames;
//                while (iter.hasNext()) {
//                    Map.Entry<String, String> me = iter.next();
//                    moiAttributes.push(me.getKey());
//                    pName += "," + me.getKey();
//                }
//                
//                moColumns.put(className, moiAttributes);
//                moiPrintWriters.get(className).println(pName);
//            }
//            
//            Stack moiAttributes = moColumns.get(className);
//            moiParameterValueMap = classNameAttrsMap.get(className);
//            //System.out.println(moiParameterValueMap.toString());
//            for(int i = 0; i< moiAttributes.size(); i++){
//                String moiName = moiAttributes.get(i).toString();
//                
//                if( moiParameterValueMap.containsKey(moiName) ){
//                    paramValues += "," + toCSVFormat(moiParameterValueMap.get(moiName));
//                }else{
//                    paramValues += ",";
//                }   
//            }
//            
//            
//            PrintWriter pw = moiPrintWriters.get(className);
//            pw.println(paramValues);
//            
//            moiParameterValueMap.clear();
//            classNameAttrsMap.get(className).clear();
//            return;
//        }
    }
    

    
    /**
     * Parser entry point 
     * 
     * @since 1.0.0
     * @version 1.1.0
     * 
     * @throws XMLStreamException
     * @throws FileNotFoundException
     * @throws UnsupportedEncodingException 
     */
    public void parse() throws XMLStreamException, FileNotFoundException, UnsupportedEncodingException {
        //Extract parameters
        if (parserState == ParserStates.EXTRACTING_PARAMETERS) {
            processFileOrDirectory();

            parserState = ParserStates.EXTRACTING_VALUES;
        }

        //Reset variables
        resetVariables();
        
        //Extracting values
        if (parserState == ParserStates.EXTRACTING_VALUES) {
            processFileOrDirectory();
            parserState = ParserStates.EXTRACTING_DONE;
        }
        
        closeMOPWMap();
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
    
        
     public void setExtractParametersOnly(Boolean bool){
        extractParametersOnly = bool;
    }
    
     
    /**
     * Set name of file to parser.
     * 
     * @since 1.0.1
     * @version 1.0.0
     * @param dataSource 
     */
    public void setDataSource(String dataSource ){
        this.dataSource = dataSource;
    }
    
    
  /**
     * Extract parameter list from  parameter file
     * 
     * @param filename 
     */
    public  void getParametersToExtract(String filename) throws FileNotFoundException, IOException{
        BufferedReader br = new BufferedReader(new FileReader(filename));
        for(String line; (line = br.readLine()) != null; ) {
           String [] moAndParameters =  line.split(":");
           String mo = moAndParameters[0];
           String [] parameters = moAndParameters[1].split(",");
           
           Stack parameterStack = new Stack();
           for(int i =0; i < parameters.length; i++){
               parameterStack.push(parameters[i]);
           }
           
           moColumns.put(mo, parameterStack);

        }
        
        //Move to the parameter value extraction stage
        //parserState = ParserStates.EXTRACTING_VALUES;
    }
     
    /**
     * @param args the command line arguments
     *
     * @since 0.1.0
     * @version 0.1.0
     */
    public static void main(String[] args) {
        //Define
        Options options = new Options();
        CommandLine cmd = null;
        String outputDirectory = null;
        String inputFile = null;
        String parameterConfigFile = null;
        Boolean onlyExtractParameters = false;
        Boolean showHelpMessage = false;
        Boolean showVersion = false;
        Boolean attachMetaFields = false; //Attach mattachMetaFields FILENAME,DATETIME,TECHNOLOGY,VENDOR,VERSION,NETYPE

        try {
            options.addOption("p", "extract-parameters", false, "extract only the managed objects and parameters");
            options.addOption("v", "version", false, "display version");
            options.addOption(Option.builder("i")
                    .longOpt("input-file")
                    .desc("input file or directory name")
                    .hasArg()
                    .argName("INPUT_FILE").build());
            options.addOption(Option.builder("o")
                    .longOpt("output-directory")
                    .desc("output directory name")
                    .hasArg()
                    .argName("OUTPUT_DIRECTORY").build());
            options.addOption(Option.builder("c")
                    .longOpt("parameter-config")
                    .desc("parameter configuration file")
                    .hasArg()
                    .argName("PARAMETER_CONFIG").build());
            options.addOption("h", "help", false, "show help");

            //Parse command line arguments
            CommandLineParser parser = new DefaultParser();
            cmd = parser.parse(options, args);

            if (cmd.hasOption("h")) {
                showHelpMessage = true;
            }

            if (cmd.hasOption("v")) {
                showVersion = true;
            }

            if (cmd.hasOption('o')) {
                outputDirectory = cmd.getOptionValue("o");
            }

            if (cmd.hasOption('i')) {
                inputFile = cmd.getOptionValue("i");
            }

            if (cmd.hasOption('c')) {
                parameterConfigFile = cmd.getOptionValue("c");
            }

            if (cmd.hasOption('p')) {
                onlyExtractParameters = true;
            }

        } catch (IllegalArgumentException e) {

        } catch (ParseException ex) {
//            java.util.logging.Logger.getLogger(HuaweiCMObjectParser.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        try {
            
            if(showVersion == true ){
                System.out.println(VERSION);
                System.out.println("Copyright (c) 2019 Bodastage Solutions(http://www.bodastage.com)");
                System.exit(0);
            }
            
            //show help
            if( showHelpMessage == true || 
                inputFile == null || 
                ( outputDirectory == null && onlyExtractParameters == false) ){
                     HelpFormatter formatter = new HelpFormatter();
                     String header = "Parses Huawei MO Tree CM XML data to csv\n\n";
                     String footer = "\n";
                     footer += "Examples: \n";
                     footer += "java -jar boda-huaweicmmotreeparser.jar -i dump_file -o out_folder\n";
                     footer += "java -jar boda-huaweicmmotreeparser.jar -i input_folder -o out_folder\n";
                     footer += "java -jar boda-huaweicmmotreeparser.jar -i input_folder -p\n";
                     footer += "java -jar boda-huaweicmmotreeparser.jar -i input_folder -p -m\n";
                     footer += "\nCopyright (c) 2019 Bodastage Solutions(http://www.bodastage.com)";
                     formatter.printHelp( "java -jar boda-huaweicmmotreeparser.jar", header, options, footer );
                     System.exit(0);
            }
            
            //Confirm that the output directory is a directory and has write 
            //privileges
            if(outputDirectory != null ){
                File fOutputDir = new File(outputDirectory);
                if (!fOutputDir.isDirectory()) {
                    System.err.println("ERROR: The specified output directory is not a directory!.");
                    System.exit(1);
                }

                if (!fOutputDir.canWrite()) {
                    System.err.println("ERROR: Cannot write to output directory!");
                    System.exit(1);
                }
            }
            
            //Get parser instance
            HuaweiCMMOTreeParser cmParser = new HuaweiCMMOTreeParser();

            
            if(onlyExtractParameters == true ){
                cmParser.setExtractParametersOnly(true);
            }
            
            
            if(  parameterConfigFile != null ){
                File f = new File(parameterConfigFile);
                if(f.isFile()){
                    cmParser.setParameterFile(parameterConfigFile);
                    cmParser.getParametersToExtract(parameterConfigFile);
                    cmParser.parserState = ParserStates.EXTRACTING_VALUES;
                }
            }
            
            cmParser.setDataSource(inputFile);
            if(outputDirectory != null ) cmParser.setOutputDirectory(outputDirectory);
            
            cmParser.parse();
            
        }catch (Exception ex) {
            System.out.println(ex.getMessage());
            System.exit(1);
        }
    }
    
    
}
