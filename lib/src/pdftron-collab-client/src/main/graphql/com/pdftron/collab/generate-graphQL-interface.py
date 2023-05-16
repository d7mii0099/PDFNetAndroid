#!/usr/bin/env python
#coding: utf-8
import argparse
import os
import re
import shutil
import sys
import textwrap

startingPath = os.getcwd()

def main(*args):
    parser = argparse.ArgumentParser()
    parser.add_argument('-p', '--path', dest='path', help='Specify a path to the typescript files parent directory.')
    parser.add_argument('-d', '--destination', dest='destination', default='graphql', help='Specify a destination path for the output files.')
    stored_args, ignored_args = parser.parse_known_args()

    path = stored_args.path

    if(not stored_args.path):
        print 'You must provide a path to the typescipt templates with -p. Run with -h for usage.'
        sys.exit(-1)

    destinationDir = stored_args.destination
    if not os.path.exists(destinationDir):
        os.mkdir(destinationDir)

    subdirs = ['mutations','queries','subscriptions']

    for subdir in subdirs:
        subdirPath = '{0}/{1}'.format(path, subdir)
        outputDir = os.path.join(destinationDir, subdir)
        if not os.path.exists(outputDir):
            os.mkdir(outputDir)
        for typeScriptFile in os.listdir(subdirPath):
            if typeScriptFile.endswith('.ts'):
                outputFilename = typeScriptFile.replace('.ts','.graphql')
                outputFilename = outputFilename[0].upper() + outputFilename[1:]

                # ensure subscription file names start with `On`
                if subdir is 'subscriptions':
                    if not outputFilename.startswith('On'):
                        outputFilename = 'On' + outputFilename
                outputPath = os.path.join(outputDir, outputFilename)

                fp = open(os.path.join(subdirPath,typeScriptFile))
                fpContents = fp.read()
                fp.close()
                # interface definition starts with gql` in typescript files (skip commented ones)
                versionMatch = re.search("^[^\/]*gql\`\n(\s*)(.*)\`", fpContents, re.M | re.DOTALL)
                if versionMatch is None:
                    continue
                whitespace = versionMatch.group(1)
                matchedGroup = versionMatch.group(2)

                fp = open(outputPath, 'w')
                # write de-indented string
                fp.write(textwrap.dedent(matchedGroup))
                fp.close()

    print('Generated graphql files in the ' + destinationDir + ' directory')
    
if __name__ == '__main__':
    sys.exit(main(*sys.argv))

# usage: ./generate-graphQL-interface.py -p /Users/jamie/Work/ApolloCollab/apollo-collab/packages/collab-client/src/graphql
