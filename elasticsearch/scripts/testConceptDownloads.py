#!/usr/bin/env python

import sys
import os
import glob
import json
from deepdiff import DeepDiff

CONCEPT_INPUT_DIR_1 = 'REPLACE_WITH_DIRECTORY1'
CONCEPT_INPUT_DIR_2 = 'REPLACE_WITH_DIRECTORY2'

concepts_in_dir = [ os.path.basename(p) for p in glob.glob(CONCEPT_INPUT_DIR_1 + "*.json")]

for file in concepts_in_dir:
    input_file_1 = CONCEPT_INPUT_DIR_1  + file
    input_file_2 = CONCEPT_INPUT_DIR_2  + file
    concept_1 = {}
    with open(input_file_1,'r') as f:
        concept_1 = json.load(f)
    concept_2 = {}
    with open(input_file_2,'r') as f:
        concept_2 = json.load(f)
    
    ddiff = DeepDiff(concept_1, concept_2, ignore_order=True)
    if ddiff:
        print("Working on: ", file)
        print(ddiff)
    
