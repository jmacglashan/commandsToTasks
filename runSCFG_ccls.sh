#!/bin/sh
export JAVA_HOME=/export/home/ab3900/java/jdk1.7.0_67/jre
export PATH=$JAVA_HOME/bin:$PATH
chmod a+x runMoses.sh
chmod a+x runMosesTranslation.sh 
java -cp lib/*:bin experiments.sokoban.SokoMTExperiment SCFG_MT
