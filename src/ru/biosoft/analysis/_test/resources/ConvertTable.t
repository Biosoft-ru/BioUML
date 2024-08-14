=LoadTable	test/input
ID	FC:Float
200784_s_at	-3.2
200796_s_at	-3.7
201050_at	-2.6
201123_s_at	-4
201332_s_at	-3
201367_s_at	-4.3
201496_x_at	5.3
201654_s_at	-4
202016_at	2.6
202196_s_at	-3
=End

=SetProperties	test/input
referenceType	AffymetrixProbeTableType
=End

=LoadTable	test/result
ID	Affymetrix ID:Text	FC:Float
143244	201123_s_at	-4
1984	201123_s_at	-4
23646	201050_at	-2.6
27122	202196_s_at	-3
3339	201654_s_at	-4
4035	200784_s_at	-3.2
4170	200796_s_at	-3.7
4232	202016_at	2.6
4629	201496_x_at	5.3
6778	201332_s_at	-3
678	201367_s_at	-4.3
=End

=Analysis	ru.biosoft.analysis.TableConverter
sourceTable	test/input
sourceType	Probes: Affymetrix
targetType	Genes: Entrez
outputTable	test/output
species	Homo sapiens
=End

=CompareTables
test/output
test/result
=End
