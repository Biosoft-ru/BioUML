=LoadTable	test/input
ID	FC:Float
201123_s_at	-4
201050_at	-2.6
201332_s_at	-3
=End

=SetProperties	test/input
referenceType	AffymetrixProbeTableType
=End

=LoadTable	test/result
ID	Affymetrix ID:Text	FC:Float
ENSG00000132507	201123_s_at	-4
ENSG00000178510	201123_s_at	-4
ENSG00000216238	201123_s_at	-4
ENSG00000217086	201123_s_at	-4
ENSG00000105223	201050_at	-2.6
ENSG00000166888	201332_s_at	-3
=End

=Analysis	ru.biosoft.analysis.TableConverter
sourceTable	test/input
sourceType	Probes: Affymetrix
targetType	Genes: Ensembl
outputTable	test/output
species	Homo sapiens
maxMatches	4
=End

=CompareTables
test/output
test/result
=End

=LoadTable	test/input
ID	FC:Float
201123_s_at	-4
201050_at	-2.6
201332_s_at	-3
=End

=SetProperties	test/input
referenceType	AffymetrixProbeTableType
=End

=LoadTable	test/result
ID	Affymetrix ID:Text	FC:Float
ENSG00000105223	201050_at	-2.6
ENSG00000166888	201332_s_at	-3
=End

=Analysis	ru.biosoft.analysis.TableConverter
sourceTable	test/input
sourceType	Probes: Affymetrix
targetType	Genes: Ensembl
outputTable	test/output
species	Homo sapiens
maxMatches	2
=End

=CompareTables
test/output
test/result
=End

=LoadTable	test/input
ID	FC:Float
201123_s_at	-4
201050_at	-2.6
201332_s_at	-3
=End

=SetProperties	test/input
referenceType	AffymetrixProbeTableType
=End

=LoadTable	test/result
ID	Affymetrix ID:Text	FC:Float
ENSG00000132507	201123_s_at	-4
ENSG00000178510	201123_s_at	-4
ENSG00000216238	201123_s_at	-4
ENSG00000217086	201123_s_at	-4
ENSG00000105223	201050_at	-2.6
ENSG00000166888	201332_s_at	-3
=End

=Analysis	ru.biosoft.analysis.TableConverter
sourceTable	test/input
sourceType	Probes: Affymetrix
targetType	Genes: Ensembl
outputTable	test/output
species	Homo sapiens
maxMatches	0
=End

=CompareTables
test/output
test/result
=End