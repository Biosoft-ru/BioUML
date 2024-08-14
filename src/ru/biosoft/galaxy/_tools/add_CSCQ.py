#!/usr/bin/python

#ADD CS and CQ tags to SAM file from original CSFastq file
#Read SAM from stdin, write SAM with CS and CQ to stdout, accept CSFastq file as first argument

from sys import stdin, stdout, argv

csfastq = open(argv[1])

for SAMline in stdin:
  if SAMline[0] != '@':
    l1 = csfastq.readline()
    l2 = csfastq.readline()
    l3 = csfastq.readline()
    l4 = csfastq.readline()
    SAMname = SAMline.split("\t",1)[0]
    FQname = l1[1:-1].split()[0]
    if SAMname != FQname:
      raise Exception("Unmatched read (csfastq %s) (sam %s)" % (FQname,SAMname))
    SAMline = SAMline[:-1] + "\tCS:Z:" + l2[:-1] + "\tCQ:Z:" + l4
  stdout.write(SAMline)

csfastq.close()
