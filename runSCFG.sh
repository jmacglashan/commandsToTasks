#!/bin/sh
chmod a+x runMoses.sh
chmo a+x runMosesTranslation.sh
java -cp lib/*:bin experiments.sokoban.SokoMTExperiment SCFG_MT
