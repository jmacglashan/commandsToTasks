#!/bin/sh
chmod a+x runMoses.sh
java -cp lib/*:bin experiments.sokoban.SokoMTExperiment SCFG_MT
