#!/bin/sh
cp moses-training/test.test /export/projects/nlpeducation/nlpcommands/mosesmodel/datastore/moses.2012-02-06/training/model/
cd /export/projects/nlpeducation/nlpcommands/mosesmodel/datastore/moses.2012-02-06/training/model
rm -rf out.txt score.out
/export/projects/nlp/BOLT-MT/backup/tools/MOSES/moses-2010-08-13/moses-cmd/src/moses -f moses.ini -n-best-list scores.out 100 < test.test >  out.txt
