=LoadTable	test/input
ID	num:Integer
NM_000020	1
NM_000034	1
NM_000100	1
NM_000175	1
NM_000182	1
NM_000183	1
=End

=SetProperties	test/input
referenceType	RefSeqTranscriptTableType
=End

=LoadTable	test/result
ID	Gene symbol:Text	num:Integer
NM_000020	ACVRL1	1
NM_000034	ALDOA	1
NM_000100	CSTB	1
NM_000175	GPI	1
NM_000182	HADHA	1
NM_000183	HADHB	1
=End

=Analysis	ru.biosoft.analysis.Annotate
inputTablePath	test/input
outputTablePath	test/output
annotationCollectionPath	databases/Ensembl/Data/gene
annotationColumnKeys	title
=End

=CompareTables
test/output
test/result
=End
