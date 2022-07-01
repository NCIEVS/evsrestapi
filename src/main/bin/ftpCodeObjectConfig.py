import json

def ftpCodes() :
    ftpCodeObj = {}
    with open("value_set_report_config.txt", encoding = 'utf-8') as f:
        lines = f.readlines()
        for line in lines:
            code = line.split("|")[1].split("/")[-1]
            ftpCode = "/".join(line.split("|")[-2].split("/")[4:-1]) + "/";
            ftpCodeObj[code] = ftpCode;
    print(json.dumps(ftpCodeObj, indent=4, sort_keys=True))


if __name__ == '__main__':
    ftpCodes()