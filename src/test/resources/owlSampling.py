from datetime import datetime
import json
import os
import re
import sys

inAxiom = False
inClass = False
inRestriction = False
inSubclass = False
inEquivalentClass = False
inObjectProperty = False
inAnnotationProperty = False
currentClassURI = ""
currentClassCode = ""
currentClassPath = []
lastSpaces = 0
spaces = 0
axiomInfo = [] # key value pair storing info about current axiom
properties = {} # master list
axiomProperties = {} # list of axiom properties
objectProperties = {} # list of object properties
annotationProperties = {} # list of annotationProperties
propertiesMultipleSamples = {} # properties that should show multiple examples
propertiesMultipleSampleCodes = ['P310'] # the codes that should work with multiple samples
propertiesCurrentClass = {} # current class list
propertiesParentChilden = {} # parent/child list
parentStyle1 = [] # parent, child key value pair for subclass parent
parentStyle2 = [] # parent, child key value pair for rdf:description parent
uri2Code = {} # store ID codes for each URI when processed
uriRestrictions2Code = {} # separately store restriction codes so they don't get processed as roots
allParents = {} # all parent to children relationships
allChildren = {} # all children to parent relationships
parentCount = {} # list of parent count codes from 1 to n
deprecated = {} # list of all deprecated concepts
newRestriction = "" # restriction code
hitClass = False # ignore axioms and stuff before hitting any classes
termCodeline = "" # terminology Code identifier line

def checkParamsValid(argv):
    if(len(argv) != 3):
        print("Usage: owlQA.py <terminology owl file path> <terminology json path>")
        return False
    elif(os.path.isfile(argv[1]) == False or argv[1][-4:] != ".owl" or argv[2][-5:] != ".json"):
        print(argv[1][-4:])
        print("terminology owl file path is invalid")
        print("Usage: owlQA.py <terminology owl file path> <terminology json path>")
        return False
    return True

def parentChildProcess(line):
    uriToProcess = re.findall('"([^"]*)"', line)[0]
    if(line.startswith("<rdfs:subClassOf rdf:resource=")):
        if(parentStyle1 == []): # first example of rdf:subClassOf child
            parentStyle1.append((currentClassURI, uriToProcess)) # hold in the parentStyle1 object as tuple
    elif(line.startswith("<rdf:Description") and inEquivalentClass):
        if(parentStyle2 == []): # first example of rdf:Description child
            parentStyle2.append((currentClassURI, uriToProcess)) # hold in the parentStyle2 object as tuple
            
    if(currentClassURI in allParents and uriToProcess not in allParents[currentClassURI]): # process parent relationship
        allParents[currentClassURI].append(uriToProcess)
    else:
        allParents[currentClassURI] = [uriToProcess]
        
    if(uriToProcess in allChildren and currentClassURI not in allChildren[uriToProcess]): # process child relationship
        allChildren[uriToProcess].append(currentClassURI)
    else:
        allChildren[uriToProcess] = [currentClassURI]
    return

def checkForNewProperty(line):
    splitLine = re.split("[<>= \"]", line.strip()) # split by special characters
    splitLine = [x for x in splitLine if x != ''] # remove empty entries for consistency
    if(splitLine[0] in properties or splitLine[0] in propertiesCurrentClass): # check duplicates
        return ""
    detail = ""
    if("rdf:resource=\"" in line): # grab stuff in quotes
        detail = re.split(r'[#/]', re.findall('"([^"]*)"', line)[0])[-1] # the code is the relevant part
    else: # grab stuff in tag
        detail = re.findall(">(.*?)<", line)[0]
    return (splitLine[0], currentClassURI + "\t" + currentClassCode + "\t" + splitLine[0] + "\t" + detail + "\n")

def handleRestriction(line):
    global newRestriction # grab newRestriction global
    detail = re.findall('"([^"]*)"', line)[0]
    pathCode = "/".join(currentClassPath) + "~" # prebuild tag stack for restriction
    property = re.split(r'[#/]', detail)[-1] # extract property
    if(line.startswith("<owl:onProperty")): # property code
      if(detail in uri2Code):
        newRestriction = uri2Code[detail]
      else:
        newRestriction = property
            
    elif(line.startswith("<owl:someValuesFrom")): # value code
      if(newRestriction == "" or pathCode + newRestriction in uriRestrictions2Code): # duplicate
        return
      propertiesCurrentClass[pathCode+newRestriction] = currentClassURI + "\t" + currentClassCode + "\t" + pathCode+newRestriction + "\t" + detail + "\n" # add code/path to properties
      uriRestrictions2Code[pathCode+newRestriction] = newRestriction
      newRestriction = "" # saved code now used, reset to empty
            
def handleAxiom(line):
    global currentClassURI # grab globals
    global currentClassCode
    if(line.startswith("<owl:annotatedSource")): # get source uri and code
        currentClassURI = re.findall('"([^"]*)"', line)[0]
    elif(line.startswith("<owl:annotatedProperty")): # get property code
        sourceProperty = re.findall('"([^"]*)"', line)[0]
        axiomInfo.append("qualifier-" + re.split(r'[#/]', sourceProperty)[-1] + "~")
    elif(line.startswith("<owl:annotatedTarget")): # get target code
        axiomInfo.append(re.findall(">(.*?)<", line)[0] + "~")
    elif(not line.startswith("<owl:annotated") and axiomInfo[0] + re.split(r'[< >]', line)[1] not in axiomProperties): # get connected properties
        newProperty = re.split(r'[< >]', line)[1] # extract property from line
        if(len(re.findall(">(.*?)<", line)) > 0):
            newCode = re.findall(">(.*?)<", line)[0] # extract code from line
        else:
            newCode = re.split(r'[#/]', re.findall('"([^"]*)"', line)[0])[-1]
        axiomProperties[axiomInfo[0] + newProperty] = currentClassURI + "\t" + currentClassCode + "\t" + axiomInfo[0] + newProperty + "\t" + axiomInfo[1] + newCode + "\n"

if __name__ == "__main__":
    print("--------------------------------------------------")
    print("Starting..." + datetime.now().strftime("%d-%b-%Y %H:%M:%S"))
    print("--------------------------------------------------")
    
    if(checkParamsValid(sys.argv) == False):
        exit(1)
    with open (sys.argv[1], "r", encoding='utf-8') as owlFile:
        ontoLines = owlFile.readlines()
    terminology = sys.argv[1].split("/")[-1].split(".")[0].split("_")[0]
    with open(sys.argv[2]) as termJSONFile: # import id identifier line for terminology
      termJSONObject = json.load(termJSONFile)
      if(not termJSONObject["code"]):
        print("terminology json file does not have ID entry")
        exit(1)
      termCodeline = "<" + termJSONObject["code"] # data lines all start with #
    
    with open(terminology + "_Sampling_OWL.txt", "w") as termFile:
        for index, line in enumerate(ontoLines): # get index just in case
            lastSpaces = spaces # previous line's number of leading spaces (for comparison)
            spaces = len(line) - len(line.lstrip()) # current number of spaces (for stack level checking)
            line = line.strip() # no need for leading spaces anymore
            if(line.startswith("// Annotations")): # skip ending annotation
                hitClass = False
                
            elif(line.startswith("<owl:ObjectProperty")):
              inObjectProperty = True;
              currentClassURI = re.findall('"([^"]*)"', line)[0]
            elif(line.startswith("</owl:ObjectProperty>")):
              inObjectProperty = False
            elif inObjectProperty and line.startswith(termCodeline):
              uri2Code[currentClassURI] = re.findall(">(.*?)<", line)[0]
              objectProperties[currentClassURI] = uri2Code[currentClassURI]
              
            elif(line.startswith("<owl:AnnotationProperty")):
              inObjectProperty = True;
              currentClassURI = re.findall('"([^"]*)"', line)[0]
            elif(line.startswith("</owl:AnnotationProperty>")):
              inObjectProperty = False
            elif inObjectProperty and line.startswith(termCodeline):
              uri2Code[currentClassURI] = re.findall(">(.*?)<", line)[0]
              annotationProperties[currentClassURI] = uri2Code[currentClassURI]
                
            elif(len(line) < 1 or line[0] != '<'): # blank lines or random text
                continue
            elif(line.startswith("<owl:deprecated")): # ignore deprecated classes
                inClass = False
                propertiesCurrentClass = {} # ignore properties in deprecated class
                deprecated[currentClassURI] = True
                
            elif(line.startswith("<owl:Class ") and not inEquivalentClass):
              if not hitClass:
                hitClass = True
              inClass = True
              propertiesCurrentClass = {} # reset for new class
              currentClassURI = re.findall('"([^"]*)"', line)[0] # set uri entry in line
              currentClassCode = re.split("/|#", currentClassURI)[-1] # set initial class code
              uri2Code[currentClassURI] = currentClassCode # set initial uri code value
              continue
            elif(line.startswith("</owl:Class>") and not inEquivalentClass):
                for key, value in propertiesCurrentClass.items(): # replace code entry and write to file
                    properties[key] = value # add to master list
                inClass = False
                currentClassPath = []
                continue
                
            if(inClass and not inRestriction): # keep stack of current tree for restrictions (more/down level)
                if(spaces > lastSpaces): # add to stack based on spacing
                    currentClassPath.append(re.split(">| ", line)[0][1:])
                elif(lastSpaces > spaces): # remove from stack based on spacing (less/up level)
                    currentClassPath.pop()
                else: # replace in stack based on spacing (unchanged)
                    currentClassPath.pop()
                    currentClassPath.append(re.split(">| ", line)[0][1:])

            if((line.startswith("<rdfs:subClassOf>") and not line.endswith("//>\n"))): # find complex subclass            
                inSubclass = True
            elif(line.startswith("</rdfs:subClassOf>")) :
                inSubclass = False
            
            elif line.startswith("<owl:Axiom>") and hitClass: # find complex subclass            
                inAxiom = True
            elif line.startswith("</owl:Axiom>"):
                inAxiom = False
                axiomInfo = [] # empty the info list for the previous axiom
            elif inAxiom:
                handleAxiom(line)
 
            elif(line.startswith("<owl:equivalentClass>")): # tag equivalentClass (necessary for restrictions)
                inEquivalentClass = True
            elif(line.startswith("</owl:equivalentClass>")) :
                inEquivalentClass = False
                continue
            
            elif(line.startswith("<rdfs:subClassOf") or (line.startswith("<rdf:Description ") and inEquivalentClass)): # catch either example of parent/child relationship
                parentChildProcess(line)
                            
            elif(inClass and line.startswith("<owl:Restriction>")):
                inRestriction = True
            elif(inClass and line.startswith("</owl:Restriction>")):
                inRestriction = False
            elif(inClass and inRestriction):
                handleRestriction(line)

            elif(inClass and not inSubclass and not inEquivalentClass): # default property not in complex part of class
                if(line.startswith(termCodeline)): # catch ID to return if it has properties
                    currentClassCode = re.findall(">(.*?)<", line)[0]
                    uri2Code[currentClassURI] = currentClassCode # store code for uri
                    continue
                newEntry = checkForNewProperty(line)
                if(len(newEntry) > 1 and newEntry[0] in propertiesMultipleSampleCodes): # handle multiple example codes
                    exampleCode = newEntry[1].split("\t")[-1][:-1] # extract code
                    if(exampleCode not in propertiesMultipleSamples):
                        propertiesMultipleSamples[exampleCode] = newEntry[1] # set value as key to avoid duplications
                elif(len(newEntry) > 1 and newEntry[0] not in properties): # returned new property
                    propertiesCurrentClass[newEntry[0]] = newEntry[1] # add to current class property list
                    
        for key, value in properties.items(): # write normal properties
            splitLineTemp = value.split("\t") # split to get code isolated
            splitLineTemp[1] = uri2Code[splitLineTemp[0]]
            if(splitLineTemp[2] in uri2Code):
              splitLineTemp[2] = uri2Code[splitLineTemp[2]]
            if(splitLineTemp[3] and splitLineTemp[3][:-1] in uri2Code): # deal with newline
              splitLineTemp[3] = uri2Code[splitLineTemp[3][:-1]] + "\n"
            termFile.write("\t".join(splitLineTemp)) # rejoin and write
            
        for key, value in propertiesMultipleSamples.items(): # write properties with multiple examples
            splitLineTemp = value.split()
            splitLineTemp[1] = uri2Code[splitLineTemp[0]]
            termFile.write("\t".join(splitLineTemp) + "\n") # rejoin and write
            
        for key, value in axiomProperties.items(): # write properties with multiple examples
            termFile.write(value) # rejoin and write
        
        if(parentStyle1 != []): # write out subclass parent/child
            termFile.write(parentStyle1[0][0] + "\t" + uri2Code[parentStyle1[0][0]] + "\t" + "parent-style1" + "\t" + uri2Code[parentStyle1[0][1]] + "\n")
            termFile.write(parentStyle1[0][0] + "\t" + uri2Code[parentStyle1[0][1]] + "\t" + "child-style1" + "\t" + uri2Code[parentStyle1[0][0]] + "\n")
        if(parentStyle2 != []): # write out relationship parent/child
            termFile.write(parentStyle2[0][0] + "\t" + uri2Code[parentStyle2[0][0]] + "\t" + "parent-style2" + "\t" + uri2Code[parentStyle2[0][1]] + "\n")
            termFile.write(parentStyle2[0][0] + "\t" + uri2Code[parentStyle2[0][1]] + "\t" + "child-style2" + "\t" + uri2Code[parentStyle2[0][0]] + "\n")
            
        maxChildren = ("", 0)
        for parent, children in allChildren.items(): # find maximum number of children
            if len(children) > maxChildren[1]: # update whenever we find a bigger child list
                maxChildren = (parent, len(children))
        if(maxChildren[1] > 0): # write that property to the file
            termFile.write(maxChildren[0] + "\t" + uri2Code[maxChildren[0]] + "\t" + "max-children" + "\t" + str(maxChildren[1]) + "\n")
            
        for child, parents in allParents.items(): # process parent counts
            if(len(parents) not in parentCount): # add new length example
                parentCount[len(parents)] = child
        for numParents in sorted(parentCount.keys()): # sort for writing to file
            termFile.write(parentCount[numParents] + "\t" + uri2Code[parentCount[numParents]] + "\t" + "parent-count" + str(numParents) + "\n")
            
        for code in uri2Code: # write out roots (all codes with no parents)
            if code not in allParents and code not in deprecated and code not in objectProperties and code not in annotationProperties: # deprecated codes, object properties, and annotation properties are fake roots
                termFile.write(code + "\t" + uri2Code[code] + "\t" + "root" + "\n")
            

    print("")
    print("--------------------------------------------------")
    print("Ending..." + datetime.now().strftime("%d-%b-%Y %H:%M:%S"))
    print("--------------------------------------------------")