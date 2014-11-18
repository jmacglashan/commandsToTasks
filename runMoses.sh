#!/bin/sh
cd moses-training 
export LC_ALL=C
/export/home/wael/darpa-bolt-mt/backup/smt-pipeline/scripts/Wael_SMTPipeline.perl config.train.smt-pipeline.arz-eng.ini
#/export/home/wael/darpa-bolt-mt/backup/tools/moses-decoder/2012-02-06/scripts/training/train-model.perl config.train.smt-pipeline.arz-eng.ini
