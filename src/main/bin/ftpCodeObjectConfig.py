from collections import OrderedDict
import json
import sys

def ftpCodes() :
    ftpCodeObj = {}
    with open(sys.argv[1], encoding = 'utf-8') as f:
        lines = f.readlines()
        for line in lines:
            code = line.split("|")[1].split("/")[-1];
            ftpCode = "/".join(line.split("|")[-2].split("/")[4:-1]) + "/";
            ftpCodeObj[code] = ftpCode;
            
    with open("metadata/ncit.json") as ncit:
        ncitData = json.load(ncit);
    
    ncitData["subsetLinks"] = ftpCodeObj;
        
    with open("metadata/ncit.json", "w+") as ncit:
        ncit.write(json.dumps(OrderedDict(ncitData), indent=4, sort_keys=True))

if __name__ == '__main__':
    if(len(sys.argv) < 2):
        sys.exit("Needs a file path argument: `python ftpCodeObjectConfig.py <file path>");
    ftpCodes()