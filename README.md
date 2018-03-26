![Build status](https://travis-ci.org/bodastage/boda-huaweicmmotreeparser.svg?branch=master)

# boda-huaweicmmotreeparser
Parses Huawei managed object tree Configuration Data dump from  XML to csv.

Below is the expected format of the input file:

```XML
<?xml version="1.0" encoding="ISO-8859-1"?>
<MOTree>
    <MO className="BSC6900GSMNE" fdn="NE=XXX">
        <attr name="fdn">NE=XXX</attr>
        <attr name="IP">XXX.XXX.XXX.XXX</attr>
        <attr name="MOIndex">XXX</attr>
        <!-- ... -->
        <MO className="BSC6900GSMABISE1T1" fdn="NE=XXX,ABISE1T1=XXX">
            <attr name="fdn">NE=XXX,ABISE1T1=XXX</attr>
            <attr name="MOIndex">XXX</attr>
            <!-- ... -->
        </MO>
        <!-- ... -->
    </MO>
</MOTree>
```
# Usage
java -jar  huaweicmmotreeparser.jar data.xml outputDirectory

# Download and installation
The lastest compiled jar file is availabled in the dist directory. Alternatively, download it directly from [here](https://github.com/bodastage/boda-huaweicmmotreeparser/raw/master/dist/boda-huaweicmmotreeparser.jar).

# Requirements
To run the jar file, you need Java version 1.6 and above.

# Getting help
To report issues with the application or request new features use the issue [tracker](https://github.com/bodastage/boda-huaweicmmotreeparser/issues). For help and customizations send an email to info@bodastage.com.

# Credits
[Bodastage](http://www.bodastage.com) - info@bodastage.com

# Contact
For any other concerns apart from issues and feature requests, send an email to info@bodastage.com.

# Licence
This project is licensed under the Apache 2.0 licence.  See LICENCE file for details.
